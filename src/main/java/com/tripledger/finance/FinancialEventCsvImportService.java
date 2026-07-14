package com.tripledger.finance;

import com.tripledger.authorization.AuthorizationService;
import com.tripledger.authorization.Permission;
import com.tripledger.booking.Booking;
import com.tripledger.booking.BookingRepository;
import com.tripledger.common.api.ApiErrorResponse;
import com.tripledger.common.api.ApiException;
import com.tripledger.identity.ActorContext;
import com.tripledger.ingestion.ImportBatch;
import com.tripledger.ingestion.ImportBatchService;
import com.tripledger.ingestion.ImportRowOutcome;
import com.tripledger.ingestion.SourceRecord;
import com.tripledger.ingestion.SourceRecordRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class FinancialEventCsvImportService {

    private static final String SUPPORTED_TEMPLATE_TYPE = "FINANCIAL_EVENT";
    private static final String SUPPORTED_TEMPLATE_VERSION = "1";
    private static final Set<String> SUPPORTED_CURRENCIES = Set.of("EUR", "GBP", "TRY", "USD");
    private static final List<String> REQUIRED_HEADERS = List.of(
            "template_type",
            "template_version",
            "external_event_id",
            "source_version",
            "source_row_number",
            "event_type",
            "effective_at",
            "amount",
            "currency"
    );

    private final ImportBatchService importBatchService;
    private final SourceRecordRepository sourceRecordRepository;
    private final BookingRepository bookingRepository;
    private final FinancialEventRepository financialEventRepository;
    private final AuthorizationService authorizationService;
    private final Clock clock;

    public FinancialEventCsvImportService(ImportBatchService importBatchService,
                                          SourceRecordRepository sourceRecordRepository,
                                          BookingRepository bookingRepository,
                                          FinancialEventRepository financialEventRepository,
                                          AuthorizationService authorizationService,
                                          Clock clock) {
        this.importBatchService = importBatchService;
        this.sourceRecordRepository = sourceRecordRepository;
        this.bookingRepository = bookingRepository;
        this.financialEventRepository = financialEventRepository;
        this.authorizationService = authorizationService;
        this.clock = clock;
    }

    @Transactional
    public ImportBatch importCsv(ActorContext actor, ImportFinancialEventCsvCommand command) {
        authorizationService.require(actor, Permission.FINANCIAL_ACTION_WITH_MFA);
        validateCommand(command);
        ImportBatch batch = importBatchService.start(
                actor,
                new ImportBatchService.StartImportBatchCommand(
                        command.sourceSystemId(),
                        SUPPORTED_TEMPLATE_TYPE,
                        SUPPORTED_TEMPLATE_VERSION,
                        command.fileName(),
                        command.fileChecksum()
                )
        );

        CsvTable table;
        try {
            table = CsvTable.parse(command.csvContent());
        } catch (IllegalArgumentException exception) {
            importBatchService.fail(actor, batch.id(), new ImportBatchService.FailImportBatchCommand(
                    "INVALID_REQUEST",
                    "CSV content is malformed."
            ));
            return batch;
        }

        Optional<String> missingHeader = REQUIRED_HEADERS.stream()
                .filter(header -> !table.headers().containsKey(header))
                .findFirst();
        if (missingHeader.isPresent()) {
            importBatchService.fail(actor, batch.id(), new ImportBatchService.FailImportBatchCommand(
                    "MISSING_REQUIRED_COLUMN",
                    "Required column is missing: " + missingHeader.get()
            ));
            return batch;
        }

        if (table.rows().stream().anyMatch(row -> !isSupportedTemplate(row))) {
            importBatchService.fail(actor, batch.id(), new ImportBatchService.FailImportBatchCommand(
                    "UNSUPPORTED_TEMPLATE_VERSION",
                    "Only FINANCIAL_EVENT template version 1 is supported."
            ));
            return batch;
        }

        for (CsvRow row : table.rows()) {
            processRow(actor, batch, row);
        }

        return importBatchService.complete(actor, batch.id());
    }

    private void processRow(ActorContext actor, ImportBatch batch, CsvRow row) {
        ParsedFinancialEventRow parsed;
        try {
            parsed = parseFinancialEventRow(row);
        } catch (RowRejectedException exception) {
            recordRejected(actor, batch.id(), row.lineNumber(), exception.field(), exception.code(), exception.reason());
            return;
        }

        String checksum = checksum(parsed.canonicalContent());
        Optional<SourceRecord> existingSourceRecord = sourceRecordRepository
                .findByOrganisationIdAndSourceSystemIdAndRecordTypeAndExternalRecordIdAndSourceVersion(
                        actor.organisationId(),
                        batch.sourceSystemId(),
                        SourceRecord.FINANCIAL_EVENT_RECORD_TYPE,
                        parsed.externalEventId(),
                        parsed.sourceVersion()
                );
        if (existingSourceRecord.isPresent()) {
            recordDuplicateOrConflict(actor, batch.id(), row.lineNumber(), checksum, existingSourceRecord.get());
            return;
        }

        UUID bookingId = resolveBookingId(actor, parsed.bookingReference());
        SourceRecord sourceRecord = saveSourceRecord(actor, batch, parsed, row.lineNumber(), checksum);
        financialEventRepository.save(new FinancialEvent(
                UUID.randomUUID(),
                actor.organisationId(),
                sourceRecord.id(),
                bookingId,
                parsed.eventType(),
                directionFor(parsed.eventType()),
                parsed.amount(),
                parsed.currency(),
                parsed.effectiveAt(),
                parsed.externalCustomerReference(),
                null,
                null,
                actor.userId(),
                Instant.now(clock)
        ));

        importBatchService.recordRowResult(actor, batch.id(), new ImportBatchService.RecordRowResultCommand(
                row.lineNumber(),
                ImportRowOutcome.ACCEPTED,
                null,
                null,
                null,
                sourceRecord.id()
        ));
    }

    private void recordDuplicateOrConflict(ActorContext actor,
                                           UUID batchId,
                                           int rowNumber,
                                           String checksum,
                                           SourceRecord sourceRecord) {
        if (sourceRecord.contentChecksum().equals(checksum)) {
            importBatchService.recordRowResult(actor, batchId, new ImportBatchService.RecordRowResultCommand(
                    rowNumber,
                    ImportRowOutcome.DUPLICATE,
                    null,
                    null,
                    null,
                    sourceRecord.id()
            ));
            return;
        }

        recordRejected(
                actor,
                batchId,
                rowNumber,
                "sourceVersion",
                "STALE_SOURCE_VERSION",
                "Source identity already exists with different content."
        );
    }

    private UUID resolveBookingId(ActorContext actor, String bookingReference) {
        if (!StringUtils.hasText(bookingReference)) {
            return null;
        }

        List<Booking> bookings = bookingRepository.findAllByOrganisationIdAndExternalBookingId(
                actor.organisationId(),
                bookingReference
        );
        return bookings.size() == 1 ? bookings.getFirst().id() : null;
    }

    private SourceRecord saveSourceRecord(ActorContext actor,
                                          ImportBatch batch,
                                          ParsedFinancialEventRow parsed,
                                          int rowNumber,
                                          String checksum) {
        SourceRecord sourceRecord = new SourceRecord(
                UUID.randomUUID(),
                actor.organisationId(),
                batch.sourceSystemId(),
                batch.id(),
                SourceRecord.FINANCIAL_EVENT_RECORD_TYPE,
                parsed.externalEventId(),
                parsed.sourceVersion(),
                rowNumber,
                checksum,
                parsed.provenanceNote(),
                Instant.now(clock)
        );
        return sourceRecordRepository.save(sourceRecord);
    }

    private ParsedFinancialEventRow parseFinancialEventRow(CsvRow row) {
        String externalEventId = required(row, "external_event_id");
        String sourceVersion = required(row, "source_version");
        String bookingReference = nullable(row.value("booking_reference"));
        FinancialEventType eventType = enumValue(
                FinancialEventType.class,
                row,
                "event_type",
                "INVALID_FIELD_TYPE"
        );
        Instant effectiveAt = instant(row, "effective_at");
        BigDecimal amount = positiveMoney(row, "amount");
        String currency = currency(row, "currency");
        String externalCustomerReference = nullable(row.value("external_customer_reference"));
        String provenanceNote = nullable(row.value("provenance_note"));
        rejectProhibitedPaymentData(provenanceNote);

        return new ParsedFinancialEventRow(
                externalEventId,
                sourceVersion,
                bookingReference,
                eventType,
                effectiveAt,
                amount,
                currency,
                externalCustomerReference,
                provenanceNote,
                row.canonicalContent()
        );
    }

    private Instant instant(CsvRow row, String field) {
        String value = required(row, field);
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException exception) {
            throw rejected(field, "INVALID_FIELD_TYPE", "Timestamp must use ISO-8601 instant format.");
        }
    }

    private BigDecimal positiveMoney(CsvRow row, String field) {
        String value = required(row, field);
        try {
            BigDecimal amount = new BigDecimal(value);
            if (amount.signum() <= 0) {
                throw rejected(field, "INVALID_FIELD_TYPE", "Amount must be positive.");
            }
            if (amount.stripTrailingZeros().scale() > 2) {
                throw rejected(field, "INVALID_CURRENCY_PRECISION", "Currency amount has too many fractional digits.");
            }
            return amount.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException | NumberFormatException exception) {
            throw rejected(field, "INVALID_FIELD_TYPE", "Amount must be a decimal number.");
        }
    }

    private String currency(CsvRow row, String field) {
        String value = required(row, field).toUpperCase(Locale.ROOT);
        if (!value.matches("[A-Z]{3}") || !SUPPORTED_CURRENCIES.contains(value)) {
            throw rejected(field, "INVALID_CURRENCY", "Currency is not supported.");
        }
        return value;
    }

    private <E extends Enum<E>> E enumValue(Class<E> enumType, CsvRow row, String field, String code) {
        String value = required(row, field).toUpperCase(Locale.ROOT);
        try {
            return Enum.valueOf(enumType, value);
        } catch (IllegalArgumentException exception) {
            throw rejected(field, code, "Unsupported value.");
        }
    }

    private FinancialEventDirection directionFor(FinancialEventType eventType) {
        return switch (eventType) {
            case CUSTOMER_PAYMENT, CHANNEL_SETTLEMENT -> FinancialEventDirection.INCREASE_RECEIVED;
            case REFUND, PAYMENT_REVERSAL -> FinancialEventDirection.DECREASE_RECEIVED;
            case CHANNEL_COMMISSION, PAYMENT_FEE -> FinancialEventDirection.INCREASE_DEDUCTION;
            case SUPPLIER_PAYMENT -> FinancialEventDirection.INCREASE_SUPPLIER_SETTLEMENT;
            case SUPPLIER_CREDIT -> FinancialEventDirection.DECREASE_SUPPLIER_COST;
            case MANUAL_ADJUSTMENT -> FinancialEventDirection.ADJUSTMENT;
        };
    }

    private void rejectProhibitedPaymentData(String value) {
        if (value == null) {
            return;
        }
        String normalized = value.toLowerCase(Locale.ROOT);
        if (value.matches(".*\\d{13,19}.*") || normalized.contains("cvv") || normalized.contains("card number")) {
            throw rejected("provenance_note", "PROHIBITED_PAYMENT_DATA", "Raw payment data is not allowed.");
        }
    }

    private String required(CsvRow row, String field) {
        String value = row.value(field);
        if (!StringUtils.hasText(value)) {
            throw rejected(field, "MISSING_REQUIRED_FIELD", "Required field is missing.");
        }
        return value.trim();
    }

    private boolean isSupportedTemplate(CsvRow row) {
        return SUPPORTED_TEMPLATE_TYPE.equals(row.value("template_type"))
                && SUPPORTED_TEMPLATE_VERSION.equals(row.value("template_version"));
    }

    private void recordRejected(ActorContext actor,
                                UUID batchId,
                                int rowNumber,
                                String field,
                                String code,
                                String reason) {
        importBatchService.recordRowResult(actor, batchId, new ImportBatchService.RecordRowResultCommand(
                rowNumber,
                ImportRowOutcome.REJECTED,
                field,
                code,
                reason,
                null
        ));
    }

    private void validateCommand(ImportFinancialEventCsvCommand command) {
        if (command.sourceSystemId() == null) {
            throw invalidField("sourceSystemId", "Source system id is required.");
        }
        if (!StringUtils.hasText(command.fileName())) {
            throw invalidField("fileName", "File name is required.");
        }
        if (!StringUtils.hasText(command.fileChecksum())) {
            throw invalidField("fileChecksum", "File checksum is required.");
        }
        if (!StringUtils.hasText(command.csvContent())) {
            throw invalidField("csvContent", "CSV content is required.");
        }
    }

    private ApiException invalidField(String field, String reason) {
        return new ApiException(
                HttpStatus.BAD_REQUEST,
                "INVALID_REQUEST",
                "Request validation failed.",
                List.of(new ApiErrorResponse.ApiErrorDetail(field, reason))
        );
    }

    private RowRejectedException rejected(String field, String code, String reason) {
        return new RowRejectedException(field, code, reason);
    }

    private String checksum(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            return "sha256:" + HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available.", exception);
        }
    }

    private String nullable(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    public record ImportFinancialEventCsvCommand(
            UUID sourceSystemId,
            String fileName,
            String fileChecksum,
            String csvContent
    ) {
    }

    private record ParsedFinancialEventRow(
            String externalEventId,
            String sourceVersion,
            String bookingReference,
            FinancialEventType eventType,
            Instant effectiveAt,
            BigDecimal amount,
            String currency,
            String externalCustomerReference,
            String provenanceNote,
            String canonicalContent
    ) {
    }

    private record CsvTable(Map<String, Integer> headers, List<CsvRow> rows) {

        static CsvTable parse(String content) {
            List<List<String>> records = CsvParser.parse(content);
            if (records.isEmpty()) {
                throw new IllegalArgumentException("CSV content is empty.");
            }
            Map<String, Integer> headers = new HashMap<>();
            List<String> headerValues = records.getFirst();
            for (int index = 0; index < headerValues.size(); index++) {
                headers.put(headerValues.get(index).trim(), index);
            }

            List<CsvRow> rows = new ArrayList<>();
            for (int index = 1; index < records.size(); index++) {
                rows.add(new CsvRow(headers, records.get(index), index + 1));
            }
            return new CsvTable(headers, rows);
        }
    }

    private record CsvRow(Map<String, Integer> headers, List<String> values, int lineNumber) {

        String value(String header) {
            Integer index = headers.get(header);
            if (index == null || index >= values.size()) {
                return "";
            }
            return values.get(index).trim();
        }

        String canonicalContent() {
            return String.join("|", values.stream().map(String::trim).toList());
        }
    }

    private static final class CsvParser {

        private CsvParser() {
        }

        static List<List<String>> parse(String content) {
            List<List<String>> records = new ArrayList<>();
            List<String> record = new ArrayList<>();
            StringBuilder field = new StringBuilder();
            boolean quoted = false;

            for (int index = 0; index < content.length(); index++) {
                char current = content.charAt(index);
                if (quoted) {
                    if (current == '"') {
                        if (index + 1 < content.length() && content.charAt(index + 1) == '"') {
                            field.append('"');
                            index++;
                        } else {
                            quoted = false;
                        }
                    } else {
                        field.append(current);
                    }
                } else if (current == '"') {
                    quoted = true;
                } else if (current == ',') {
                    record.add(field.toString());
                    field.setLength(0);
                } else if (current == '\n') {
                    record.add(field.toString());
                    addRecord(records, record);
                    record = new ArrayList<>();
                    field.setLength(0);
                } else if (current != '\r') {
                    field.append(current);
                }
            }

            if (quoted) {
                throw new IllegalArgumentException("Unclosed quoted field.");
            }

            record.add(field.toString());
            addRecord(records, record);
            return records;
        }

        private static void addRecord(List<List<String>> records, List<String> record) {
            if (record.stream().anyMatch(StringUtils::hasText)) {
                records.add(record);
            }
        }
    }

    private static final class RowRejectedException extends RuntimeException {

        private final String field;
        private final String code;
        private final String reason;

        private RowRejectedException(String field, String code, String reason) {
            this.field = field;
            this.code = code;
            this.reason = reason;
        }

        private String field() {
            return field;
        }

        private String code() {
            return code;
        }

        private String reason() {
            return reason;
        }
    }
}

package com.tripledger.supplier;

import com.tripledger.booking.Booking;
import com.tripledger.booking.BookingItem;
import com.tripledger.booking.BookingItemRepository;
import com.tripledger.booking.BookingRepository;
import com.tripledger.common.api.ApiErrorResponse;
import com.tripledger.common.api.ApiException;
import com.tripledger.common.money.MoneyPolicy;
import com.tripledger.common.money.MoneyValidationException;
import com.tripledger.identity.ActorContext;
import com.tripledger.ingestion.ImportBatch;
import com.tripledger.ingestion.ImportBatchService;
import com.tripledger.ingestion.ImportRowOutcome;
import com.tripledger.ingestion.SourceRecord;
import com.tripledger.ingestion.SourceRecordRepository;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class SupplierObligationCsvImportService {

    private static final String SUPPORTED_TEMPLATE_TYPE = "SUPPLIER_OBLIGATION";
    private static final String SUPPORTED_TEMPLATE_VERSION = "1";
    private static final List<String> REQUIRED_HEADERS = List.of(
            "template_type",
            "template_version",
            "external_obligation_id",
            "source_version",
            "source_row_number",
            "supplier_reference",
            "supplier_name",
            "status",
            "amount",
            "currency"
    );

    private final ImportBatchService importBatchService;
    private final SourceRecordRepository sourceRecordRepository;
    private final BookingRepository bookingRepository;
    private final BookingItemRepository bookingItemRepository;
    private final SupplierRepository supplierRepository;
    private final SupplierObligationRepository supplierObligationRepository;
    private final Clock clock;

    public SupplierObligationCsvImportService(ImportBatchService importBatchService,
                                              SourceRecordRepository sourceRecordRepository,
                                              BookingRepository bookingRepository,
                                              BookingItemRepository bookingItemRepository,
                                              SupplierRepository supplierRepository,
                                              SupplierObligationRepository supplierObligationRepository,
                                              Clock clock) {
        this.importBatchService = importBatchService;
        this.sourceRecordRepository = sourceRecordRepository;
        this.bookingRepository = bookingRepository;
        this.bookingItemRepository = bookingItemRepository;
        this.supplierRepository = supplierRepository;
        this.supplierObligationRepository = supplierObligationRepository;
        this.clock = clock;
    }

    @Transactional
    public ImportBatch importCsv(ActorContext actor, ImportSupplierObligationCsvCommand command) {
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
                    "Only SUPPLIER_OBLIGATION template version 1 is supported."
            ));
            return batch;
        }

        for (CsvRow row : table.rows()) {
            processRow(actor, batch, row);
        }

        return importBatchService.complete(actor, batch.id());
    }

    private void processRow(ActorContext actor, ImportBatch batch, CsvRow row) {
        ParsedSupplierObligationRow parsed;
        try {
            parsed = parseSupplierObligationRow(row);
        } catch (RowRejectedException exception) {
            recordRejected(actor, batch.id(), row.lineNumber(), exception.field(), exception.code(), exception.reason());
            return;
        }

        String checksum = checksum(parsed.canonicalContent());
        Optional<SourceRecord> existingSourceRecord = sourceRecordRepository
                .findByOrganisationIdAndSourceSystemIdAndRecordTypeAndExternalRecordIdAndSourceVersion(
                        actor.organisationId(),
                        batch.sourceSystemId(),
                        SourceRecord.SUPPLIER_OBLIGATION_RECORD_TYPE,
                        parsed.externalObligationId(),
                        parsed.sourceVersion()
                );
        if (existingSourceRecord.isPresent()) {
            recordDuplicateOrConflict(actor, batch.id(), row.lineNumber(), checksum, existingSourceRecord.get());
            return;
        }

        LinkedBooking linkedBooking = resolveBookingLink(actor, parsed);
        Supplier supplier = upsertSupplier(actor, parsed);
        SourceRecord sourceRecord = saveSourceRecord(actor, batch, parsed, row.lineNumber(), checksum);
        supplierObligationRepository.save(new SupplierObligation(
                UUID.randomUUID(),
                actor.organisationId(),
                linkedBooking.bookingId(),
                linkedBooking.bookingItemId(),
                supplier.id(),
                sourceRecord.id(),
                parsed.amount(),
                parsed.currency(),
                parsed.dueDate(),
                parsed.status(),
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

    private LinkedBooking resolveBookingLink(ActorContext actor, ParsedSupplierObligationRow parsed) {
        if (!StringUtils.hasText(parsed.bookingReference())) {
            return new LinkedBooking(null, null);
        }

        List<Booking> bookings = bookingRepository.findAllByOrganisationIdAndExternalBookingId(
                actor.organisationId(),
                parsed.bookingReference()
        );
        if (bookings.size() != 1) {
            return new LinkedBooking(null, null);
        }

        Booking booking = bookings.getFirst();
        if (!StringUtils.hasText(parsed.itemExternalId())) {
            return new LinkedBooking(booking.id(), null);
        }

        Optional<BookingItem> bookingItem = bookingItemRepository.findByOrganisationIdAndBookingIdAndItemExternalId(
                actor.organisationId(),
                booking.id(),
                parsed.itemExternalId()
        );
        return bookingItem
                .map(item -> new LinkedBooking(booking.id(), item.id()))
                .orElseGet(() -> new LinkedBooking(booking.id(), null));
    }

    private Supplier upsertSupplier(ActorContext actor, ParsedSupplierObligationRow parsed) {
        return supplierRepository.findByOrganisationIdAndExternalReference(
                        actor.organisationId(),
                        parsed.supplierReference())
                .map(existing -> {
                    existing.rename(parsed.supplierName());
                    return existing;
                })
                .orElseGet(() -> supplierRepository.save(new Supplier(
                        UUID.randomUUID(),
                        actor.organisationId(),
                        parsed.supplierName(),
                        parsed.supplierReference(),
                        SupplierStatus.ACTIVE,
                        Instant.now(clock)
                )));
    }

    private SourceRecord saveSourceRecord(ActorContext actor,
                                          ImportBatch batch,
                                          ParsedSupplierObligationRow parsed,
                                          int rowNumber,
                                          String checksum) {
        SourceRecord sourceRecord = new SourceRecord(
                UUID.randomUUID(),
                actor.organisationId(),
                batch.sourceSystemId(),
                batch.id(),
                SourceRecord.SUPPLIER_OBLIGATION_RECORD_TYPE,
                parsed.externalObligationId(),
                parsed.sourceVersion(),
                rowNumber,
                checksum,
                null,
                Instant.now(clock)
        );
        return sourceRecordRepository.save(sourceRecord);
    }

    private ParsedSupplierObligationRow parseSupplierObligationRow(CsvRow row) {
        String externalObligationId = required(row, "external_obligation_id");
        String sourceVersion = required(row, "source_version");
        String bookingReference = nullable(row.value("booking_reference"));
        String itemExternalId = nullable(row.value("item_external_id"));
        String supplierReference = required(row, "supplier_reference");
        String supplierName = required(row, "supplier_name");
        SupplierObligationStatus status = enumValue(
                SupplierObligationStatus.class,
                row,
                "status",
                "INVALID_FIELD_TYPE"
        );
        LocalDate dueDate = nullableDate(row, "due_date");
        String currency = currency(row, "currency");
        BigDecimal amount = positiveMoney(row, "amount", currency);

        return new ParsedSupplierObligationRow(
                externalObligationId,
                sourceVersion,
                bookingReference,
                itemExternalId,
                supplierReference,
                supplierName,
                status,
                dueDate,
                amount,
                currency,
                row.canonicalContent()
        );
    }

    private String required(CsvRow row, String field) {
        String value = row.value(field);
        if (!StringUtils.hasText(value)) {
            throw rejected(field, "MISSING_REQUIRED_FIELD", "Required field is missing.");
        }
        return value.trim();
    }

    private LocalDate nullableDate(CsvRow row, String field) {
        String value = nullable(row.value(field));
        if (value == null) {
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException exception) {
            throw rejected(field, "INVALID_FIELD_TYPE", "Date must use ISO format YYYY-MM-DD.");
        }
    }

    private BigDecimal positiveMoney(CsvRow row, String field, String currency) {
        String value = required(row, field);
        try {
            return MoneyPolicy.positiveAmount(value, currency);
        } catch (MoneyValidationException exception) {
            throw rejected(field, exception.code(), exception.reason());
        }
    }

    private String currency(CsvRow row, String field) {
        try {
            return MoneyPolicy.currency(required(row, field));
        } catch (MoneyValidationException exception) {
            throw rejected(field, exception.code(), exception.reason());
        }
    }

    private <E extends Enum<E>> E enumValue(Class<E> enumType, CsvRow row, String field, String code) {
        String value = required(row, field).toUpperCase(Locale.ROOT);
        try {
            return Enum.valueOf(enumType, value);
        } catch (IllegalArgumentException exception) {
            throw rejected(field, code, "Unsupported value.");
        }
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

    private void validateCommand(ImportSupplierObligationCsvCommand command) {
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

    public record ImportSupplierObligationCsvCommand(
            UUID sourceSystemId,
            String fileName,
            String fileChecksum,
            String csvContent
    ) {
    }

    private record ParsedSupplierObligationRow(
            String externalObligationId,
            String sourceVersion,
            String bookingReference,
            String itemExternalId,
            String supplierReference,
            String supplierName,
            SupplierObligationStatus status,
            LocalDate dueDate,
            BigDecimal amount,
            String currency,
            String canonicalContent
    ) {
    }

    private record LinkedBooking(UUID bookingId, UUID bookingItemId) {
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

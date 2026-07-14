package com.tripledger.booking;

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
public class BookingCsvImportService {

    private static final String SUPPORTED_TEMPLATE_TYPE = "BOOKING";
    private static final String SUPPORTED_TEMPLATE_VERSION = "1";
    private static final List<String> REQUIRED_HEADERS = List.of(
            "template_type",
            "template_version",
            "external_booking_id",
            "source_version",
            "source_row_number",
            "booking_date",
            "service_start_date",
            "service_end_date",
            "lifecycle_status",
            "item_external_id",
            "service_type",
            "item_start_date",
            "item_end_date",
            "selling_amount",
            "selling_currency"
    );

    private final ImportBatchService importBatchService;
    private final SourceRecordRepository sourceRecordRepository;
    private final BookingRepository bookingRepository;
    private final BookingItemRepository bookingItemRepository;
    private final Clock clock;

    public BookingCsvImportService(ImportBatchService importBatchService,
                                   SourceRecordRepository sourceRecordRepository,
                                   BookingRepository bookingRepository,
                                   BookingItemRepository bookingItemRepository,
                                   Clock clock) {
        this.importBatchService = importBatchService;
        this.sourceRecordRepository = sourceRecordRepository;
        this.bookingRepository = bookingRepository;
        this.bookingItemRepository = bookingItemRepository;
        this.clock = clock;
    }

    @Transactional
    public ImportBatch importCsv(ActorContext actor, ImportBookingCsvCommand command) {
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
                    "Only BOOKING template version 1 is supported."
            ));
            return batch;
        }

        for (CsvRow row : table.rows()) {
            processRow(actor, batch, row);
        }

        return importBatchService.complete(actor, batch.id());
    }

    private void processRow(ActorContext actor, ImportBatch batch, CsvRow row) {
        ParsedBookingRow parsed;
        try {
            parsed = parseBookingRow(row);
        } catch (RowRejectedException exception) {
            recordRejected(actor, batch.id(), row.lineNumber(), exception.field(), exception.code(), exception.reason());
            return;
        }

        String checksum = checksum(parsed.canonicalContent());
        Optional<SourceRecord> existingSourceRecord = sourceRecordRepository
                .findByOrganisationIdAndSourceSystemIdAndRecordTypeAndExternalRecordIdAndSourceVersion(
                        actor.organisationId(),
                        batch.sourceSystemId(),
                        SourceRecord.BOOKING_RECORD_TYPE,
                        parsed.externalBookingId(),
                        parsed.sourceVersion()
                );
        if (existingSourceRecord.isPresent()) {
            SourceRecord sourceRecord = existingSourceRecord.get();
            if (sourceRecord.contentChecksum().equals(checksum)) {
                importBatchService.recordRowResult(actor, batch.id(), new ImportBatchService.RecordRowResultCommand(
                        row.lineNumber(),
                        ImportRowOutcome.DUPLICATE,
                        null,
                        null,
                        null,
                        sourceRecord.id()
                ));
            } else {
                recordRejected(
                        actor,
                        batch.id(),
                        row.lineNumber(),
                        "sourceVersion",
                        "STALE_SOURCE_VERSION",
                        "Source identity already exists with different content."
                );
            }
            return;
        }

        Optional<Booking> existingBooking = bookingRepository.findByOrganisationIdAndSourceSystemIdAndExternalBookingId(
                actor.organisationId(),
                batch.sourceSystemId(),
                parsed.externalBookingId()
        );
        if (existingBooking.isPresent() && isStale(actor, batch.sourceSystemId(), existingBooking.get(), parsed)) {
            recordRejected(
                    actor,
                    batch.id(),
                    row.lineNumber(),
                    "sourceVersion",
                    "STALE_SOURCE_VERSION",
                    "Source version is older than the current booking version."
            );
            return;
        }

        SourceRecord sourceRecord = saveSourceRecord(actor, batch, parsed, row.lineNumber(), checksum);
        Booking booking = existingBooking
                .map(existing -> updateBooking(existing, sourceRecord, parsed))
                .orElseGet(() -> createBooking(actor, batch.sourceSystemId(), sourceRecord, parsed));
        upsertBookingItem(actor, booking, sourceRecord, parsed);

        importBatchService.recordRowResult(actor, batch.id(), new ImportBatchService.RecordRowResultCommand(
                row.lineNumber(),
                ImportRowOutcome.ACCEPTED,
                null,
                null,
                null,
                sourceRecord.id()
        ));
    }

    private boolean isStale(ActorContext actor, UUID sourceSystemId, Booking booking, ParsedBookingRow parsed) {
        if (booking.currentSourceRecordId() == null) {
            return false;
        }

        return sourceRecordRepository.findById(booking.currentSourceRecordId())
                .filter(current -> current.organisationId().equals(actor.organisationId()))
                .filter(current -> current.sourceSystemId().equals(sourceSystemId))
                .map(current -> compareVersions(parsed.sourceVersion(), current.sourceVersion()) < 0)
                .orElse(false);
    }

    private SourceRecord saveSourceRecord(ActorContext actor,
                                          ImportBatch batch,
                                          ParsedBookingRow parsed,
                                          int rowNumber,
                                          String checksum) {
        SourceRecord sourceRecord = new SourceRecord(
                UUID.randomUUID(),
                actor.organisationId(),
                batch.sourceSystemId(),
                batch.id(),
                SourceRecord.BOOKING_RECORD_TYPE,
                parsed.externalBookingId(),
                parsed.sourceVersion(),
                rowNumber,
                checksum,
                null,
                Instant.now(clock)
        );
        return sourceRecordRepository.save(sourceRecord);
    }

    private Booking createBooking(ActorContext actor,
                                  UUID sourceSystemId,
                                  SourceRecord sourceRecord,
                                  ParsedBookingRow parsed) {
        Instant now = Instant.now(clock);
        Booking booking = new Booking(
                UUID.randomUUID(),
                actor.organisationId(),
                sourceSystemId,
                parsed.externalBookingId(),
                sourceRecord.id(),
                parsed.bookingDate(),
                parsed.serviceStartDate(),
                parsed.serviceEndDate(),
                parsed.lifecycleStatus(),
                parsed.sellingCurrency(),
                parsed.sellingAmount(),
                parsed.customerReference(),
                now,
                now
        );
        return bookingRepository.save(booking);
    }

    private Booking updateBooking(Booking booking, SourceRecord sourceRecord, ParsedBookingRow parsed) {
        booking.applySource(
                sourceRecord.id(),
                parsed.bookingDate(),
                parsed.serviceStartDate(),
                parsed.serviceEndDate(),
                parsed.lifecycleStatus(),
                parsed.sellingCurrency(),
                parsed.sellingAmount(),
                parsed.customerReference(),
                Instant.now(clock)
        );
        return booking;
    }

    private void upsertBookingItem(ActorContext actor, Booking booking, SourceRecord sourceRecord, ParsedBookingRow row) {
        Optional<BookingItem> existingItem = bookingItemRepository.findByOrganisationIdAndBookingIdAndItemExternalId(
                actor.organisationId(),
                booking.id(),
                row.itemExternalId()
        );
        if (existingItem.isPresent()) {
            existingItem.get().applySource(
                    sourceRecord.id(),
                    row.serviceType(),
                    row.itemStartDate(),
                    row.itemEndDate(),
                    row.sellingAmount(),
                    row.sellingCurrency()
            );
            return;
        }

        bookingItemRepository.save(new BookingItem(
                UUID.randomUUID(),
                actor.organisationId(),
                booking.id(),
                sourceRecord.id(),
                row.itemExternalId(),
                row.serviceType(),
                row.itemStartDate(),
                row.itemEndDate(),
                row.sellingAmount(),
                row.sellingCurrency(),
                BookingItemState.ACTIVE
        ));
    }

    private ParsedBookingRow parseBookingRow(CsvRow row) {
        String externalBookingId = required(row, "external_booking_id");
        String sourceVersion = required(row, "source_version");
        LocalDate bookingDate = date(row, "booking_date");
        LocalDate serviceStartDate = date(row, "service_start_date");
        LocalDate serviceEndDate = date(row, "service_end_date");
        BookingLifecycleStatus lifecycleStatus = enumValue(
                BookingLifecycleStatus.class,
                row,
                "lifecycle_status",
                "INVALID_FIELD_TYPE"
        );
        String itemExternalId = required(row, "item_external_id");
        BookingItemServiceType serviceType = enumValue(
                BookingItemServiceType.class,
                row,
                "service_type",
                "INVALID_FIELD_TYPE"
        );
        LocalDate itemStartDate = date(row, "item_start_date");
        LocalDate itemEndDate = date(row, "item_end_date");
        BigDecimal sellingAmount = money(row, "selling_amount");
        String sellingCurrency = currency(row, "selling_currency");
        String customerReference = nullable(row.value("customer_reference"));

        if (serviceEndDate.isBefore(serviceStartDate)) {
            throw rejected("service_end_date", "INVALID_BOOKING_DATE", "Service end date cannot precede start date.");
        }
        if (itemEndDate.isBefore(itemStartDate)) {
            throw rejected("item_end_date", "INVALID_BOOKING_DATE", "Item end date cannot precede start date.");
        }

        return new ParsedBookingRow(
                externalBookingId,
                sourceVersion,
                bookingDate,
                serviceStartDate,
                serviceEndDate,
                lifecycleStatus,
                itemExternalId,
                serviceType,
                itemStartDate,
                itemEndDate,
                sellingAmount,
                sellingCurrency,
                customerReference,
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

    private LocalDate date(CsvRow row, String field) {
        String value = required(row, field);
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException exception) {
            throw rejected(field, "INVALID_FIELD_TYPE", "Date must use ISO format YYYY-MM-DD.");
        }
    }

    private BigDecimal money(CsvRow row, String field) {
        String value = required(row, field);
        try {
            BigDecimal amount = new BigDecimal(value);
            if (amount.signum() < 0) {
                throw rejected(field, "INVALID_FIELD_TYPE", "Amount must be non-negative.");
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
        if (!value.matches("[A-Z]{3}")) {
            throw rejected(field, "INVALID_CURRENCY", "Currency must be a three-letter ISO code.");
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

    private void validateCommand(ImportBookingCsvCommand command) {
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

    private int compareVersions(String candidate, String current) {
        try {
            return Integer.compare(Integer.parseInt(candidate), Integer.parseInt(current));
        } catch (NumberFormatException exception) {
            return candidate.compareTo(current);
        }
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

    public record ImportBookingCsvCommand(
            UUID sourceSystemId,
            String fileName,
            String fileChecksum,
            String csvContent
    ) {
    }

    private record ParsedBookingRow(
            String externalBookingId,
            String sourceVersion,
            LocalDate bookingDate,
            LocalDate serviceStartDate,
            LocalDate serviceEndDate,
            BookingLifecycleStatus lifecycleStatus,
            String itemExternalId,
            BookingItemServiceType serviceType,
            LocalDate itemStartDate,
            LocalDate itemEndDate,
            BigDecimal sellingAmount,
            String sellingCurrency,
            String customerReference,
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

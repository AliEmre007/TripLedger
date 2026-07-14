package com.tripledger.finance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tripledger.authorization.AuthorizationService;
import com.tripledger.authorization.Permission;
import com.tripledger.booking.Booking;
import com.tripledger.booking.BookingLifecycleStatus;
import com.tripledger.booking.BookingRepository;
import com.tripledger.identity.ActorContext;
import com.tripledger.identity.UserRole;
import com.tripledger.ingestion.ImportBatch;
import com.tripledger.ingestion.ImportBatchRepository;
import com.tripledger.ingestion.ImportBatchService;
import com.tripledger.ingestion.ImportBatchStatus;
import com.tripledger.ingestion.ImportRowOutcome;
import com.tripledger.ingestion.ImportRowResult;
import com.tripledger.ingestion.ImportRowResultRepository;
import com.tripledger.ingestion.SourceRecord;
import com.tripledger.ingestion.SourceRecordRepository;
import com.tripledger.source.SourceSystem;
import com.tripledger.source.SourceSystemCategory;
import com.tripledger.source.SourceSystemRepository;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FinancialEventCsvImportServiceTest {

    private static final UUID ORGANISATION_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID SOURCE_SYSTEM_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID BOOKING_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final Instant NOW = Instant.parse("2026-07-14T06:00:00Z");
    private static final Path FIXTURE_ROOT = Path.of("src/test/resources/fixtures/validation-release/finance");

    @Mock
    private ImportBatchRepository importBatchRepository;

    @Mock
    private ImportRowResultRepository importRowResultRepository;

    @Mock
    private SourceSystemRepository sourceSystemRepository;

    @Mock
    private AuthorizationService authorizationService;

    @Mock
    private SourceRecordRepository sourceRecordRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private FinancialEventRepository financialEventRepository;

    private final List<SourceRecord> sourceRecords = new ArrayList<>();
    private final List<FinancialEvent> financialEvents = new ArrayList<>();
    private final List<ImportRowResult> rowResults = new ArrayList<>();
    private ImportBatch currentBatch;
    private Booking booking;

    @BeforeEach
    void setUp() {
        booking = booking();
        when(sourceSystemRepository.findByOrganisationIdAndId(ORGANISATION_ID, SOURCE_SYSTEM_ID))
                .thenReturn(Optional.of(sourceSystem()));
        when(importBatchRepository.save(any(ImportBatch.class))).thenAnswer(invocation -> {
            currentBatch = invocation.getArgument(0);
            return currentBatch;
        });
        when(importBatchRepository.findByOrganisationIdAndId(eq(ORGANISATION_ID), any(UUID.class)))
                .thenAnswer(invocation -> Optional.ofNullable(currentBatch));
        when(importRowResultRepository.existsByOrganisationIdAndImportBatchIdAndRowNumber(
                eq(ORGANISATION_ID),
                any(UUID.class),
                anyInt()
        )).thenReturn(false);
        when(importRowResultRepository.save(any(ImportRowResult.class))).thenAnswer(invocation -> {
            ImportRowResult rowResult = invocation.getArgument(0);
            rowResults.add(rowResult);
            return rowResult;
        });
        when(sourceRecordRepository.save(any(SourceRecord.class))).thenAnswer(invocation -> {
            SourceRecord sourceRecord = invocation.getArgument(0);
            sourceRecords.add(sourceRecord);
            return sourceRecord;
        });
        when(sourceRecordRepository.findByOrganisationIdAndSourceSystemIdAndRecordTypeAndExternalRecordIdAndSourceVersion(
                eq(ORGANISATION_ID),
                eq(SOURCE_SYSTEM_ID),
                eq(SourceRecord.FINANCIAL_EVENT_RECORD_TYPE),
                any(String.class),
                any(String.class)
        )).thenAnswer(invocation -> sourceRecords.stream()
                .filter(record -> record.recordType().equals(invocation.getArgument(2)))
                .filter(record -> record.externalRecordId().equals(invocation.getArgument(3)))
                .filter(record -> record.sourceVersion().equals(invocation.getArgument(4)))
                .findFirst());
        when(bookingRepository.findAllByOrganisationIdAndExternalBookingId(
                eq(ORGANISATION_ID),
                any(String.class)
        )).thenAnswer(invocation -> "TL-BKG-1001".equals(invocation.getArgument(1))
                ? List.of(booking)
                : List.of());
        when(financialEventRepository.save(any(FinancialEvent.class))).thenAnswer(invocation -> {
            FinancialEvent event = invocation.getArgument(0);
            financialEvents.add(event);
            return event;
        });
    }

    @Test
    void importsValidFinancialEventsAndPreservesUnmatchedRows() throws Exception {
        ImportBatch batch = service().importCsv(actor(), command("financial_events_valid.csv"));

        assertThat(batch.status()).isEqualTo(ImportBatchStatus.COMPLETED);
        assertThat(batch.acceptedCount()).isEqualTo(6);
        assertThat(sourceRecords).hasSize(6);
        assertThat(financialEvents).hasSize(6);
        assertThat(financialEvents.getFirst().bookingId()).isEqualTo(BOOKING_ID);
        assertThat(financialEvents.getFirst().eventType()).isEqualTo(FinancialEventType.CUSTOMER_PAYMENT);
        assertThat(financialEvents.getFirst().direction()).isEqualTo(FinancialEventDirection.INCREASE_RECEIVED);
        assertThat(financialEvents.getFirst().amount()).isEqualByComparingTo(new BigDecimal("950.00"));
        assertThat(financialEvents.getFirst().effectiveAt()).isEqualTo(Instant.parse("2026-07-02T10:15:00Z"));
        assertThat(financialEvents).filteredOn(event -> event.bookingId() == null).hasSize(2);
        assertThat(rowResults).extracting(ImportRowResult::outcome).containsOnly(ImportRowOutcome.ACCEPTED);
        verify(authorizationService).require(actor(), Permission.FINANCIAL_ACTION_WITH_MFA);
    }

    @Test
    void duplicateReimportCreatesNoDuplicateFinancialEffect() throws Exception {
        service().importCsv(actor(), command("financial_events_valid.csv"));
        rowResults.clear();

        ImportBatch duplicateBatch = service().importCsv(actor(), command("financial_events_valid.csv"));

        assertThat(duplicateBatch.status()).isEqualTo(ImportBatchStatus.COMPLETED);
        assertThat(duplicateBatch.duplicateCount()).isEqualTo(6);
        assertThat(financialEvents).hasSize(6);
        assertThat(rowResults).extracting(ImportRowResult::outcome).containsOnly(ImportRowOutcome.DUPLICATE);
    }

    @Test
    void invalidMoneyRowsAreRejectedWithoutCreatingEvents() throws Exception {
        ImportBatch batch = service().importCsv(actor(), command("financial_events_invalid_money.csv"));

        assertThat(batch.status()).isEqualTo(ImportBatchStatus.COMPLETED_WITH_ERRORS);
        assertThat(batch.rejectedCount()).isEqualTo(2);
        assertThat(financialEvents).isEmpty();
        assertThat(rowResults).extracting(ImportRowResult::fieldName)
                .containsExactly("amount", "currency");
        assertThat(rowResults).extracting(ImportRowResult::errorCode)
                .containsExactly("INVALID_CURRENCY_PRECISION", "INVALID_CURRENCY");
    }

    private FinancialEventCsvImportService service() {
        ImportBatchService importBatchService = new ImportBatchService(
                importBatchRepository,
                importRowResultRepository,
                sourceSystemRepository,
                authorizationService,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
        return new FinancialEventCsvImportService(
                importBatchService,
                sourceRecordRepository,
                bookingRepository,
                financialEventRepository,
                authorizationService,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    private FinancialEventCsvImportService.ImportFinancialEventCsvCommand command(String fixtureName)
            throws Exception {
        return new FinancialEventCsvImportService.ImportFinancialEventCsvCommand(
                SOURCE_SYSTEM_ID,
                fixtureName,
                "sha256:" + fixtureName,
                Files.readString(FIXTURE_ROOT.resolve(fixtureName))
        );
    }

    private SourceSystem sourceSystem() {
        return new SourceSystem(
                SOURCE_SYSTEM_ID,
                ORGANISATION_ID,
                "OTA Export",
                SourceSystemCategory.PAYMENT_PROVIDER,
                "OTA_DEMO",
                "Europe/Istanbul",
                true,
                NOW
        );
    }

    private Booking booking() {
        return new Booking(
                BOOKING_ID,
                ORGANISATION_ID,
                SOURCE_SYSTEM_ID,
                "TL-BKG-1001",
                null,
                LocalDate.parse("2026-07-01"),
                LocalDate.parse("2026-08-10"),
                LocalDate.parse("2026-08-15"),
                BookingLifecycleStatus.CONFIRMED,
                "EUR",
                new BigDecimal("1000.00"),
                "CUST-1001",
                NOW,
                NOW
        );
    }

    private ActorContext actor() {
        return new ActorContext(
                USER_ID,
                ORGANISATION_ID,
                "Finance User",
                UserRole.FINANCE,
                true,
                "corr-123"
        );
    }
}

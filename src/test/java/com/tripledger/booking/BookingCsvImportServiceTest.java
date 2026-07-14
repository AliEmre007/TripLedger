package com.tripledger.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.tripledger.authorization.AuthorizationService;
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
class BookingCsvImportServiceTest {

    private static final UUID ORGANISATION_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID SOURCE_SYSTEM_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final Instant NOW = Instant.parse("2026-07-14T06:00:00Z");
    private static final Path FIXTURE_ROOT = Path.of("src/test/resources/fixtures/validation-release/booking");

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
    private BookingItemRepository bookingItemRepository;

    private final List<SourceRecord> sourceRecords = new ArrayList<>();
    private final List<Booking> bookings = new ArrayList<>();
    private final List<BookingItem> bookingItems = new ArrayList<>();
    private final List<ImportRowResult> rowResults = new ArrayList<>();
    private ImportBatch currentBatch;

    @BeforeEach
    void setUp() {
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
                eq(SourceRecord.BOOKING_RECORD_TYPE),
                any(String.class),
                any(String.class)
        )).thenAnswer(invocation -> sourceRecords.stream()
                .filter(record -> record.externalRecordId().equals(invocation.getArgument(3)))
                .filter(record -> record.sourceVersion().equals(invocation.getArgument(4)))
                .findFirst());
        when(sourceRecordRepository.findById(any(UUID.class))).thenAnswer(invocation -> sourceRecords.stream()
                .filter(record -> record.id().equals(invocation.getArgument(0)))
                .findFirst());
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> {
            Booking booking = invocation.getArgument(0);
            bookings.add(booking);
            return booking;
        });
        when(bookingRepository.findByOrganisationIdAndSourceSystemIdAndExternalBookingId(
                eq(ORGANISATION_ID),
                eq(SOURCE_SYSTEM_ID),
                any(String.class)
        )).thenAnswer(invocation -> bookings.stream()
                .filter(booking -> booking.externalBookingId().equals(invocation.getArgument(2)))
                .findFirst());
        when(bookingItemRepository.save(any(BookingItem.class))).thenAnswer(invocation -> {
            BookingItem item = invocation.getArgument(0);
            bookingItems.add(item);
            return item;
        });
        when(bookingItemRepository.findByOrganisationIdAndBookingIdAndItemExternalId(
                eq(ORGANISATION_ID),
                any(UUID.class),
                any(String.class)
        )).thenAnswer(invocation -> bookingItems.stream()
                .filter(item -> item.bookingId().equals(invocation.getArgument(1)))
                .filter(item -> item.itemExternalId().equals(invocation.getArgument(2)))
                .findFirst());
    }

    @Test
    void importsValidBookingCsvAndCreatesCanonicalBookings() throws Exception {
        ImportBatch batch = service().importCsv(actor(), command("booking_valid_v1.csv"));

        assertThat(batch.status()).isEqualTo(ImportBatchStatus.COMPLETED);
        assertThat(batch.acceptedCount()).isEqualTo(3);
        assertThat(batch.totalCount()).isEqualTo(3);
        assertThat(sourceRecords).hasSize(3);
        assertThat(bookings).hasSize(3);
        assertThat(bookingItems).hasSize(3);
        assertThat(bookings.getFirst().externalBookingId()).isEqualTo("TL-BKG-1001");
        assertThat(bookings.getFirst().contractedSellingAmount()).isEqualByComparingTo(new BigDecimal("1000.00"));
        assertThat(rowResults).extracting(ImportRowResult::outcome)
                .containsOnly(ImportRowOutcome.ACCEPTED);
    }

    @Test
    void mixedRowsCompleteWithErrorsAndRecordFieldRejections() throws Exception {
        ImportBatch batch = service().importCsv(actor(), command("booking_mixed_rows.csv"));

        assertThat(batch.status()).isEqualTo(ImportBatchStatus.COMPLETED_WITH_ERRORS);
        assertThat(batch.acceptedCount()).isEqualTo(8);
        assertThat(batch.rejectedCount()).isEqualTo(2);
        assertThat(bookings).hasSize(8);
        assertThat(rowResults).filteredOn(result -> result.outcome() == ImportRowOutcome.REJECTED)
                .extracting(ImportRowResult::errorCode)
                .containsExactlyInAnyOrder("INVALID_BOOKING_DATE", "INVALID_CURRENCY_PRECISION");
    }

    @Test
    void duplicateReimportCreatesNoDuplicateBookingsOrItems() throws Exception {
        service().importCsv(actor(), command("booking_valid_v1.csv"));
        rowResults.clear();

        ImportBatch duplicateBatch = service().importCsv(actor(), command("booking_duplicate_reimport.csv"));

        assertThat(duplicateBatch.status()).isEqualTo(ImportBatchStatus.COMPLETED);
        assertThat(duplicateBatch.duplicateCount()).isEqualTo(3);
        assertThat(bookings).hasSize(3);
        assertThat(bookingItems).hasSize(3);
        assertThat(rowResults).extracting(ImportRowResult::outcome)
                .containsOnly(ImportRowOutcome.DUPLICATE);
    }

    @Test
    void staleSourceVersionDoesNotOverwriteCurrentBooking() throws Exception {
        service().importCsv(actor(), command("booking_valid_v1.csv"));
        Booking original = bookings.getFirst();
        SourceRecord versionThree = new SourceRecord(
                UUID.randomUUID(),
                ORGANISATION_ID,
                SOURCE_SYSTEM_ID,
                currentBatch.id(),
                SourceRecord.BOOKING_RECORD_TYPE,
                "TL-BKG-1001",
                "3",
                2,
                "sha256:current",
                null,
                NOW
        );
        sourceRecords.add(versionThree);
        original.applySource(
                versionThree.id(),
                original.bookingDate(),
                original.serviceStartDate(),
                original.serviceEndDate(),
                original.lifecycleStatus(),
                original.sellingCurrency(),
                original.contractedSellingAmount(),
                "CUST-1001",
                NOW
        );
        rowResults.clear();

        ImportBatch staleBatch = service().importCsv(actor(), command("booking_stale_version.csv"));

        assertThat(staleBatch.status()).isEqualTo(ImportBatchStatus.COMPLETED_WITH_ERRORS);
        assertThat(staleBatch.rejectedCount()).isEqualTo(1);
        assertThat(original.currentSourceRecordId()).isEqualTo(versionThree.id());
        assertThat(rowResults.getFirst().errorCode()).isEqualTo("STALE_SOURCE_VERSION");
    }

    @Test
    void unsupportedTemplateVersionFailsBatchBeforeDomainWrites() throws Exception {
        ImportBatch batch = service().importCsv(actor(), command("unsupported_template_version.csv"));

        assertThat(batch.status()).isEqualTo(ImportBatchStatus.FAILED);
        assertThat(batch.failureCode()).isEqualTo("UNSUPPORTED_TEMPLATE_VERSION");
        assertThat(bookings).isEmpty();
        assertThat(sourceRecords).isEmpty();
        assertThat(rowResults).isEmpty();
    }

    private BookingCsvImportService service() {
        ImportBatchService importBatchService = new ImportBatchService(
                importBatchRepository,
                importRowResultRepository,
                sourceSystemRepository,
                authorizationService,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
        return new BookingCsvImportService(
                importBatchService,
                sourceRecordRepository,
                bookingRepository,
                bookingItemRepository,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    private BookingCsvImportService.ImportBookingCsvCommand command(String fixtureName) throws Exception {
        return new BookingCsvImportService.ImportBookingCsvCommand(
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
                SourceSystemCategory.BOOKING_CHANNEL,
                "OTA_DEMO",
                "Europe/Istanbul",
                true,
                NOW
        );
    }

    private ActorContext actor() {
        return new ActorContext(
                USER_ID,
                ORGANISATION_ID,
                "Test User",
                UserRole.OPERATIONS,
                true,
                "corr-123"
        );
    }
}

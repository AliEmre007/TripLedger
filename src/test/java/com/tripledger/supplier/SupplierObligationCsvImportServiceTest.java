package com.tripledger.supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.tripledger.authorization.AuthorizationService;
import com.tripledger.booking.Booking;
import com.tripledger.booking.BookingItem;
import com.tripledger.booking.BookingItemRepository;
import com.tripledger.booking.BookingItemServiceType;
import com.tripledger.booking.BookingItemState;
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
class SupplierObligationCsvImportServiceTest {

    private static final UUID ORGANISATION_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID SOURCE_SYSTEM_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID BOOKING_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID BOOKING_ITEM_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final Instant NOW = Instant.parse("2026-07-14T06:00:00Z");
    private static final Path FIXTURE_ROOT = Path.of("src/test/resources/fixtures/validation-release/supplier");

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

    @Mock
    private SupplierRepository supplierRepository;

    @Mock
    private SupplierObligationRepository supplierObligationRepository;

    private final List<SourceRecord> sourceRecords = new ArrayList<>();
    private final List<Supplier> suppliers = new ArrayList<>();
    private final List<SupplierObligation> obligations = new ArrayList<>();
    private final List<ImportRowResult> rowResults = new ArrayList<>();
    private ImportBatch currentBatch;
    private Booking booking;
    private BookingItem bookingItem;

    @BeforeEach
    void setUp() {
        booking = booking();
        bookingItem = bookingItem();
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
                eq(SourceRecord.SUPPLIER_OBLIGATION_RECORD_TYPE),
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
        when(bookingItemRepository.findByOrganisationIdAndBookingIdAndItemExternalId(
                eq(ORGANISATION_ID),
                eq(BOOKING_ID),
                any(String.class)
        )).thenAnswer(invocation -> "TL-BKG-1001-ROOM".equals(invocation.getArgument(2))
                ? Optional.of(bookingItem)
                : Optional.empty());
        when(supplierRepository.findByOrganisationIdAndExternalReference(
                eq(ORGANISATION_ID),
                any(String.class)
        )).thenAnswer(invocation -> suppliers.stream()
                .filter(supplier -> supplier.externalReference().equals(invocation.getArgument(1)))
                .findFirst());
        when(supplierRepository.save(any(Supplier.class))).thenAnswer(invocation -> {
            Supplier supplier = invocation.getArgument(0);
            suppliers.add(supplier);
            return supplier;
        });
        when(supplierObligationRepository.save(any(SupplierObligation.class))).thenAnswer(invocation -> {
            SupplierObligation obligation = invocation.getArgument(0);
            obligations.add(obligation);
            return obligation;
        });
    }

    @Test
    void importsLinkedAndUnlinkedSupplierObligations() throws Exception {
        ImportBatch batch = service().importCsv(actor(), command("supplier_obligations.csv"));

        assertThat(batch.status()).isEqualTo(ImportBatchStatus.COMPLETED);
        assertThat(batch.acceptedCount()).isEqualTo(2);
        assertThat(sourceRecords).hasSize(2);
        assertThat(suppliers).hasSize(2);
        assertThat(obligations).hasSize(2);
        assertThat(obligations.getFirst().bookingId()).isEqualTo(BOOKING_ID);
        assertThat(obligations.getFirst().bookingItemId()).isEqualTo(BOOKING_ITEM_ID);
        assertThat(obligations.getFirst().amount()).isEqualByComparingTo(new BigDecimal("500.00"));
        assertThat(obligations.getFirst().contributesToActiveSupplierCost()).isTrue();
        assertThat(obligations.get(1).bookingId()).isNull();
        assertThat(obligations.get(1).bookingItemId()).isNull();
        assertThat(obligations.get(1).contributesToActiveSupplierCost()).isFalse();
        assertThat(rowResults).extracting(ImportRowResult::outcome).containsOnly(ImportRowOutcome.ACCEPTED);
    }

    @Test
    void duplicateReimportCreatesNoDuplicateObligations() throws Exception {
        service().importCsv(actor(), command("supplier_obligations.csv"));
        rowResults.clear();

        ImportBatch duplicateBatch = service().importCsv(actor(), command("supplier_obligations.csv"));

        assertThat(duplicateBatch.status()).isEqualTo(ImportBatchStatus.COMPLETED);
        assertThat(duplicateBatch.duplicateCount()).isEqualTo(2);
        assertThat(obligations).hasSize(2);
        assertThat(rowResults).extracting(ImportRowResult::outcome).containsOnly(ImportRowOutcome.DUPLICATE);
    }

    @Test
    void invalidMoneyIsRejectedWithoutCreatingObligation() {
        String csv = """
                template_type,template_version,external_obligation_id,source_version,source_row_number,\
                supplier_reference,supplier_name,status,amount,currency
                SUPPLIER_OBLIGATION,1,SUP-NEG,1,2,HOTEL-ALBA,Hotel Alba,CONFIRMED,-1.00,EUR
                SUPPLIER_OBLIGATION,1,SUP-PRECISION,1,3,HOTEL-ALBA,Hotel Alba,CONFIRMED,1.001,EUR
                """;

        ImportBatch batch = service().importCsv(actor(), new SupplierObligationCsvImportService
                .ImportSupplierObligationCsvCommand(SOURCE_SYSTEM_ID, "invalid.csv", "sha256:invalid", csv));

        assertThat(batch.status()).isEqualTo(ImportBatchStatus.COMPLETED_WITH_ERRORS);
        assertThat(batch.rejectedCount()).isEqualTo(2);
        assertThat(obligations).isEmpty();
        assertThat(rowResults).extracting(ImportRowResult::errorCode)
                .containsExactly("INVALID_FIELD_TYPE", "INVALID_CURRENCY_PRECISION");
    }

    private SupplierObligationCsvImportService service() {
        ImportBatchService importBatchService = new ImportBatchService(
                importBatchRepository,
                importRowResultRepository,
                sourceSystemRepository,
                authorizationService,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
        return new SupplierObligationCsvImportService(
                importBatchService,
                sourceRecordRepository,
                bookingRepository,
                bookingItemRepository,
                supplierRepository,
                supplierObligationRepository,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    private SupplierObligationCsvImportService.ImportSupplierObligationCsvCommand command(String fixtureName)
            throws Exception {
        return new SupplierObligationCsvImportService.ImportSupplierObligationCsvCommand(
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
                "Supplier Export",
                SourceSystemCategory.SUPPLIER,
                "SUPPLIER_DEMO",
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

    private BookingItem bookingItem() {
        return new BookingItem(
                BOOKING_ITEM_ID,
                ORGANISATION_ID,
                BOOKING_ID,
                null,
                "TL-BKG-1001-ROOM",
                BookingItemServiceType.HOTEL,
                LocalDate.parse("2026-08-10"),
                LocalDate.parse("2026-08-15"),
                new BigDecimal("1000.00"),
                "EUR",
                BookingItemState.ACTIVE
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

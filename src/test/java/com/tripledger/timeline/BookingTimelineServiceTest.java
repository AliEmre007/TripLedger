package com.tripledger.timeline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tripledger.audit.AuditEvent;
import com.tripledger.audit.AuditEventRepository;
import com.tripledger.audit.AuditOutcome;
import com.tripledger.audit.AuditService;
import com.tripledger.authorization.AuthorizationService;
import com.tripledger.authorization.Permission;
import com.tripledger.booking.Booking;
import com.tripledger.booking.BookingLifecycleStatus;
import com.tripledger.booking.BookingRepository;
import com.tripledger.common.api.ApiException;
import com.tripledger.discrepancy.Discrepancy;
import com.tripledger.discrepancy.DiscrepancyRepository;
import com.tripledger.discrepancy.DiscrepancySeverity;
import com.tripledger.discrepancy.DiscrepancyStatus;
import com.tripledger.discrepancy.DiscrepancyType;
import com.tripledger.economics.CalculationSnapshot;
import com.tripledger.economics.CalculationSnapshotRepository;
import com.tripledger.economics.CalculationStatus;
import com.tripledger.finance.FinancialEvent;
import com.tripledger.finance.FinancialEventDirection;
import com.tripledger.finance.FinancialEventRepository;
import com.tripledger.finance.FinancialEventType;
import com.tripledger.identity.ActorContext;
import com.tripledger.identity.UserRole;
import com.tripledger.ingestion.SourceRecordRepository;
import com.tripledger.matching.BookingMatchRepository;
import com.tripledger.reconciliation.ReconciliationResultRepository;
import com.tripledger.supplier.SupplierObligationRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class BookingTimelineServiceTest {

    private static final UUID ORGANISATION_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID BOOKING_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID SOURCE_RECORD_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID EVENT_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final UUID SNAPSHOT_ID = UUID.fromString("66666666-6666-6666-6666-666666666666");
    private static final UUID DISCREPANCY_ID = UUID.fromString("77777777-7777-7777-7777-777777777777");
    private static final UUID AUDIT_ID = UUID.fromString("88888888-8888-8888-8888-888888888888");

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private SourceRecordRepository sourceRecordRepository;

    @Mock
    private SupplierObligationRepository supplierObligationRepository;

    @Mock
    private FinancialEventRepository financialEventRepository;

    @Mock
    private CalculationSnapshotRepository calculationSnapshotRepository;

    @Mock
    private BookingMatchRepository bookingMatchRepository;

    @Mock
    private ReconciliationResultRepository reconciliationResultRepository;

    @Mock
    private DiscrepancyRepository discrepancyRepository;

    @Mock
    private AuditEventRepository auditEventRepository;

    @Mock
    private AuthorizationService authorizationService;

    @Test
    void returnsChronologicalTimelineWithDistinctCategories() {
        when(bookingRepository.findByOrganisationIdAndId(ORGANISATION_ID, BOOKING_ID))
                .thenReturn(Optional.of(booking()));
        when(sourceRecordRepository.findBookingTimelineRecords(ORGANISATION_ID, BOOKING_ID, SOURCE_RECORD_ID))
                .thenReturn(List.of());
        when(supplierObligationRepository.findAllByOrganisationIdAndBookingIdOrderByCreatedAtAsc(
                ORGANISATION_ID,
                BOOKING_ID
        )).thenReturn(List.of());
        when(financialEventRepository.findAllByOrganisationIdAndBookingIdOrderByEffectiveAtAsc(
                ORGANISATION_ID,
                BOOKING_ID
        )).thenReturn(List.of(financialEvent()));
        when(calculationSnapshotRepository.findAllByOrganisationIdAndBookingIdOrderByCreatedAtAsc(
                ORGANISATION_ID,
                BOOKING_ID
        )).thenReturn(List.of(calculation()));
        when(bookingMatchRepository.findAllByOrganisationIdAndBookingIdOrderByCreatedAtAsc(
                ORGANISATION_ID,
                BOOKING_ID
        )).thenReturn(List.of());
        when(reconciliationResultRepository.findAllByOrganisationIdAndBookingIdOrderByCreatedAtAsc(
                ORGANISATION_ID,
                BOOKING_ID
        )).thenReturn(List.of());
        when(discrepancyRepository.findAllByOrganisationIdAndBookingIdOrderByCreatedAtAsc(
                ORGANISATION_ID,
                BOOKING_ID
        )).thenReturn(List.of(discrepancy()));
        when(auditEventRepository.findAllByOrganisationIdAndTargetTypeAndTargetIdOrderByCreatedAtAsc(
                ORGANISATION_ID,
                AuditService.TARGET_BOOKING,
                BOOKING_ID
        )).thenReturn(List.of(auditEvent()));

        BookingTimeline timeline = service().get(actor(), BOOKING_ID);

        assertThat(timeline.bookingId()).isEqualTo(BOOKING_ID);
        assertThat(timeline.events())
                .extracting(TimelineEvent::eventType)
                .containsExactly(
                        "BOOKING_IMPORTED",
                        "FINANCIAL_EVENT_ACCEPTED",
                        "ECONOMICS_CALCULATED",
                        "DISCREPANCY_RECORDED",
                        "AUDIT_EVENT_RECORDED"
                );
        assertThat(timeline.events())
                .extracting(TimelineEvent::category)
                .containsExactly(
                        TimelineEventCategory.SOURCE,
                        TimelineEventCategory.SOURCE,
                        TimelineEventCategory.SYSTEM,
                        TimelineEventCategory.SYSTEM,
                        TimelineEventCategory.USER
                );
        verify(authorizationService).require(actor(), Permission.PROTECTED_READ);
    }

    @Test
    void returnsNotFoundForMissingOrWrongOrganisationBooking() {
        when(bookingRepository.findByOrganisationIdAndId(ORGANISATION_ID, BOOKING_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().get(actor(), BOOKING_ID))
                .isInstanceOfSatisfying(ApiException.class, exception -> {
                    assertThat(exception.status()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(exception.code()).isEqualTo("BOOKING_NOT_FOUND");
                });
    }

    private BookingTimelineService service() {
        return new BookingTimelineService(
                bookingRepository,
                sourceRecordRepository,
                supplierObligationRepository,
                financialEventRepository,
                calculationSnapshotRepository,
                bookingMatchRepository,
                reconciliationResultRepository,
                discrepancyRepository,
                auditEventRepository,
                authorizationService
        );
    }

    private Booking booking() {
        return new Booking(
                BOOKING_ID,
                ORGANISATION_ID,
                UUID.randomUUID(),
                "TL-BKG-1001",
                SOURCE_RECORD_ID,
                LocalDate.parse("2026-07-01"),
                LocalDate.parse("2026-08-01"),
                LocalDate.parse("2026-08-07"),
                BookingLifecycleStatus.CONFIRMED,
                "EUR",
                new BigDecimal("1000.00"),
                "CUST-1001",
                Instant.parse("2026-07-01T10:00:00Z"),
                Instant.parse("2026-07-01T10:00:00Z")
        );
    }

    private FinancialEvent financialEvent() {
        return new FinancialEvent(
                EVENT_ID,
                ORGANISATION_ID,
                UUID.randomUUID(),
                BOOKING_ID,
                FinancialEventType.CUSTOMER_PAYMENT,
                FinancialEventDirection.INCREASE_RECEIVED,
                new BigDecimal("950.00"),
                "EUR",
                Instant.parse("2026-07-02T08:00:00Z"),
                "PAY-1001",
                null,
                null,
                null,
                Instant.parse("2026-07-02T08:01:00Z")
        );
    }

    private CalculationSnapshot calculation() {
        return new CalculationSnapshot(
                SNAPSHOT_ID,
                ORGANISATION_ID,
                BOOKING_ID,
                "economics-v1",
                new BigDecimal("1000.00"),
                new BigDecimal("950.00"),
                new BigDecimal("50.00"),
                new BigDecimal("600.00"),
                new BigDecimal("350.00"),
                "EUR",
                CalculationStatus.READY,
                "",
                Instant.parse("2026-07-03T08:00:00Z")
        );
    }

    private Discrepancy discrepancy() {
        return new Discrepancy(
                DISCREPANCY_ID,
                ORGANISATION_ID,
                BOOKING_ID,
                DiscrepancyType.SHORT_SETTLEMENT,
                DiscrepancySeverity.HIGH,
                "EXPECTED_CUSTOMER_RECEIVABLE",
                "expected=950.00;matched=900.00;currency=EUR",
                new BigDecimal("50.00"),
                "EUR",
                DiscrepancyStatus.ACTIVE,
                null,
                "Expected EUR 950.00 but matched EUR 900.00; variance EUR 50.00.",
                Instant.parse("2026-07-04T08:00:00Z"),
                null
        );
    }

    private AuditEvent auditEvent() {
        return new AuditEvent(
                AUDIT_ID,
                ORGANISATION_ID,
                USER_ID,
                null,
                "FINANCIAL_EVENT_REVERSED",
                AuditService.TARGET_BOOKING,
                BOOKING_ID,
                AuditOutcome.SUCCESS,
                "financial_event:" + EVENT_ID,
                "financial_event:" + UUID.randomUUID(),
                "Corrected duplicate payment.",
                "corr-123",
                Instant.parse("2026-07-05T08:00:00Z")
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

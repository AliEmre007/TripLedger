package com.tripledger.reconciliation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tripledger.authorization.AuthorizationService;
import com.tripledger.authorization.Permission;
import com.tripledger.booking.Booking;
import com.tripledger.booking.BookingLifecycleStatus;
import com.tripledger.booking.BookingRepository;
import com.tripledger.common.api.ApiException;
import com.tripledger.economics.BookingEconomicsDetail;
import com.tripledger.economics.BookingEconomicsService;
import com.tripledger.economics.CalculationStatus;
import com.tripledger.identity.ActorContext;
import com.tripledger.identity.UserRole;
import com.tripledger.matching.BookingMatch;
import com.tripledger.matching.BookingMatchRepository;
import com.tripledger.matching.MatchAllocation;
import com.tripledger.matching.MatchAllocationRepository;
import com.tripledger.matching.MatchStatus;
import com.tripledger.matching.MatchType;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class ReconciliationServiceTest {

    private static final UUID ORGANISATION_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID BOOKING_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID SNAPSHOT_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID MATCH_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final UUID RESULT_ID = UUID.fromString("66666666-6666-6666-6666-666666666666");
    private static final Instant NOW = Instant.parse("2026-07-14T06:00:00Z");

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private BookingEconomicsService bookingEconomicsService;

    @Mock
    private BookingMatchRepository bookingMatchRepository;

    @Mock
    private MatchAllocationRepository matchAllocationRepository;

    @Mock
    private ReconciliationResultRepository reconciliationResultRepository;

    @Mock
    private AuthorizationService authorizationService;

    @Test
    void reconcilesBookingWhenExpectedAmountIsFullyMatched() {
        arrangeBookingAndEconomics(CalculationStatus.READY, new BigDecimal("950.00"));
        when(bookingMatchRepository.findAllByOrganisationIdAndBookingIdOrderByCreatedAtDesc(
                ORGANISATION_ID,
                BOOKING_ID))
                .thenReturn(List.of(match(MatchStatus.ACTIVE)));
        when(matchAllocationRepository.findAllByOrganisationIdAndMatchIdInAndActiveTrue(
                ORGANISATION_ID,
                List.of(MATCH_ID)))
                .thenReturn(List.of(allocation(new BigDecimal("950.00"))));
        when(reconciliationResultRepository
                .findFirstByOrganisationIdAndBookingIdAndSupersededAtIsNullOrderByCreatedAtDesc(
                        ORGANISATION_ID,
                        BOOKING_ID))
                .thenReturn(Optional.empty());
        when(reconciliationResultRepository.save(any(ReconciliationResult.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ReconciliationResultDetail result = service().run(actor(), BOOKING_ID);

        assertThat(result.status()).isEqualTo(ReconciliationStatus.RECONCILED);
        assertThat(result.expectedAmount()).isEqualByComparingTo(new BigDecimal("950.00"));
        assertThat(result.matchedAmount()).isEqualByComparingTo(new BigDecimal("950.00"));
        assertThat(result.varianceAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        verify(authorizationService).require(actor(), Permission.FINANCIAL_ACTION);
    }

    @Test
    void partiallyReconcilesWhenSomeAmountIsMatched() {
        arrangeBookingAndEconomics(CalculationStatus.READY, new BigDecimal("950.00"));
        when(bookingMatchRepository.findAllByOrganisationIdAndBookingIdOrderByCreatedAtDesc(
                ORGANISATION_ID,
                BOOKING_ID))
                .thenReturn(List.of(match(MatchStatus.ACTIVE)));
        when(matchAllocationRepository.findAllByOrganisationIdAndMatchIdInAndActiveTrue(
                ORGANISATION_ID,
                List.of(MATCH_ID)))
                .thenReturn(List.of(allocation(new BigDecimal("500.00"))));
        when(reconciliationResultRepository
                .findFirstByOrganisationIdAndBookingIdAndSupersededAtIsNullOrderByCreatedAtDesc(
                        ORGANISATION_ID,
                        BOOKING_ID))
                .thenReturn(Optional.empty());
        when(reconciliationResultRepository.save(any(ReconciliationResult.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ReconciliationResultDetail result = service().run(actor(), BOOKING_ID);

        assertThat(result.status()).isEqualTo(ReconciliationStatus.PARTIALLY_RECONCILED);
        assertThat(result.varianceAmount()).isEqualByComparingTo(new BigDecimal("450.00"));
    }

    @Test
    void ambiguousMatchProducesDiscrepantState() {
        arrangeBookingAndEconomics(CalculationStatus.READY, new BigDecimal("950.00"));
        when(bookingMatchRepository.findAllByOrganisationIdAndBookingIdOrderByCreatedAtDesc(
                ORGANISATION_ID,
                BOOKING_ID))
                .thenReturn(List.of(match(MatchStatus.REVIEW_REQUIRED)));
        when(reconciliationResultRepository
                .findFirstByOrganisationIdAndBookingIdAndSupersededAtIsNullOrderByCreatedAtDesc(
                        ORGANISATION_ID,
                        BOOKING_ID))
                .thenReturn(Optional.empty());
        when(reconciliationResultRepository.save(any(ReconciliationResult.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ReconciliationResultDetail result = service().run(actor(), BOOKING_ID);

        assertThat(result.status()).isEqualTo(ReconciliationStatus.DISCREPANT);
    }

    @Test
    void notReadyEconomicsProducesNotReadyState() {
        arrangeBookingAndEconomics(CalculationStatus.NOT_READY, null);
        when(bookingMatchRepository.findAllByOrganisationIdAndBookingIdOrderByCreatedAtDesc(
                ORGANISATION_ID,
                BOOKING_ID))
                .thenReturn(List.of());
        when(reconciliationResultRepository
                .findFirstByOrganisationIdAndBookingIdAndSupersededAtIsNullOrderByCreatedAtDesc(
                        ORGANISATION_ID,
                        BOOKING_ID))
                .thenReturn(Optional.empty());
        when(reconciliationResultRepository.save(any(ReconciliationResult.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ReconciliationResultDetail result = service().run(actor(), BOOKING_ID);

        assertThat(result.status()).isEqualTo(ReconciliationStatus.NOT_READY);
        assertThat(result.expectedAmount()).isNull();
        assertThat(result.varianceAmount()).isNull();
    }

    @Test
    void laterChangedInputsSupersedePriorReconciledResult() {
        arrangeBookingAndEconomics(CalculationStatus.READY, new BigDecimal("1000.00"));
        ReconciliationResult previous = new ReconciliationResult(
                RESULT_ID,
                ORGANISATION_ID,
                BOOKING_ID,
                SNAPSHOT_ID,
                "reconciliation-v1",
                ReconciliationStatus.RECONCILED,
                BigDecimal.ZERO.setScale(2),
                "EUR",
                NOW.minusSeconds(60),
                null
        );
        when(bookingMatchRepository.findAllByOrganisationIdAndBookingIdOrderByCreatedAtDesc(
                ORGANISATION_ID,
                BOOKING_ID))
                .thenReturn(List.of(match(MatchStatus.ACTIVE)));
        when(matchAllocationRepository.findAllByOrganisationIdAndMatchIdInAndActiveTrue(
                ORGANISATION_ID,
                List.of(MATCH_ID)))
                .thenReturn(List.of(allocation(new BigDecimal("950.00"))));
        when(reconciliationResultRepository
                .findFirstByOrganisationIdAndBookingIdAndSupersededAtIsNullOrderByCreatedAtDesc(
                        ORGANISATION_ID,
                        BOOKING_ID))
                .thenReturn(Optional.of(previous));
        when(reconciliationResultRepository.save(any(ReconciliationResult.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ReconciliationResultDetail result = service().run(actor(), BOOKING_ID);

        assertThat(result.status()).isEqualTo(ReconciliationStatus.PARTIALLY_RECONCILED);
        assertThat(previous.supersededAt()).isEqualTo(NOW);
    }

    @Test
    void crossOrganisationBookingReturnsNotFound() {
        when(bookingRepository.findByOrganisationIdAndId(ORGANISATION_ID, BOOKING_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().run(actor(), BOOKING_ID))
                .isInstanceOfSatisfying(ApiException.class, exception -> {
                    assertThat(exception.status()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(exception.code()).isEqualTo("BOOKING_NOT_FOUND");
                });
    }

    private void arrangeBookingAndEconomics(CalculationStatus status, BigDecimal expectedAmount) {
        when(bookingRepository.findByOrganisationIdAndId(ORGANISATION_ID, BOOKING_ID))
                .thenReturn(Optional.of(booking()));
        when(bookingEconomicsService.calculate(actor(), BOOKING_ID))
                .thenReturn(new BookingEconomicsDetail(
                        SNAPSHOT_ID,
                        BOOKING_ID,
                        "economics-v1",
                        "EUR",
                        new BigDecimal("1000.00"),
                        expectedAmount,
                        BigDecimal.ZERO.setScale(2),
                        status == CalculationStatus.READY ? new BigDecimal("1.00") : null,
                        null,
                        status,
                        status == CalculationStatus.READY ? List.of() : List.of("ACTIVE_SUPPLIER_COST"),
                        NOW
                ));
    }

    private ReconciliationService service() {
        return new ReconciliationService(
                bookingRepository,
                bookingEconomicsService,
                bookingMatchRepository,
                matchAllocationRepository,
                reconciliationResultRepository,
                authorizationService,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    private Booking booking() {
        return new Booking(
                BOOKING_ID,
                ORGANISATION_ID,
                UUID.randomUUID(),
                "TL-BKG-1001",
                UUID.randomUUID(),
                LocalDate.parse("2026-07-01"),
                LocalDate.parse("2026-08-01"),
                LocalDate.parse("2026-08-07"),
                BookingLifecycleStatus.CONFIRMED,
                "EUR",
                new BigDecimal("1000.00"),
                "CUST-1001",
                NOW,
                NOW
        );
    }

    private BookingMatch match(MatchStatus status) {
        return new BookingMatch(
                MATCH_ID,
                ORGANISATION_ID,
                BOOKING_ID,
                MatchType.AUTOMATIC,
                status == MatchStatus.ACTIVE ? "EXACT_BOOKING_AMOUNT" : "AMBIGUOUS_MATCH",
                status,
                USER_ID,
                NOW,
                null,
                status == MatchStatus.ACTIVE ? null : "AMBIGUOUS_MATCH"
        );
    }

    private MatchAllocation allocation(BigDecimal amount) {
        return new MatchAllocation(
                UUID.randomUUID(),
                ORGANISATION_ID,
                MATCH_ID,
                UUID.randomUUID(),
                amount,
                "EUR",
                true,
                null,
                amount,
                "EUR"
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

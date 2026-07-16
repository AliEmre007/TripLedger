package com.tripledger.matching;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tripledger.audit.AuditService;
import com.tripledger.authorization.AuthorizationService;
import com.tripledger.authorization.Permission;
import com.tripledger.booking.Booking;
import com.tripledger.booking.BookingLifecycleStatus;
import com.tripledger.booking.BookingRepository;
import com.tripledger.common.api.ApiException;
import com.tripledger.economics.BookingEconomicsDetail;
import com.tripledger.economics.BookingEconomicsService;
import com.tripledger.economics.CalculationStatus;
import com.tripledger.finance.ExchangeRateEvidence;
import com.tripledger.finance.ExchangeRateEvidenceRepository;
import com.tripledger.finance.FinancialEvent;
import com.tripledger.finance.FinancialEventDirection;
import com.tripledger.finance.FinancialEventRepository;
import com.tripledger.finance.FinancialEventType;
import com.tripledger.identity.ActorContext;
import com.tripledger.identity.UserRole;
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
class DeterministicMatcherServiceTest {

    private static final UUID ORGANISATION_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID BOOKING_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID EVENT_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID EVENT_ID_TWO = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final UUID EXCHANGE_RATE_ID = UUID.fromString("66666666-6666-6666-6666-666666666666");
    private static final Instant NOW = Instant.parse("2026-07-14T06:00:00Z");

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private FinancialEventRepository financialEventRepository;

    @Mock
    private ExchangeRateEvidenceRepository exchangeRateEvidenceRepository;

    @Mock
    private BookingMatchRepository bookingMatchRepository;

    @Mock
    private MatchAllocationRepository matchAllocationRepository;

    @Mock
    private BookingEconomicsService bookingEconomicsService;

    @Mock
    private AuthorizationService authorizationService;

    @Mock
    private AuditService auditService;

    @Test
    void createsAutomaticMatchForSingleExactCandidate() {
        arrangeBookingAndReadyEconomics("950.00");
        FinancialEvent event = event(EVENT_ID, new BigDecimal("950.00"), "EUR");
        when(financialEventRepository.findAllByOrganisationIdAndBookingIdOrderByEffectiveAtAsc(
                ORGANISATION_ID,
                BOOKING_ID))
                .thenReturn(List.of(event));
        when(matchAllocationRepository.existsByOrganisationIdAndFinancialEventIdAndActiveTrue(
                ORGANISATION_ID,
                EVENT_ID))
                .thenReturn(false);
        when(bookingMatchRepository.save(any(BookingMatch.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(matchAllocationRepository.save(any(MatchAllocation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        MatchingRunResult result = service().run(actor(), BOOKING_ID);

        assertThat(result.status()).isEqualTo(MatchStatus.ACTIVE);
        assertThat(result.ruleCode()).isEqualTo("EXACT_BOOKING_AMOUNT");
        assertThat(result.financialEventId()).isEqualTo(EVENT_ID);
        assertThat(result.amount()).isEqualByComparingTo(new BigDecimal("950.00"));
        assertThat(result.currency()).isEqualTo("EUR");
        verify(authorizationService).require(actor(), Permission.FINANCIAL_ACTION);
        verify(matchAllocationRepository).save(any(MatchAllocation.class));
        verify(auditService).recordSuccess(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
        );
    }

    @Test
    void ambiguousCandidatesCreateReviewRecordWithoutAllocation() {
        arrangeBookingAndReadyEconomics("950.00");
        when(financialEventRepository.findAllByOrganisationIdAndBookingIdOrderByEffectiveAtAsc(
                ORGANISATION_ID,
                BOOKING_ID))
                .thenReturn(List.of(
                        event(EVENT_ID, new BigDecimal("950.00"), "EUR"),
                        event(EVENT_ID_TWO, new BigDecimal("950.00"), "EUR")
                ));
        when(bookingMatchRepository.save(any(BookingMatch.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        MatchingRunResult result = service().run(actor(), BOOKING_ID);

        assertThat(result.status()).isEqualTo(MatchStatus.REVIEW_REQUIRED);
        assertThat(result.ruleCode()).isEqualTo("AMBIGUOUS_MATCH");
        assertThat(result.reason()).isEqualTo("AMBIGUOUS_MATCH");
        verify(matchAllocationRepository, never()).save(any(MatchAllocation.class));
    }

    @Test
    void usesExchangeRateEvidenceForCrossCurrencyCandidate() {
        arrangeBookingAndReadyEconomics("100.00");
        FinancialEvent event = event(EVENT_ID, new BigDecimal("3500.00"), "TRY");
        when(financialEventRepository.findAllByOrganisationIdAndBookingIdOrderByEffectiveAtAsc(
                ORGANISATION_ID,
                BOOKING_ID))
                .thenReturn(List.of(event));
        when(exchangeRateEvidenceRepository
                .findAllByOrganisationIdAndFinancialEventIdOrderByEffectiveAtDescCreatedAtDesc(
                        ORGANISATION_ID,
                        EVENT_ID))
                .thenReturn(List.of(exchangeRate()));
        when(bookingMatchRepository.save(any(BookingMatch.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(matchAllocationRepository.save(any(MatchAllocation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        MatchingRunResult result = service().run(actor(), BOOKING_ID);

        assertThat(result.status()).isEqualTo(MatchStatus.ACTIVE);
        assertThat(result.amount()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(result.currency()).isEqualTo("EUR");
        assertThat(result.originalAmount()).isEqualByComparingTo(new BigDecimal("3500.00"));
        assertThat(result.originalCurrency()).isEqualTo("TRY");
        assertThat(result.exchangeRateId()).isEqualTo(EXCHANGE_RATE_ID);
    }

    @Test
    void doesNotMatchCrossCurrencyCandidateWithoutRateEvidence() {
        arrangeBookingAndReadyEconomics("100.00");
        when(financialEventRepository.findAllByOrganisationIdAndBookingIdOrderByEffectiveAtAsc(
                ORGANISATION_ID,
                BOOKING_ID))
                .thenReturn(List.of(event(EVENT_ID, new BigDecimal("3500.00"), "TRY")));
        when(exchangeRateEvidenceRepository
                .findAllByOrganisationIdAndFinancialEventIdOrderByEffectiveAtDescCreatedAtDesc(
                        ORGANISATION_ID,
                        EVENT_ID))
                .thenReturn(List.of());

        MatchingRunResult result = service().run(actor(), BOOKING_ID);

        assertThat(result.status()).isEqualTo(MatchStatus.REVIEW_REQUIRED);
        assertThat(result.matchId()).isNull();
        assertThat(result.reason()).isEqualTo("No unique deterministic candidate.");
        verify(matchAllocationRepository, never()).save(any(MatchAllocation.class));
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

    private void arrangeBookingAndReadyEconomics(String expectedAmount) {
        when(bookingRepository.findByOrganisationIdAndId(ORGANISATION_ID, BOOKING_ID))
                .thenReturn(Optional.of(booking()));
        when(bookingEconomicsService.calculate(actor(), BOOKING_ID))
                .thenReturn(new BookingEconomicsDetail(
                        UUID.randomUUID(),
                        BOOKING_ID,
                        "economics-v1",
                        "EUR",
                        new BigDecimal("1000.00"),
                        new BigDecimal(expectedAmount),
                        new BigDecimal("0.00"),
                        new BigDecimal("1.00"),
                        new BigDecimal(expectedAmount).subtract(new BigDecimal("1.00")),
                        CalculationStatus.READY,
                        List.of(),
                        NOW
                ));
    }

    private DeterministicMatcherService service() {
        return new DeterministicMatcherService(
                bookingRepository,
                financialEventRepository,
                exchangeRateEvidenceRepository,
                bookingMatchRepository,
                matchAllocationRepository,
                bookingEconomicsService,
                authorizationService,
                auditService,
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

    private FinancialEvent event(UUID id, BigDecimal amount, String currency) {
        return new FinancialEvent(
                id,
                ORGANISATION_ID,
                UUID.randomUUID(),
                BOOKING_ID,
                FinancialEventType.CUSTOMER_PAYMENT,
                FinancialEventDirection.INCREASE_RECEIVED,
                amount,
                currency,
                Instant.parse("2026-07-02T10:00:00Z"),
                "PAY-" + id,
                null,
                null,
                USER_ID,
                NOW
        );
    }

    private ExchangeRateEvidence exchangeRate() {
        return new ExchangeRateEvidence(
                EXCHANGE_RATE_ID,
                ORGANISATION_ID,
                EVENT_ID,
                new BigDecimal("3500.00"),
                "TRY",
                new BigDecimal("100.00"),
                "EUR",
                new BigDecimal("0.028571428600"),
                NOW,
                "manual-finance-evidence",
                "HALF_UP-v1",
                USER_ID,
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

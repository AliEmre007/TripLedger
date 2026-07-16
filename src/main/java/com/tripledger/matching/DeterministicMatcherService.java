package com.tripledger.matching;

import com.tripledger.audit.AuditService;
import com.tripledger.authorization.AuthorizationService;
import com.tripledger.authorization.Permission;
import com.tripledger.booking.Booking;
import com.tripledger.booking.BookingRepository;
import com.tripledger.common.api.ApiException;
import com.tripledger.economics.BookingEconomicsDetail;
import com.tripledger.economics.BookingEconomicsService;
import com.tripledger.economics.CalculationStatus;
import com.tripledger.finance.ExchangeRateEvidence;
import com.tripledger.finance.ExchangeRateEvidenceRepository;
import com.tripledger.finance.FinancialEvent;
import com.tripledger.finance.FinancialEventRepository;
import com.tripledger.finance.FinancialEventType;
import com.tripledger.identity.ActorContext;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeterministicMatcherService {

    static final String RULE_EXACT_BOOKING_AMOUNT = "EXACT_BOOKING_AMOUNT";
    static final String RULE_AMBIGUOUS_MATCH = "AMBIGUOUS_MATCH";

    private final BookingRepository bookingRepository;
    private final FinancialEventRepository financialEventRepository;
    private final ExchangeRateEvidenceRepository exchangeRateEvidenceRepository;
    private final BookingMatchRepository bookingMatchRepository;
    private final MatchAllocationRepository matchAllocationRepository;
    private final BookingEconomicsService bookingEconomicsService;
    private final AuthorizationService authorizationService;
    private final AuditService auditService;
    private final Clock clock;

    public DeterministicMatcherService(BookingRepository bookingRepository,
                                       FinancialEventRepository financialEventRepository,
                                       ExchangeRateEvidenceRepository exchangeRateEvidenceRepository,
                                       BookingMatchRepository bookingMatchRepository,
                                       MatchAllocationRepository matchAllocationRepository,
                                       BookingEconomicsService bookingEconomicsService,
                                       AuthorizationService authorizationService,
                                       AuditService auditService,
                                       Clock clock) {
        this.bookingRepository = bookingRepository;
        this.financialEventRepository = financialEventRepository;
        this.exchangeRateEvidenceRepository = exchangeRateEvidenceRepository;
        this.bookingMatchRepository = bookingMatchRepository;
        this.matchAllocationRepository = matchAllocationRepository;
        this.bookingEconomicsService = bookingEconomicsService;
        this.authorizationService = authorizationService;
        this.auditService = auditService;
        this.clock = clock;
    }

    @Transactional
    public MatchingRunResult run(ActorContext actor, UUID bookingId) {
        authorizationService.require(actor, Permission.FINANCIAL_ACTION);
        Booking booking = bookingRepository.findByOrganisationIdAndId(actor.organisationId(), bookingId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "BOOKING_NOT_FOUND",
                        "Booking was not found."
                ));
        BookingEconomicsDetail economics = bookingEconomicsService.calculate(actor, booking.id());
        if (economics.status() != CalculationStatus.READY || economics.expectedCustomerReceivable() == null) {
            return reviewRequired(actor, booking, "Expected customer receivable is not ready.");
        }

        List<Candidate> candidates = financialEventRepository
                .findAllByOrganisationIdAndBookingIdOrderByEffectiveAtAsc(actor.organisationId(), booking.id())
                .stream()
                .filter(this::compatibleEventType)
                .filter(event -> !matchAllocationRepository.existsByOrganisationIdAndFinancialEventIdAndActiveTrue(
                        actor.organisationId(),
                        event.id()))
                .filter(event -> withinWindow(booking, event))
                .map(event -> candidate(event, economics.expectedCustomerReceivable(), economics.currency()))
                .filter(Objects::nonNull)
                .toList();

        if (candidates.size() == 1) {
            return createAutomaticMatch(actor, booking, candidates.getFirst());
        }
        if (candidates.size() > 1) {
            return reviewRequired(actor, booking, "AMBIGUOUS_MATCH");
        }
        auditService.recordSuccess(
                actor,
                "MATCHING_RUN_COMPLETED",
                AuditService.TARGET_BOOKING,
                booking.id(),
                null,
                null,
                "No unique deterministic candidate."
        );
        return new MatchingRunResult(
                booking.id(),
                MatchStatus.REVIEW_REQUIRED,
                RULE_EXACT_BOOKING_AMOUNT,
                null,
                null,
                null,
                economics.currency(),
                null,
                null,
                null,
                "No unique deterministic candidate."
        );
    }

    private MatchingRunResult createAutomaticMatch(ActorContext actor, Booking booking, Candidate candidate) {
        BookingMatch match = bookingMatchRepository.save(new BookingMatch(
                UUID.randomUUID(),
                booking.organisationId(),
                booking.id(),
                MatchType.AUTOMATIC,
                RULE_EXACT_BOOKING_AMOUNT,
                MatchStatus.ACTIVE,
                actor.userId(),
                Instant.now(clock),
                null,
                null
        ));
        matchAllocationRepository.save(new MatchAllocation(
                UUID.randomUUID(),
                booking.organisationId(),
                match.id(),
                candidate.event().id(),
                candidate.amount(),
                candidate.currency(),
                true,
                candidate.exchangeRateId(),
                candidate.event().amount(),
                candidate.event().currency()
        ));
        auditService.recordSuccess(
                actor,
                "MATCH_CREATED",
                AuditService.TARGET_BOOKING,
                booking.id(),
                null,
                "booking_match:" + match.id(),
                RULE_EXACT_BOOKING_AMOUNT
        );
        return new MatchingRunResult(
                booking.id(),
                MatchStatus.ACTIVE,
                RULE_EXACT_BOOKING_AMOUNT,
                match.id(),
                candidate.event().id(),
                candidate.amount(),
                candidate.currency(),
                candidate.exchangeRateId(),
                candidate.event().amount(),
                candidate.event().currency(),
                null
        );
    }

    private MatchingRunResult reviewRequired(ActorContext actor, Booking booking, String reason) {
        BookingMatch match = bookingMatchRepository.save(new BookingMatch(
                UUID.randomUUID(),
                booking.organisationId(),
                booking.id(),
                MatchType.AUTOMATIC,
                RULE_AMBIGUOUS_MATCH,
                MatchStatus.REVIEW_REQUIRED,
                actor.userId(),
                Instant.now(clock),
                null,
                reason
        ));
        auditService.recordSuccess(
                actor,
                "MATCH_REVIEW_REQUIRED",
                AuditService.TARGET_BOOKING,
                booking.id(),
                null,
                "booking_match:" + match.id(),
                reason
        );
        return new MatchingRunResult(
                booking.id(),
                MatchStatus.REVIEW_REQUIRED,
                RULE_AMBIGUOUS_MATCH,
                match.id(),
                null,
                null,
                booking.sellingCurrency(),
                null,
                null,
                null,
                reason
        );
    }

    private Candidate candidate(FinancialEvent event, BigDecimal expectedAmount, String expectedCurrency) {
        if (event.currency().equals(expectedCurrency) && event.amount().compareTo(expectedAmount) == 0) {
            return new Candidate(event, event.amount(), event.currency(), null);
        }

        Optional<ExchangeRateEvidence> evidence = exchangeRateEvidenceRepository
                .findAllByOrganisationIdAndFinancialEventIdOrderByEffectiveAtDescCreatedAtDesc(
                        event.organisationId(),
                        event.id())
                .stream()
                .filter(rate -> rate.targetCurrency().equals(expectedCurrency))
                .filter(rate -> rate.targetAmount().compareTo(expectedAmount) == 0)
                .findFirst();
        return evidence
                .map(rate -> new Candidate(event, rate.targetAmount(), rate.targetCurrency(), rate.id()))
                .orElse(null);
    }

    private boolean compatibleEventType(FinancialEvent event) {
        return event.eventType() == FinancialEventType.CUSTOMER_PAYMENT
                || event.eventType() == FinancialEventType.CHANNEL_SETTLEMENT;
    }

    private boolean withinWindow(Booking booking, FinancialEvent event) {
        Instant start = booking.bookingDate().atStartOfDay(java.time.ZoneOffset.UTC).toInstant()
                .minus(7, ChronoUnit.DAYS);
        Instant end = booking.bookingDate().atStartOfDay(java.time.ZoneOffset.UTC).toInstant()
                .plus(30, ChronoUnit.DAYS);
        return !event.effectiveAt().isBefore(start) && !event.effectiveAt().isAfter(end);
    }

    private record Candidate(FinancialEvent event, BigDecimal amount, String currency, UUID exchangeRateId) {
    }
}

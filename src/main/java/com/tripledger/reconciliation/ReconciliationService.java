package com.tripledger.reconciliation;

import com.tripledger.authorization.AuthorizationService;
import com.tripledger.authorization.Permission;
import com.tripledger.booking.Booking;
import com.tripledger.booking.BookingRepository;
import com.tripledger.common.api.ApiException;
import com.tripledger.economics.BookingEconomicsDetail;
import com.tripledger.economics.BookingEconomicsService;
import com.tripledger.economics.CalculationStatus;
import com.tripledger.identity.ActorContext;
import com.tripledger.matching.BookingMatch;
import com.tripledger.matching.BookingMatchRepository;
import com.tripledger.matching.MatchAllocation;
import com.tripledger.matching.MatchAllocationRepository;
import com.tripledger.matching.MatchStatus;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReconciliationService {

    private static final String RULE_VERSION = "reconciliation-v1";

    private final BookingRepository bookingRepository;
    private final BookingEconomicsService bookingEconomicsService;
    private final BookingMatchRepository bookingMatchRepository;
    private final MatchAllocationRepository matchAllocationRepository;
    private final ReconciliationResultRepository reconciliationResultRepository;
    private final AuthorizationService authorizationService;
    private final Clock clock;

    public ReconciliationService(BookingRepository bookingRepository,
                                 BookingEconomicsService bookingEconomicsService,
                                 BookingMatchRepository bookingMatchRepository,
                                 MatchAllocationRepository matchAllocationRepository,
                                 ReconciliationResultRepository reconciliationResultRepository,
                                 AuthorizationService authorizationService,
                                 Clock clock) {
        this.bookingRepository = bookingRepository;
        this.bookingEconomicsService = bookingEconomicsService;
        this.bookingMatchRepository = bookingMatchRepository;
        this.matchAllocationRepository = matchAllocationRepository;
        this.reconciliationResultRepository = reconciliationResultRepository;
        this.authorizationService = authorizationService;
        this.clock = clock;
    }

    @Transactional
    public ReconciliationResultDetail run(ActorContext actor, UUID bookingId) {
        authorizationService.require(actor, Permission.FINANCIAL_ACTION);
        Booking booking = bookingRepository.findByOrganisationIdAndId(actor.organisationId(), bookingId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "BOOKING_NOT_FOUND",
                        "Booking was not found."
                ));
        BookingEconomicsDetail economics = bookingEconomicsService.calculate(actor, booking.id());

        List<BookingMatch> matches = bookingMatchRepository
                .findAllByOrganisationIdAndBookingIdOrderByCreatedAtDesc(actor.organisationId(), booking.id());
        boolean hasReviewRequired = matches.stream()
                .anyMatch(match -> match.status() == MatchStatus.REVIEW_REQUIRED);
        List<UUID> activeMatchIds = matches.stream()
                .filter(match -> match.status() == MatchStatus.ACTIVE)
                .map(BookingMatch::id)
                .toList();
        BigDecimal matchedAmount = activeMatchIds.isEmpty()
                ? BigDecimal.ZERO.setScale(2)
                : matchAllocationRepository.findAllByOrganisationIdAndMatchIdInAndActiveTrue(
                                actor.organisationId(),
                                activeMatchIds)
                        .stream()
                        .map(MatchAllocation::amount)
                        .reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add);

        ReconciliationStatus status = status(economics, hasReviewRequired, matchedAmount);
        BigDecimal expectedAmount = economics.expectedCustomerReceivable();
        BigDecimal varianceAmount = expectedAmount == null ? null : expectedAmount.subtract(matchedAmount);
        String varianceCurrency = expectedAmount == null ? null : economics.currency();

        Instant now = Instant.now(clock);
        reconciliationResultRepository
                .findFirstByOrganisationIdAndBookingIdAndSupersededAtIsNullOrderByCreatedAtDesc(
                        actor.organisationId(),
                        booking.id())
                .ifPresent(result -> {
                    result.supersede(now);
                    reconciliationResultRepository.save(result);
                });

        ReconciliationResult result = reconciliationResultRepository.save(new ReconciliationResult(
                UUID.randomUUID(),
                booking.organisationId(),
                booking.id(),
                economics.snapshotId(),
                RULE_VERSION,
                status,
                varianceAmount,
                varianceCurrency,
                now,
                null
        ));

        return new ReconciliationResultDetail(
                result.id(),
                booking.id(),
                result.calculationSnapshotId(),
                result.ruleVersion(),
                result.status(),
                expectedAmount,
                matchedAmount,
                result.varianceAmount(),
                economics.currency(),
                result.createdAt()
        );
    }

    private ReconciliationStatus status(BookingEconomicsDetail economics,
                                        boolean hasReviewRequired,
                                        BigDecimal matchedAmount) {
        if (economics.status() != CalculationStatus.READY || economics.expectedCustomerReceivable() == null) {
            return ReconciliationStatus.NOT_READY;
        }
        if (hasReviewRequired) {
            return ReconciliationStatus.DISCREPANT;
        }
        int comparison = matchedAmount.compareTo(economics.expectedCustomerReceivable());
        if (comparison == 0) {
            return ReconciliationStatus.RECONCILED;
        }
        if (matchedAmount.signum() > 0) {
            return ReconciliationStatus.PARTIALLY_RECONCILED;
        }
        return ReconciliationStatus.NOT_READY;
    }
}

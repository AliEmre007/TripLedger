package com.tripledger.discrepancy;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DiscrepancyGenerationService {

    public static final String EXPECTED_RECEIVABLE_COMPONENT = "EXPECTED_CUSTOMER_RECEIVABLE";
    public static final String MATCHING_COMPONENT = "MATCHING";

    private static final BigDecimal DEFAULT_MATERIALITY_THRESHOLD = new BigDecimal("1.00");

    private final DiscrepancyRepository discrepancyRepository;
    private final Clock clock;

    public DiscrepancyGenerationService(DiscrepancyRepository discrepancyRepository, Clock clock) {
        this.discrepancyRepository = discrepancyRepository;
        this.clock = clock;
    }

    @Transactional
    public Discrepancy recordShortSettlement(UUID organisationId,
                                             UUID bookingId,
                                             BigDecimal expectedAmount,
                                             BigDecimal matchedAmount,
                                             String currency) {
        BigDecimal variance = expectedAmount.subtract(matchedAmount);
        if (variance.compareTo(DEFAULT_MATERIALITY_THRESHOLD) < 0) {
            return null;
        }
        String causeIdentity = "expected=" + expectedAmount + ";matched=" + matchedAmount + ";currency=" + currency;
        String explanation = "Expected " + currency + " " + expectedAmount
                + " but matched " + currency + " " + matchedAmount
                + "; variance " + currency + " " + variance + ".";
        return findOrCreate(
                organisationId,
                bookingId,
                DiscrepancyType.SHORT_SETTLEMENT,
                EXPECTED_RECEIVABLE_COMPONENT,
                causeIdentity,
                variance,
                currency,
                explanation
        );
    }

    @Transactional
    public Discrepancy recordAmbiguousMatch(UUID organisationId, UUID bookingId, String reason) {
        return findOrCreate(
                organisationId,
                bookingId,
                DiscrepancyType.AMBIGUOUS_MATCH,
                MATCHING_COMPONENT,
                "AMBIGUOUS_MATCH",
                null,
                null,
                reason
        );
    }

    private Discrepancy findOrCreate(UUID organisationId,
                                     UUID bookingId,
                                     DiscrepancyType type,
                                     String component,
                                     String causeIdentity,
                                     BigDecimal amount,
                                     String currency,
                                     String explanation) {
        return discrepancyRepository
                .findByOrganisationIdAndBookingIdAndTypeAndComponentAndCauseIdentityAndStatus(
                        organisationId,
                        bookingId,
                        type,
                        component,
                        causeIdentity,
                        DiscrepancyStatus.ACTIVE)
                .orElseGet(() -> discrepancyRepository.save(new Discrepancy(
                        UUID.randomUUID(),
                        organisationId,
                        bookingId,
                        type,
                        DiscrepancySeverity.HIGH,
                        component,
                        causeIdentity,
                        amount,
                        currency,
                        DiscrepancyStatus.ACTIVE,
                        null,
                        explanation,
                        Instant.now(clock),
                        null
                )));
    }
}

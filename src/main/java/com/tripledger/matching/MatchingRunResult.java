package com.tripledger.matching;

import java.math.BigDecimal;
import java.util.UUID;

public record MatchingRunResult(
        UUID bookingId,
        MatchStatus status,
        String ruleCode,
        UUID matchId,
        UUID financialEventId,
        BigDecimal amount,
        String currency,
        UUID exchangeRateId,
        BigDecimal originalAmount,
        String originalCurrency,
        String reason
) {
}

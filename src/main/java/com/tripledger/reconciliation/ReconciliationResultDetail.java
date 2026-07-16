package com.tripledger.reconciliation;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ReconciliationResultDetail(
        UUID id,
        UUID bookingId,
        UUID calculationSnapshotId,
        String ruleVersion,
        ReconciliationStatus status,
        BigDecimal expectedAmount,
        BigDecimal matchedAmount,
        BigDecimal varianceAmount,
        String currency,
        Instant createdAt
) {
}

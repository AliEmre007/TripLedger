package com.tripledger.discrepancy;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record DiscrepancySummary(
        UUID id,
        UUID organisationId,
        UUID bookingId,
        DiscrepancyType type,
        DiscrepancySeverity severity,
        String component,
        BigDecimal amount,
        String currency,
        DiscrepancyStatus status,
        UUID ownerUserId,
        String explanation,
        long ageDays,
        Instant createdAt,
        Instant resolvedAt
) {

    static DiscrepancySummary from(Discrepancy discrepancy, long ageDays) {
        return new DiscrepancySummary(
                discrepancy.id(),
                discrepancy.organisationId(),
                discrepancy.bookingId(),
                discrepancy.type(),
                discrepancy.severity(),
                discrepancy.component(),
                discrepancy.amount(),
                discrepancy.currency(),
                discrepancy.status(),
                discrepancy.ownerUserId(),
                discrepancy.explanation(),
                ageDays,
                discrepancy.createdAt(),
                discrepancy.resolvedAt()
        );
    }
}

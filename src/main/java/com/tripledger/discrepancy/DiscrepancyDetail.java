package com.tripledger.discrepancy;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record DiscrepancyDetail(
        UUID id,
        UUID organisationId,
        UUID bookingId,
        DiscrepancyType type,
        DiscrepancySeverity severity,
        String component,
        String causeIdentity,
        BigDecimal amount,
        String currency,
        DiscrepancyStatus status,
        UUID ownerUserId,
        String explanation,
        long ageDays,
        Instant createdAt,
        Instant resolvedAt,
        DiscrepancyBookingEvidence booking
) {

    static DiscrepancyDetail from(Discrepancy discrepancy,
                                  long ageDays,
                                  DiscrepancyBookingEvidence booking) {
        return new DiscrepancyDetail(
                discrepancy.id(),
                discrepancy.organisationId(),
                discrepancy.bookingId(),
                discrepancy.type(),
                discrepancy.severity(),
                discrepancy.component(),
                discrepancy.causeIdentity(),
                discrepancy.amount(),
                discrepancy.currency(),
                discrepancy.status(),
                discrepancy.ownerUserId(),
                discrepancy.explanation(),
                ageDays,
                discrepancy.createdAt(),
                discrepancy.resolvedAt(),
                booking
        );
    }
}

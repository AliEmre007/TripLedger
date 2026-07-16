package com.tripledger.discrepancy;

import java.util.UUID;

record DiscrepancyFilter(
        DiscrepancyStatus status,
        DiscrepancyType type,
        DiscrepancySeverity severity,
        UUID ownerUserId,
        String currency,
        int page,
        int size
) {
}

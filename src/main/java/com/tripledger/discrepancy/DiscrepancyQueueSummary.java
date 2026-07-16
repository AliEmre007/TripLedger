package com.tripledger.discrepancy;

import java.math.BigDecimal;

public record DiscrepancyQueueSummary(
        long totalCount,
        long activeCount,
        long resolvedCount,
        BigDecimal totalAmount
) {
}

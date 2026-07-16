package com.tripledger.discrepancy;

import java.util.List;

public record DiscrepancyListResponse(
        List<DiscrepancySummary> items,
        DiscrepancyQueueSummary summary,
        int page,
        int size,
        long totalItems,
        int totalPages
) {
}

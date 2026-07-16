package com.tripledger.timeline;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TimelineEvent(
        UUID id,
        Instant occurredAt,
        TimelineEventCategory category,
        String eventType,
        String title,
        String summary,
        String targetType,
        UUID targetId,
        UUID actorUserId,
        BigDecimal amount,
        String currency,
        String status,
        String evidenceReference
) {
}

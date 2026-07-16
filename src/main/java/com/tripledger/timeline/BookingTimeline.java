package com.tripledger.timeline;

import java.util.List;
import java.util.UUID;

public record BookingTimeline(
        UUID bookingId,
        UUID organisationId,
        List<TimelineEvent> events
) {
}

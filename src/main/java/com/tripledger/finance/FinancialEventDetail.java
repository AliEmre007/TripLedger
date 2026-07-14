package com.tripledger.finance;

import com.tripledger.booking.SourceRecordDetail;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record FinancialEventDetail(
        UUID id,
        UUID organisationId,
        UUID bookingId,
        FinancialEventType eventType,
        FinancialEventDirection direction,
        BigDecimal amount,
        String currency,
        Instant effectiveAt,
        String externalReference,
        UUID reversesEventId,
        String adjustmentReason,
        boolean matchedToBooking,
        SourceRecordDetail sourceRecord,
        UUID createdByUserId,
        Instant createdAt
) {

    static FinancialEventDetail from(FinancialEvent event, SourceRecordDetail sourceRecord) {
        return new FinancialEventDetail(
                event.id(),
                event.organisationId(),
                event.bookingId(),
                event.eventType(),
                event.direction(),
                event.amount(),
                event.currency(),
                event.effectiveAt(),
                event.externalReference(),
                event.reversesEventId(),
                event.adjustmentReason(),
                event.matchedToBooking(),
                sourceRecord,
                event.createdByUserId(),
                event.createdAt()
        );
    }
}

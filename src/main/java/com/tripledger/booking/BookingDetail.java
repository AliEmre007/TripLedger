package com.tripledger.booking;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record BookingDetail(
        UUID id,
        UUID organisationId,
        UUID sourceSystemId,
        String externalBookingId,
        LocalDate bookingDate,
        LocalDate serviceStartDate,
        LocalDate serviceEndDate,
        BookingLifecycleStatus lifecycleStatus,
        BigDecimal contractedSellingAmount,
        String sellingCurrency,
        String customerReference,
        SourceRecordDetail currentSourceRecord,
        List<BookingItemDetail> items,
        Instant createdAt,
        Instant updatedAt
) {

    static BookingDetail from(Booking booking,
                              SourceRecordDetail currentSourceRecord,
                              List<BookingItemDetail> items) {
        return new BookingDetail(
                booking.id(),
                booking.organisationId(),
                booking.sourceSystemId(),
                booking.externalBookingId(),
                booking.bookingDate(),
                booking.serviceStartDate(),
                booking.serviceEndDate(),
                booking.lifecycleStatus(),
                booking.contractedSellingAmount(),
                booking.sellingCurrency(),
                booking.customerReference(),
                currentSourceRecord,
                List.copyOf(items),
                booking.createdAt(),
                booking.updatedAt()
        );
    }
}

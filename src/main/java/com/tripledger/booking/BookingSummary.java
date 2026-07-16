package com.tripledger.booking;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record BookingSummary(
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
        String customerReference
) {

    static BookingSummary from(Booking booking) {
        return new BookingSummary(
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
                booking.customerReference()
        );
    }
}

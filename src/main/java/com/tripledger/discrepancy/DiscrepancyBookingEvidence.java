package com.tripledger.discrepancy;

import com.tripledger.booking.Booking;
import com.tripledger.booking.BookingLifecycleStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record DiscrepancyBookingEvidence(
        UUID id,
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

    static DiscrepancyBookingEvidence from(Booking booking) {
        if (booking == null) {
            return null;
        }

        return new DiscrepancyBookingEvidence(
                booking.id(),
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

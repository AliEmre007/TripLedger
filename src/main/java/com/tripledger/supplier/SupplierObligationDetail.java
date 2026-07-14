package com.tripledger.supplier;

import com.tripledger.booking.SourceRecordDetail;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record SupplierObligationDetail(
        UUID id,
        UUID organisationId,
        UUID bookingId,
        UUID bookingItemId,
        UUID supplierId,
        String supplierReference,
        String supplierName,
        BigDecimal amount,
        String currency,
        LocalDate dueDate,
        SupplierObligationStatus status,
        boolean linkedToBookingEconomics,
        boolean contributesToActiveSupplierCost,
        SourceRecordDetail sourceRecord,
        Instant createdAt
) {

    static SupplierObligationDetail from(SupplierObligation obligation,
                                         Supplier supplier,
                                         SourceRecordDetail sourceRecord) {
        return new SupplierObligationDetail(
                obligation.id(),
                obligation.organisationId(),
                obligation.bookingId(),
                obligation.bookingItemId(),
                obligation.supplierId(),
                supplier == null ? null : supplier.externalReference(),
                supplier == null ? null : supplier.name(),
                obligation.amount(),
                obligation.currency(),
                obligation.dueDate(),
                obligation.status(),
                obligation.linkedToBookingEconomics(),
                obligation.contributesToActiveSupplierCost(),
                sourceRecord,
                obligation.createdAt()
        );
    }
}

package com.tripledger.economics;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record BookingEconomicsDetail(
        UUID snapshotId,
        UUID bookingId,
        String ruleVersion,
        String currency,
        BigDecimal contractedGrossSale,
        BigDecimal expectedCustomerReceivable,
        BigDecimal expectedDeductions,
        BigDecimal activeSupplierCost,
        BigDecimal estimatedGrossMargin,
        CalculationStatus status,
        List<String> unknownComponents,
        Instant createdAt
) {

    static BookingEconomicsDetail from(CalculationSnapshot snapshot, List<String> unknownComponents) {
        return new BookingEconomicsDetail(
                snapshot.id(),
                snapshot.bookingId(),
                snapshot.ruleVersion(),
                snapshot.currency(),
                snapshot.contractedGrossSale(),
                snapshot.expectedCustomerReceivable(),
                snapshot.expectedDeductions(),
                snapshot.activeSupplierCost(),
                snapshot.estimatedGrossMargin(),
                snapshot.status(),
                unknownComponents,
                snapshot.createdAt()
        );
    }
}

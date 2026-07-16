package com.tripledger.economics;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record BookingEconomicsExplanationDetail(
        UUID bookingId,
        String ruleVersion,
        String currency,
        List<FormulaDetail> formulas,
        List<ComponentDetail> components,
        List<ExchangeRateDetail> exchangeRates,
        String rounding
) {

    public record FormulaDetail(
            String subtotal,
            String formula,
            String ruleReference
    ) {
    }

    public record ComponentDetail(
            String componentType,
            String subtotal,
            String sourceTable,
            UUID sourceId,
            UUID sourceRecordId,
            BigDecimal originalAmount,
            String originalCurrency,
            BigDecimal amount,
            String currency,
            UUID exchangeRateId,
            String formulaReference
    ) {
    }

    public record ExchangeRateDetail(
            UUID id,
            UUID financialEventId,
            BigDecimal sourceAmount,
            String sourceCurrency,
            BigDecimal targetAmount,
            String targetCurrency,
            BigDecimal rate,
            Instant effectiveAt,
            String rateSource,
            String roundingPolicyVersion
    ) {
    }
}

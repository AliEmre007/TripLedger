package com.tripledger.finance;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ExchangeRateEvidenceDetail(
        UUID id,
        UUID organisationId,
        UUID financialEventId,
        BigDecimal sourceAmount,
        String sourceCurrency,
        BigDecimal targetAmount,
        String targetCurrency,
        BigDecimal rate,
        Instant effectiveAt,
        String rateSource,
        String roundingPolicyVersion,
        UUID createdByUserId,
        Instant createdAt
) {

    static ExchangeRateEvidenceDetail from(ExchangeRateEvidence evidence) {
        return new ExchangeRateEvidenceDetail(
                evidence.id(),
                evidence.organisationId(),
                evidence.financialEventId(),
                evidence.sourceAmount(),
                evidence.sourceCurrency(),
                evidence.targetAmount(),
                evidence.targetCurrency(),
                evidence.rate(),
                evidence.effectiveAt(),
                evidence.rateSource(),
                evidence.roundingPolicyVersion(),
                evidence.createdByUserId(),
                evidence.createdAt()
        );
    }
}

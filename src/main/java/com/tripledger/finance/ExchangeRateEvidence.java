package com.tripledger.finance;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "exchange_rate")
public class ExchangeRateEvidence {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID organisationId;

    private UUID financialEventId;

    @Column(nullable = false)
    private BigDecimal sourceAmount;

    @Column(nullable = false)
    @JdbcTypeCode(SqlTypes.CHAR)
    private String sourceCurrency;

    @Column(nullable = false)
    private BigDecimal targetAmount;

    @Column(nullable = false)
    @JdbcTypeCode(SqlTypes.CHAR)
    private String targetCurrency;

    @Column(nullable = false)
    private BigDecimal rate;

    @Column(nullable = false)
    private Instant effectiveAt;

    @Column(nullable = false)
    private String rateSource;

    @Column(nullable = false)
    private String roundingPolicyVersion;

    private UUID createdByUserId;

    @Column(nullable = false)
    private Instant createdAt;

    protected ExchangeRateEvidence() {
    }

    public ExchangeRateEvidence(UUID id,
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
                                Instant createdAt) {
        this.id = id;
        this.organisationId = organisationId;
        this.financialEventId = financialEventId;
        this.sourceAmount = sourceAmount;
        this.sourceCurrency = sourceCurrency;
        this.targetAmount = targetAmount;
        this.targetCurrency = targetCurrency;
        this.rate = rate;
        this.effectiveAt = effectiveAt;
        this.rateSource = rateSource;
        this.roundingPolicyVersion = roundingPolicyVersion;
        this.createdByUserId = createdByUserId;
        this.createdAt = createdAt;
    }

    public UUID id() {
        return id;
    }

    public UUID organisationId() {
        return organisationId;
    }

    public UUID financialEventId() {
        return financialEventId;
    }

    public BigDecimal sourceAmount() {
        return sourceAmount;
    }

    public String sourceCurrency() {
        return sourceCurrency;
    }

    public BigDecimal targetAmount() {
        return targetAmount;
    }

    public String targetCurrency() {
        return targetCurrency;
    }

    public BigDecimal rate() {
        return rate;
    }

    public Instant effectiveAt() {
        return effectiveAt;
    }

    public String rateSource() {
        return rateSource;
    }

    public String roundingPolicyVersion() {
        return roundingPolicyVersion;
    }

    public UUID createdByUserId() {
        return createdByUserId;
    }

    public Instant createdAt() {
        return createdAt;
    }
}

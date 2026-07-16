package com.tripledger.economics;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "calculation_snapshot")
public class CalculationSnapshot {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID organisationId;

    @Column(nullable = false)
    private UUID bookingId;

    @Column(nullable = false)
    private String ruleVersion;

    @Column(nullable = false)
    private BigDecimal contractedGrossSale;

    private BigDecimal expectedCustomerReceivable;

    private BigDecimal expectedDeductions;

    private BigDecimal activeSupplierCost;

    private BigDecimal estimatedGrossMargin;

    @Column(nullable = false)
    @JdbcTypeCode(SqlTypes.CHAR)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CalculationStatus status;

    @Column(nullable = false)
    private String unknownComponents;

    @Column(nullable = false)
    private Instant createdAt;

    protected CalculationSnapshot() {
    }

    public CalculationSnapshot(UUID id,
                               UUID organisationId,
                               UUID bookingId,
                               String ruleVersion,
                               BigDecimal contractedGrossSale,
                               BigDecimal expectedCustomerReceivable,
                               BigDecimal expectedDeductions,
                               BigDecimal activeSupplierCost,
                               BigDecimal estimatedGrossMargin,
                               String currency,
                               CalculationStatus status,
                               String unknownComponents,
                               Instant createdAt) {
        this.id = id;
        this.organisationId = organisationId;
        this.bookingId = bookingId;
        this.ruleVersion = ruleVersion;
        this.contractedGrossSale = contractedGrossSale;
        this.expectedCustomerReceivable = expectedCustomerReceivable;
        this.expectedDeductions = expectedDeductions;
        this.activeSupplierCost = activeSupplierCost;
        this.estimatedGrossMargin = estimatedGrossMargin;
        this.currency = currency;
        this.status = status;
        this.unknownComponents = unknownComponents;
        this.createdAt = createdAt;
    }

    public UUID id() {
        return id;
    }

    public UUID organisationId() {
        return organisationId;
    }

    public UUID bookingId() {
        return bookingId;
    }

    public String ruleVersion() {
        return ruleVersion;
    }

    public BigDecimal contractedGrossSale() {
        return contractedGrossSale;
    }

    public BigDecimal expectedCustomerReceivable() {
        return expectedCustomerReceivable;
    }

    public BigDecimal expectedDeductions() {
        return expectedDeductions;
    }

    public BigDecimal activeSupplierCost() {
        return activeSupplierCost;
    }

    public BigDecimal estimatedGrossMargin() {
        return estimatedGrossMargin;
    }

    public String currency() {
        return currency;
    }

    public CalculationStatus status() {
        return status;
    }

    public String unknownComponents() {
        return unknownComponents;
    }

    public Instant createdAt() {
        return createdAt;
    }
}

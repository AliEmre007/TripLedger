package com.tripledger.reconciliation;

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
@Table(name = "reconciliation_result")
public class ReconciliationResult {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID organisationId;

    @Column(nullable = false)
    private UUID bookingId;

    @Column(nullable = false)
    private UUID calculationSnapshotId;

    @Column(nullable = false)
    private String ruleVersion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReconciliationStatus status;

    private BigDecimal varianceAmount;

    @JdbcTypeCode(SqlTypes.CHAR)
    private String varianceCurrency;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant supersededAt;

    protected ReconciliationResult() {
    }

    public ReconciliationResult(UUID id,
                                UUID organisationId,
                                UUID bookingId,
                                UUID calculationSnapshotId,
                                String ruleVersion,
                                ReconciliationStatus status,
                                BigDecimal varianceAmount,
                                String varianceCurrency,
                                Instant createdAt,
                                Instant supersededAt) {
        this.id = id;
        this.organisationId = organisationId;
        this.bookingId = bookingId;
        this.calculationSnapshotId = calculationSnapshotId;
        this.ruleVersion = ruleVersion;
        this.status = status;
        this.varianceAmount = varianceAmount;
        this.varianceCurrency = varianceCurrency;
        this.createdAt = createdAt;
        this.supersededAt = supersededAt;
    }

    public void supersede(Instant when) {
        this.supersededAt = when;
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

    public UUID calculationSnapshotId() {
        return calculationSnapshotId;
    }

    public String ruleVersion() {
        return ruleVersion;
    }

    public ReconciliationStatus status() {
        return status;
    }

    public BigDecimal varianceAmount() {
        return varianceAmount;
    }

    public String varianceCurrency() {
        return varianceCurrency;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant supersededAt() {
        return supersededAt;
    }
}

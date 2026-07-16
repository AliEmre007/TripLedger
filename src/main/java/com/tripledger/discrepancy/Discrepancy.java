package com.tripledger.discrepancy;

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
@Table(name = "discrepancy")
public class Discrepancy {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID organisationId;

    private UUID bookingId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DiscrepancyType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DiscrepancySeverity severity;

    private String component;

    @Column(nullable = false)
    private String causeIdentity;

    private BigDecimal amount;

    @JdbcTypeCode(SqlTypes.CHAR)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DiscrepancyStatus status;

    private UUID ownerUserId;

    @Column(nullable = false)
    private String explanation;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant resolvedAt;

    protected Discrepancy() {
    }

    public Discrepancy(UUID id,
                       UUID organisationId,
                       UUID bookingId,
                       DiscrepancyType type,
                       DiscrepancySeverity severity,
                       String component,
                       String causeIdentity,
                       BigDecimal amount,
                       String currency,
                       DiscrepancyStatus status,
                       UUID ownerUserId,
                       String explanation,
                       Instant createdAt,
                       Instant resolvedAt) {
        this.id = id;
        this.organisationId = organisationId;
        this.bookingId = bookingId;
        this.type = type;
        this.severity = severity;
        this.component = component;
        this.causeIdentity = causeIdentity;
        this.amount = amount;
        this.currency = currency;
        this.status = status;
        this.ownerUserId = ownerUserId;
        this.explanation = explanation;
        this.createdAt = createdAt;
        this.resolvedAt = resolvedAt;
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

    public DiscrepancyType type() {
        return type;
    }

    public DiscrepancySeverity severity() {
        return severity;
    }

    public String component() {
        return component;
    }

    public String causeIdentity() {
        return causeIdentity;
    }

    public BigDecimal amount() {
        return amount;
    }

    public String currency() {
        return currency;
    }

    public DiscrepancyStatus status() {
        return status;
    }

    public UUID ownerUserId() {
        return ownerUserId;
    }

    public String explanation() {
        return explanation;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant resolvedAt() {
        return resolvedAt;
    }
}

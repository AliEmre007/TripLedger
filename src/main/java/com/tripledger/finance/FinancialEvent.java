package com.tripledger.finance;

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
@Table(name = "financial_event")
public class FinancialEvent {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID organisationId;

    private UUID sourceRecordId;

    private UUID bookingId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FinancialEventType eventType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FinancialEventDirection direction;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    @JdbcTypeCode(SqlTypes.CHAR)
    private String currency;

    @Column(nullable = false)
    private Instant effectiveAt;

    private String externalReference;

    private UUID reversesEventId;

    private String adjustmentReason;

    private UUID createdByUserId;

    @Column(nullable = false)
    private Instant createdAt;

    protected FinancialEvent() {
    }

    public FinancialEvent(UUID id,
                          UUID organisationId,
                          UUID sourceRecordId,
                          UUID bookingId,
                          FinancialEventType eventType,
                          FinancialEventDirection direction,
                          BigDecimal amount,
                          String currency,
                          Instant effectiveAt,
                          String externalReference,
                          UUID reversesEventId,
                          String adjustmentReason,
                          UUID createdByUserId,
                          Instant createdAt) {
        this.id = id;
        this.organisationId = organisationId;
        this.sourceRecordId = sourceRecordId;
        this.bookingId = bookingId;
        this.eventType = eventType;
        this.direction = direction;
        this.amount = amount;
        this.currency = currency;
        this.effectiveAt = effectiveAt;
        this.externalReference = externalReference;
        this.reversesEventId = reversesEventId;
        this.adjustmentReason = adjustmentReason;
        this.createdByUserId = createdByUserId;
        this.createdAt = createdAt;
    }

    public boolean matchedToBooking() {
        return bookingId != null;
    }

    public UUID id() {
        return id;
    }

    public UUID organisationId() {
        return organisationId;
    }

    public UUID sourceRecordId() {
        return sourceRecordId;
    }

    public UUID bookingId() {
        return bookingId;
    }

    public FinancialEventType eventType() {
        return eventType;
    }

    public FinancialEventDirection direction() {
        return direction;
    }

    public BigDecimal amount() {
        return amount;
    }

    public String currency() {
        return currency;
    }

    public Instant effectiveAt() {
        return effectiveAt;
    }

    public String externalReference() {
        return externalReference;
    }

    public UUID reversesEventId() {
        return reversesEventId;
    }

    public String adjustmentReason() {
        return adjustmentReason;
    }

    public UUID createdByUserId() {
        return createdByUserId;
    }

    public Instant createdAt() {
        return createdAt;
    }
}

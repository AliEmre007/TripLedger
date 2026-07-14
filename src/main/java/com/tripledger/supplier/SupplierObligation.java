package com.tripledger.supplier;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "supplier_obligation")
public class SupplierObligation {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID organisationId;

    private UUID bookingId;

    private UUID bookingItemId;

    private UUID supplierId;

    private UUID sourceRecordId;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    @JdbcTypeCode(SqlTypes.CHAR)
    private String currency;

    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SupplierObligationStatus status;

    @Column(nullable = false)
    private Instant createdAt;

    protected SupplierObligation() {
    }

    public SupplierObligation(UUID id,
                              UUID organisationId,
                              UUID bookingId,
                              UUID bookingItemId,
                              UUID supplierId,
                              UUID sourceRecordId,
                              BigDecimal amount,
                              String currency,
                              LocalDate dueDate,
                              SupplierObligationStatus status,
                              Instant createdAt) {
        this.id = id;
        this.organisationId = organisationId;
        this.bookingId = bookingId;
        this.bookingItemId = bookingItemId;
        this.supplierId = supplierId;
        this.sourceRecordId = sourceRecordId;
        this.amount = amount;
        this.currency = currency;
        this.dueDate = dueDate;
        this.status = status;
        this.createdAt = createdAt;
    }

    public boolean linkedToBookingEconomics() {
        return bookingId != null || bookingItemId != null;
    }

    public boolean contributesToActiveSupplierCost() {
        return linkedToBookingEconomics() && status != SupplierObligationStatus.CANCELLED;
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

    public UUID bookingItemId() {
        return bookingItemId;
    }

    public UUID supplierId() {
        return supplierId;
    }

    public UUID sourceRecordId() {
        return sourceRecordId;
    }

    public BigDecimal amount() {
        return amount;
    }

    public String currency() {
        return currency;
    }

    public LocalDate dueDate() {
        return dueDate;
    }

    public SupplierObligationStatus status() {
        return status;
    }

    public Instant createdAt() {
        return createdAt;
    }
}

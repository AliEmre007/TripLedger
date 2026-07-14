package com.tripledger.booking;

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
@Table(name = "booking")
public class Booking {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID organisationId;

    @Column(nullable = false)
    private UUID sourceSystemId;

    @Column(nullable = false)
    private String externalBookingId;

    private UUID currentSourceRecordId;

    @Column(nullable = false)
    private LocalDate bookingDate;

    private LocalDate serviceStartDate;

    private LocalDate serviceEndDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingLifecycleStatus lifecycleStatus;

    @Column(nullable = false)
    @JdbcTypeCode(SqlTypes.CHAR)
    private String sellingCurrency;

    @Column(nullable = false)
    private BigDecimal contractedSellingAmount;

    private String customerReference;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected Booking() {
    }

    public Booking(UUID id,
                   UUID organisationId,
                   UUID sourceSystemId,
                   String externalBookingId,
                   UUID currentSourceRecordId,
                   LocalDate bookingDate,
                   LocalDate serviceStartDate,
                   LocalDate serviceEndDate,
                   BookingLifecycleStatus lifecycleStatus,
                   String sellingCurrency,
                   BigDecimal contractedSellingAmount,
                   String customerReference,
                   Instant createdAt,
                   Instant updatedAt) {
        this.id = id;
        this.organisationId = organisationId;
        this.sourceSystemId = sourceSystemId;
        this.externalBookingId = externalBookingId;
        this.currentSourceRecordId = currentSourceRecordId;
        this.bookingDate = bookingDate;
        this.serviceStartDate = serviceStartDate;
        this.serviceEndDate = serviceEndDate;
        this.lifecycleStatus = lifecycleStatus;
        this.sellingCurrency = sellingCurrency;
        this.contractedSellingAmount = contractedSellingAmount;
        this.customerReference = customerReference;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public void applySource(UUID sourceRecordId,
                            LocalDate bookingDate,
                            LocalDate serviceStartDate,
                            LocalDate serviceEndDate,
                            BookingLifecycleStatus lifecycleStatus,
                            String sellingCurrency,
                            BigDecimal contractedSellingAmount,
                            String customerReference,
                            Instant updatedAt) {
        this.currentSourceRecordId = sourceRecordId;
        this.bookingDate = bookingDate;
        this.serviceStartDate = serviceStartDate;
        this.serviceEndDate = serviceEndDate;
        this.lifecycleStatus = lifecycleStatus;
        this.sellingCurrency = sellingCurrency;
        this.contractedSellingAmount = contractedSellingAmount;
        this.customerReference = customerReference;
        this.updatedAt = updatedAt;
    }

    public UUID id() {
        return id;
    }

    public UUID organisationId() {
        return organisationId;
    }

    public UUID sourceSystemId() {
        return sourceSystemId;
    }

    public String externalBookingId() {
        return externalBookingId;
    }

    public UUID currentSourceRecordId() {
        return currentSourceRecordId;
    }

    public LocalDate bookingDate() {
        return bookingDate;
    }

    public LocalDate serviceStartDate() {
        return serviceStartDate;
    }

    public LocalDate serviceEndDate() {
        return serviceEndDate;
    }

    public BookingLifecycleStatus lifecycleStatus() {
        return lifecycleStatus;
    }

    public String sellingCurrency() {
        return sellingCurrency;
    }

    public BigDecimal contractedSellingAmount() {
        return contractedSellingAmount;
    }
}

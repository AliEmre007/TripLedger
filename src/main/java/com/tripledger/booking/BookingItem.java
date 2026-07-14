package com.tripledger.booking;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "booking_item")
public class BookingItem {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID organisationId;

    @Column(nullable = false)
    private UUID bookingId;

    private UUID sourceRecordId;

    @Column(nullable = false)
    private String itemExternalId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingItemServiceType serviceType;

    @Column(nullable = false)
    private LocalDate serviceStartDate;

    @Column(nullable = false)
    private LocalDate serviceEndDate;

    @Column(nullable = false)
    private BigDecimal sellingAmount;

    @Column(nullable = false)
    @JdbcTypeCode(SqlTypes.CHAR)
    private String sellingCurrency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingItemState state;

    protected BookingItem() {
    }

    public BookingItem(UUID id,
                       UUID organisationId,
                       UUID bookingId,
                       UUID sourceRecordId,
                       String itemExternalId,
                       BookingItemServiceType serviceType,
                       LocalDate serviceStartDate,
                       LocalDate serviceEndDate,
                       BigDecimal sellingAmount,
                       String sellingCurrency,
                       BookingItemState state) {
        this.id = id;
        this.organisationId = organisationId;
        this.bookingId = bookingId;
        this.sourceRecordId = sourceRecordId;
        this.itemExternalId = itemExternalId;
        this.serviceType = serviceType;
        this.serviceStartDate = serviceStartDate;
        this.serviceEndDate = serviceEndDate;
        this.sellingAmount = sellingAmount;
        this.sellingCurrency = sellingCurrency;
        this.state = state;
    }

    public void applySource(UUID sourceRecordId,
                            BookingItemServiceType serviceType,
                            LocalDate serviceStartDate,
                            LocalDate serviceEndDate,
                            BigDecimal sellingAmount,
                            String sellingCurrency) {
        this.sourceRecordId = sourceRecordId;
        this.serviceType = serviceType;
        this.serviceStartDate = serviceStartDate;
        this.serviceEndDate = serviceEndDate;
        this.sellingAmount = sellingAmount;
        this.sellingCurrency = sellingCurrency;
        this.state = BookingItemState.ACTIVE;
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

    public String itemExternalId() {
        return itemExternalId;
    }

    public UUID sourceRecordId() {
        return sourceRecordId;
    }

    public BookingItemServiceType serviceType() {
        return serviceType;
    }

    public LocalDate serviceStartDate() {
        return serviceStartDate;
    }

    public LocalDate serviceEndDate() {
        return serviceEndDate;
    }

    public BigDecimal sellingAmount() {
        return sellingAmount;
    }

    public String sellingCurrency() {
        return sellingCurrency;
    }

    public BookingItemState state() {
        return state;
    }
}

package com.tripledger.matching;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "booking_match")
public class BookingMatch {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID organisationId;

    @Column(nullable = false)
    private UUID bookingId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MatchType matchType;

    @Column(nullable = false)
    private String ruleCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MatchStatus status;

    private UUID createdByUserId;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant removedAt;

    private String reason;

    protected BookingMatch() {
    }

    public BookingMatch(UUID id,
                        UUID organisationId,
                        UUID bookingId,
                        MatchType matchType,
                        String ruleCode,
                        MatchStatus status,
                        UUID createdByUserId,
                        Instant createdAt,
                        Instant removedAt,
                        String reason) {
        this.id = id;
        this.organisationId = organisationId;
        this.bookingId = bookingId;
        this.matchType = matchType;
        this.ruleCode = ruleCode;
        this.status = status;
        this.createdByUserId = createdByUserId;
        this.createdAt = createdAt;
        this.removedAt = removedAt;
        this.reason = reason;
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

    public MatchType matchType() {
        return matchType;
    }

    public String ruleCode() {
        return ruleCode;
    }

    public MatchStatus status() {
        return status;
    }

    public UUID createdByUserId() {
        return createdByUserId;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant removedAt() {
        return removedAt;
    }

    public String reason() {
        return reason;
    }
}

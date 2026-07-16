package com.tripledger.matching;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "match_allocation")
public class MatchAllocation {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID organisationId;

    @Column(nullable = false)
    private UUID matchId;

    @Column(nullable = false)
    private UUID financialEventId;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    @JdbcTypeCode(SqlTypes.CHAR)
    private String currency;

    @Column(nullable = false)
    private boolean active;

    private UUID exchangeRateId;

    private BigDecimal originalAmount;

    @JdbcTypeCode(SqlTypes.CHAR)
    private String originalCurrency;

    protected MatchAllocation() {
    }

    public MatchAllocation(UUID id,
                           UUID organisationId,
                           UUID matchId,
                           UUID financialEventId,
                           BigDecimal amount,
                           String currency,
                           boolean active,
                           UUID exchangeRateId,
                           BigDecimal originalAmount,
                           String originalCurrency) {
        this.id = id;
        this.organisationId = organisationId;
        this.matchId = matchId;
        this.financialEventId = financialEventId;
        this.amount = amount;
        this.currency = currency;
        this.active = active;
        this.exchangeRateId = exchangeRateId;
        this.originalAmount = originalAmount;
        this.originalCurrency = originalCurrency;
    }

    public UUID id() {
        return id;
    }

    public UUID organisationId() {
        return organisationId;
    }

    public UUID matchId() {
        return matchId;
    }

    public UUID financialEventId() {
        return financialEventId;
    }

    public BigDecimal amount() {
        return amount;
    }

    public String currency() {
        return currency;
    }

    public boolean active() {
        return active;
    }

    public UUID exchangeRateId() {
        return exchangeRateId;
    }

    public BigDecimal originalAmount() {
        return originalAmount;
    }

    public String originalCurrency() {
        return originalCurrency;
    }
}

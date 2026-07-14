package com.tripledger.identity;

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
@Table(name = "organisation")
public class Organisation {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, columnDefinition = "char(3)")
    @JdbcTypeCode(SqlTypes.CHAR)
    private String baseCurrency;

    @Column(nullable = false)
    private BigDecimal materialityThreshold;

    @Column(nullable = false)
    private BigDecimal defaultAmountTolerance;

    @Column(nullable = false)
    private int defaultDateWindowBeforeDays;

    @Column(nullable = false)
    private int defaultDateWindowAfterDays;

    @Column(nullable = false)
    private String roundingPolicyVersion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrganisationStatus status;

    @Column(nullable = false)
    private Instant createdAt;

    protected Organisation() {
    }
}

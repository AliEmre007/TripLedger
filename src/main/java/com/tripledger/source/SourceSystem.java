package com.tripledger.source;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "source_system")
public class SourceSystem {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID organisationId;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SourceSystemCategory category;

    @Column(nullable = false)
    private String externalCode;

    @Column(nullable = false)
    private String timeZone;

    @Column(nullable = false)
    private boolean active;

    @Column(nullable = false)
    private Instant createdAt;

    protected SourceSystem() {
    }

    public SourceSystem(UUID id,
                        UUID organisationId,
                        String name,
                        SourceSystemCategory category,
                        String externalCode,
                        String timeZone,
                        boolean active,
                        Instant createdAt) {
        this.id = id;
        this.organisationId = organisationId;
        this.name = name;
        this.category = category;
        this.externalCode = externalCode;
        this.timeZone = timeZone;
        this.active = active;
        this.createdAt = createdAt;
    }

    public UUID id() {
        return id;
    }

    public UUID organisationId() {
        return organisationId;
    }

    public String name() {
        return name;
    }

    public SourceSystemCategory category() {
        return category;
    }

    public String externalCode() {
        return externalCode;
    }

    public String timeZone() {
        return timeZone;
    }

    public boolean active() {
        return active;
    }

    public Instant createdAt() {
        return createdAt;
    }
}

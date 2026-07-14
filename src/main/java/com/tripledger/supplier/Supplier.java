package com.tripledger.supplier;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "supplier")
public class Supplier {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID organisationId;

    @Column(nullable = false)
    private String name;

    private String externalReference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SupplierStatus status;

    @Column(nullable = false)
    private Instant createdAt;

    protected Supplier() {
    }

    public Supplier(UUID id,
                    UUID organisationId,
                    String name,
                    String externalReference,
                    SupplierStatus status,
                    Instant createdAt) {
        this.id = id;
        this.organisationId = organisationId;
        this.name = name;
        this.externalReference = externalReference;
        this.status = status;
        this.createdAt = createdAt;
    }

    public void rename(String name) {
        this.name = name;
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

    public String externalReference() {
        return externalReference;
    }

    public SupplierStatus status() {
        return status;
    }

    public Instant createdAt() {
        return createdAt;
    }
}

package com.tripledger.identity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "app_user")
public class AppUser {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID organisationId;

    @Column(nullable = false)
    private String identitySubject;

    @Column(nullable = false)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant deactivatedAt;

    protected AppUser() {
    }

    public AppUser(UUID id,
                   UUID organisationId,
                   String identitySubject,
                   String displayName,
                   UserRole role,
                   UserStatus status,
                   Instant createdAt,
                   Instant deactivatedAt) {
        this.id = id;
        this.organisationId = organisationId;
        this.identitySubject = identitySubject;
        this.displayName = displayName;
        this.role = role;
        this.status = status;
        this.createdAt = createdAt;
        this.deactivatedAt = deactivatedAt;
    }

    public UUID id() {
        return id;
    }

    public UUID organisationId() {
        return organisationId;
    }

    public String identitySubject() {
        return identitySubject;
    }

    public String displayName() {
        return displayName;
    }

    public UserRole role() {
        return role;
    }

    public UserStatus status() {
        return status;
    }
}

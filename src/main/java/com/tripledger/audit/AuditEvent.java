package com.tripledger.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_event")
public class AuditEvent {

    @Id
    private UUID id;

    private UUID organisationId;

    private UUID actorUserId;

    private String systemActor;

    @Column(nullable = false)
    private String action;

    @Column(nullable = false)
    private String targetType;

    private UUID targetId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuditOutcome outcome;

    private String beforeReference;

    private String afterReference;

    private String reason;

    @Column(nullable = false)
    private String correlationId;

    @Column(nullable = false)
    private Instant createdAt;

    protected AuditEvent() {
    }

    public AuditEvent(UUID id,
                      UUID organisationId,
                      UUID actorUserId,
                      String systemActor,
                      String action,
                      String targetType,
                      UUID targetId,
                      AuditOutcome outcome,
                      String beforeReference,
                      String afterReference,
                      String reason,
                      String correlationId,
                      Instant createdAt) {
        this.id = id;
        this.organisationId = organisationId;
        this.actorUserId = actorUserId;
        this.systemActor = systemActor;
        this.action = action;
        this.targetType = targetType;
        this.targetId = targetId;
        this.outcome = outcome;
        this.beforeReference = beforeReference;
        this.afterReference = afterReference;
        this.reason = reason;
        this.correlationId = correlationId;
        this.createdAt = createdAt;
    }

    public UUID id() {
        return id;
    }

    public UUID organisationId() {
        return organisationId;
    }

    public UUID actorUserId() {
        return actorUserId;
    }

    public String systemActor() {
        return systemActor;
    }

    public String action() {
        return action;
    }

    public String targetType() {
        return targetType;
    }

    public UUID targetId() {
        return targetId;
    }

    public AuditOutcome outcome() {
        return outcome;
    }

    public String beforeReference() {
        return beforeReference;
    }

    public String afterReference() {
        return afterReference;
    }

    public String reason() {
        return reason;
    }

    public String correlationId() {
        return correlationId;
    }

    public Instant createdAt() {
        return createdAt;
    }
}

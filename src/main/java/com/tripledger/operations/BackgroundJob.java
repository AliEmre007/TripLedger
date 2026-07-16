package com.tripledger.operations;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "background_job")
public class BackgroundJob {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID organisationId;

    @Column(nullable = false)
    private String jobType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BackgroundJobStatus status;

    private String targetType;

    private UUID targetId;

    @Column(nullable = false)
    private String idempotencyKey;

    @Column(nullable = false)
    private UUID requestedByUserId;

    @Column(nullable = false)
    private int maxAttempts;

    @Column(nullable = false)
    private int attemptCount;

    private String diagnosticCategory;

    private String diagnosticMessage;

    @Column(nullable = false)
    private String correlationId;

    @Column(nullable = false)
    private Instant requestedAt;

    private Instant lastAttemptAt;

    private Instant nextAttemptAt;

    private Instant completedAt;

    protected BackgroundJob() {
    }

    public BackgroundJob(UUID id,
                         UUID organisationId,
                         String jobType,
                         String targetType,
                         UUID targetId,
                         String idempotencyKey,
                         UUID requestedByUserId,
                         int maxAttempts,
                         String correlationId,
                         Instant requestedAt) {
        this.id = id;
        this.organisationId = organisationId;
        this.jobType = jobType;
        this.status = BackgroundJobStatus.PENDING;
        this.targetType = targetType;
        this.targetId = targetId;
        this.idempotencyKey = idempotencyKey;
        this.requestedByUserId = requestedByUserId;
        this.maxAttempts = maxAttempts;
        this.correlationId = correlationId;
        this.requestedAt = requestedAt;
    }

    public void startAttempt(Instant attemptedAt) {
        this.status = BackgroundJobStatus.RUNNING;
        this.attemptCount++;
        this.lastAttemptAt = attemptedAt;
        this.nextAttemptAt = null;
    }

    public void scheduleRetry(String category, String message, Instant nextAttemptAt) {
        this.status = BackgroundJobStatus.PENDING;
        this.diagnosticCategory = category;
        this.diagnosticMessage = message;
        this.nextAttemptAt = nextAttemptAt;
    }

    public void succeed(Instant completedAt) {
        this.status = BackgroundJobStatus.SUCCEEDED;
        this.diagnosticCategory = null;
        this.diagnosticMessage = null;
        this.nextAttemptAt = null;
        this.completedAt = completedAt;
    }

    public void fail(String category, String message, Instant completedAt) {
        this.status = BackgroundJobStatus.FAILED;
        this.diagnosticCategory = category;
        this.diagnosticMessage = message;
        this.nextAttemptAt = null;
        this.completedAt = completedAt;
    }

    public boolean terminal() {
        return status == BackgroundJobStatus.SUCCEEDED || status == BackgroundJobStatus.FAILED;
    }

    public UUID id() {
        return id;
    }

    public UUID organisationId() {
        return organisationId;
    }

    public String jobType() {
        return jobType;
    }

    public BackgroundJobStatus status() {
        return status;
    }

    public String targetType() {
        return targetType;
    }

    public UUID targetId() {
        return targetId;
    }

    public String idempotencyKey() {
        return idempotencyKey;
    }

    public UUID requestedByUserId() {
        return requestedByUserId;
    }

    public int maxAttempts() {
        return maxAttempts;
    }

    public int attemptCount() {
        return attemptCount;
    }

    public String diagnosticCategory() {
        return diagnosticCategory;
    }

    public String diagnosticMessage() {
        return diagnosticMessage;
    }

    public String correlationId() {
        return correlationId;
    }

    public Instant requestedAt() {
        return requestedAt;
    }

    public Instant lastAttemptAt() {
        return lastAttemptAt;
    }

    public Instant nextAttemptAt() {
        return nextAttemptAt;
    }

    public Instant completedAt() {
        return completedAt;
    }
}

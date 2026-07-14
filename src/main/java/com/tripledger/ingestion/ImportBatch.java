package com.tripledger.ingestion;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "import_batch")
public class ImportBatch {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID organisationId;

    @Column(nullable = false)
    private UUID sourceSystemId;

    @Column(nullable = false)
    private String templateType;

    @Column(nullable = false)
    private String templateVersion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ImportBatchStatus status;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false)
    private String fileChecksum;

    @Column(nullable = false)
    private UUID receivedByUserId;

    @Column(nullable = false)
    private Instant receivedAt;

    private Instant completedAt;

    private String failureCode;

    private String failureReason;

    @Column(nullable = false)
    private int totalCount;

    @Column(nullable = false)
    private int acceptedCount;

    @Column(nullable = false)
    private int duplicateCount;

    @Column(nullable = false)
    private int rejectedCount;

    @Column(nullable = false)
    private int failedCount;

    protected ImportBatch() {
    }

    public ImportBatch(UUID id,
                       UUID organisationId,
                       UUID sourceSystemId,
                       String templateType,
                       String templateVersion,
                       ImportBatchStatus status,
                       String fileName,
                       String fileChecksum,
                       UUID receivedByUserId,
                       Instant receivedAt) {
        this.id = id;
        this.organisationId = organisationId;
        this.sourceSystemId = sourceSystemId;
        this.templateType = templateType;
        this.templateVersion = templateVersion;
        this.status = status;
        this.fileName = fileName;
        this.fileChecksum = fileChecksum;
        this.receivedByUserId = receivedByUserId;
        this.receivedAt = receivedAt;
    }

    public void record(ImportRowOutcome outcome) {
        totalCount++;
        switch (outcome) {
            case ACCEPTED -> acceptedCount++;
            case DUPLICATE -> duplicateCount++;
            case REJECTED -> rejectedCount++;
            case FAILED -> failedCount++;
        }
    }

    public void complete(Instant completedAt) {
        this.status = ImportBatchStatus.COMPLETED;
        this.completedAt = completedAt;
        this.failureCode = null;
        this.failureReason = null;
    }

    public void fail(String failureCode, String failureReason, Instant completedAt) {
        this.status = ImportBatchStatus.FAILED;
        this.failureCode = failureCode;
        this.failureReason = failureReason;
        this.completedAt = completedAt;
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

    public String templateType() {
        return templateType;
    }

    public String templateVersion() {
        return templateVersion;
    }

    public ImportBatchStatus status() {
        return status;
    }

    public String fileName() {
        return fileName;
    }

    public String fileChecksum() {
        return fileChecksum;
    }

    public UUID receivedByUserId() {
        return receivedByUserId;
    }

    public Instant receivedAt() {
        return receivedAt;
    }

    public Instant completedAt() {
        return completedAt;
    }

    public String failureCode() {
        return failureCode;
    }

    public String failureReason() {
        return failureReason;
    }

    public int totalCount() {
        return totalCount;
    }

    public int acceptedCount() {
        return acceptedCount;
    }

    public int duplicateCount() {
        return duplicateCount;
    }

    public int rejectedCount() {
        return rejectedCount;
    }

    public int failedCount() {
        return failedCount;
    }
}

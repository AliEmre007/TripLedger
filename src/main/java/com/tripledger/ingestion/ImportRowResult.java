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
@Table(name = "import_row_result")
public class ImportRowResult {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID organisationId;

    @Column(nullable = false)
    private UUID importBatchId;

    @Column(nullable = false)
    private int rowNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ImportRowOutcome outcome;

    private String fieldName;

    private String errorCode;

    private String reason;

    private UUID sourceRecordId;

    @Column(nullable = false)
    private Instant recordedAt;

    protected ImportRowResult() {
    }

    public ImportRowResult(UUID id,
                           UUID organisationId,
                           UUID importBatchId,
                           int rowNumber,
                           ImportRowOutcome outcome,
                           String fieldName,
                           String errorCode,
                           String reason,
                           UUID sourceRecordId,
                           Instant recordedAt) {
        this.id = id;
        this.organisationId = organisationId;
        this.importBatchId = importBatchId;
        this.rowNumber = rowNumber;
        this.outcome = outcome;
        this.fieldName = fieldName;
        this.errorCode = errorCode;
        this.reason = reason;
        this.sourceRecordId = sourceRecordId;
        this.recordedAt = recordedAt;
    }

    public UUID id() {
        return id;
    }

    public UUID organisationId() {
        return organisationId;
    }

    public UUID importBatchId() {
        return importBatchId;
    }

    public int rowNumber() {
        return rowNumber;
    }

    public ImportRowOutcome outcome() {
        return outcome;
    }

    public String fieldName() {
        return fieldName;
    }

    public String errorCode() {
        return errorCode;
    }

    public String reason() {
        return reason;
    }

    public UUID sourceRecordId() {
        return sourceRecordId;
    }

    public Instant recordedAt() {
        return recordedAt;
    }
}

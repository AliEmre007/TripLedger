package com.tripledger.ingestion;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "source_record")
public class SourceRecord {

    public static final String BOOKING_RECORD_TYPE = "BOOKING";

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID organisationId;

    @Column(nullable = false)
    private UUID sourceSystemId;

    @Column(nullable = false)
    private UUID importBatchId;

    @Column(nullable = false)
    private String recordType;

    @Column(nullable = false)
    private String externalRecordId;

    @Column(nullable = false)
    private String sourceVersion;

    @Column(nullable = false)
    private int sourceRowNumber;

    @Column(nullable = false)
    private String contentChecksum;

    private String payloadReference;

    @Column(nullable = false)
    private Instant acceptedAt;

    protected SourceRecord() {
    }

    public SourceRecord(UUID id,
                        UUID organisationId,
                        UUID sourceSystemId,
                        UUID importBatchId,
                        String recordType,
                        String externalRecordId,
                        String sourceVersion,
                        int sourceRowNumber,
                        String contentChecksum,
                        String payloadReference,
                        Instant acceptedAt) {
        this.id = id;
        this.organisationId = organisationId;
        this.sourceSystemId = sourceSystemId;
        this.importBatchId = importBatchId;
        this.recordType = recordType;
        this.externalRecordId = externalRecordId;
        this.sourceVersion = sourceVersion;
        this.sourceRowNumber = sourceRowNumber;
        this.contentChecksum = contentChecksum;
        this.payloadReference = payloadReference;
        this.acceptedAt = acceptedAt;
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

    public UUID importBatchId() {
        return importBatchId;
    }

    public String recordType() {
        return recordType;
    }

    public String externalRecordId() {
        return externalRecordId;
    }

    public String sourceVersion() {
        return sourceVersion;
    }

    public int sourceRowNumber() {
        return sourceRowNumber;
    }

    public String contentChecksum() {
        return contentChecksum;
    }

    public String payloadReference() {
        return payloadReference;
    }

    public Instant acceptedAt() {
        return acceptedAt;
    }
}

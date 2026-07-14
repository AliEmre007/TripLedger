package com.tripledger.booking;

import com.tripledger.ingestion.SourceRecord;
import java.time.Instant;
import java.util.UUID;

public record SourceRecordDetail(
        UUID id,
        UUID sourceSystemId,
        UUID importBatchId,
        String recordType,
        String externalRecordId,
        String sourceVersion,
        int sourceRowNumber,
        String contentChecksum,
        String payloadReference,
        Instant acceptedAt
) {

    public static SourceRecordDetail from(SourceRecord sourceRecord) {
        if (sourceRecord == null) {
            return null;
        }

        return new SourceRecordDetail(
                sourceRecord.id(),
                sourceRecord.sourceSystemId(),
                sourceRecord.importBatchId(),
                sourceRecord.recordType(),
                sourceRecord.externalRecordId(),
                sourceRecord.sourceVersion(),
                sourceRecord.sourceRowNumber(),
                sourceRecord.contentChecksum(),
                sourceRecord.payloadReference(),
                sourceRecord.acceptedAt()
        );
    }
}

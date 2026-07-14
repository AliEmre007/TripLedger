package com.tripledger.ingestion;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SourceRecordRepository extends JpaRepository<SourceRecord, UUID> {

    Optional<SourceRecord> findByOrganisationIdAndSourceSystemIdAndRecordTypeAndExternalRecordIdAndSourceVersion(
            UUID organisationId,
            UUID sourceSystemId,
            String recordType,
            String externalRecordId,
            String sourceVersion
    );
}

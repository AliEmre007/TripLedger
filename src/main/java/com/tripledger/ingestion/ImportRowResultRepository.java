package com.tripledger.ingestion;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImportRowResultRepository extends JpaRepository<ImportRowResult, UUID> {

    boolean existsByOrganisationIdAndImportBatchIdAndRowNumber(UUID organisationId, UUID importBatchId, int rowNumber);

    List<ImportRowResult> findByOrganisationIdAndImportBatchIdOrderByRowNumberAsc(
            UUID organisationId,
            UUID importBatchId
    );
}

package com.tripledger.ingestion;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImportBatchRepository extends JpaRepository<ImportBatch, UUID> {

    Optional<ImportBatch> findByOrganisationIdAndId(UUID organisationId, UUID id);

    List<ImportBatch> findByOrganisationIdOrderByReceivedAtDesc(UUID organisationId);
}

package com.tripledger.operations;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BackgroundJobRepository extends JpaRepository<BackgroundJob, UUID> {

    Optional<BackgroundJob> findByOrganisationIdAndId(UUID organisationId, UUID id);

    Optional<BackgroundJob> findByOrganisationIdAndJobTypeAndIdempotencyKey(
            UUID organisationId,
            String jobType,
            String idempotencyKey
    );
}

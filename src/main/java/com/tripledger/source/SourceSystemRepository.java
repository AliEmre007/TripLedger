package com.tripledger.source;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SourceSystemRepository extends JpaRepository<SourceSystem, UUID> {

    boolean existsByOrganisationIdAndExternalCode(UUID organisationId, String externalCode);

    List<SourceSystem> findByOrganisationIdOrderByExternalCodeAsc(UUID organisationId);

    Optional<SourceSystem> findByOrganisationIdAndId(UUID organisationId, UUID id);
}

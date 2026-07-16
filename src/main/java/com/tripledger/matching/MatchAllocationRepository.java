package com.tripledger.matching;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MatchAllocationRepository extends JpaRepository<MatchAllocation, UUID> {

    boolean existsByOrganisationIdAndFinancialEventIdAndActiveTrue(UUID organisationId, UUID financialEventId);

    List<MatchAllocation> findAllByOrganisationIdAndMatchIdInAndActiveTrue(
            UUID organisationId,
            Collection<UUID> matchIds
    );
}

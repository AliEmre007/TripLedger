package com.tripledger.finance;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExchangeRateEvidenceRepository extends JpaRepository<ExchangeRateEvidence, UUID> {

    List<ExchangeRateEvidence> findAllByOrganisationIdOrderByEffectiveAtDescCreatedAtDesc(UUID organisationId);

    List<ExchangeRateEvidence> findAllByOrganisationIdAndFinancialEventIdOrderByEffectiveAtDescCreatedAtDesc(
            UUID organisationId,
            UUID financialEventId);
}

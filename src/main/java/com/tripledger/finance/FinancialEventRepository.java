package com.tripledger.finance;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FinancialEventRepository extends JpaRepository<FinancialEvent, UUID> {

    Optional<FinancialEvent> findByOrganisationIdAndId(UUID organisationId, UUID id);

    Optional<FinancialEvent> findByOrganisationIdAndSourceRecordId(UUID organisationId, UUID sourceRecordId);

    boolean existsByOrganisationIdAndReversesEventId(UUID organisationId, UUID reversesEventId);

    List<FinancialEvent> findAllByOrganisationIdOrderByEffectiveAtDesc(UUID organisationId);

    List<FinancialEvent> findAllByOrganisationIdAndBookingIdIsNullOrderByEffectiveAtDesc(UUID organisationId);
}

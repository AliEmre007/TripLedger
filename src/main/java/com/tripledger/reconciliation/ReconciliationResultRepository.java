package com.tripledger.reconciliation;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReconciliationResultRepository extends JpaRepository<ReconciliationResult, UUID> {

    Optional<ReconciliationResult> findFirstByOrganisationIdAndBookingIdAndSupersededAtIsNullOrderByCreatedAtDesc(
            UUID organisationId,
            UUID bookingId
    );

    List<ReconciliationResult> findAllByOrganisationIdAndBookingIdOrderByCreatedAtAsc(
            UUID organisationId,
            UUID bookingId
    );
}

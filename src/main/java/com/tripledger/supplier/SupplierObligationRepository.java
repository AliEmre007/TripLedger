package com.tripledger.supplier;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupplierObligationRepository extends JpaRepository<SupplierObligation, UUID> {

    Optional<SupplierObligation> findByOrganisationIdAndSourceRecordId(UUID organisationId, UUID sourceRecordId);

    List<SupplierObligation> findAllByOrganisationIdOrderByCreatedAtDesc(UUID organisationId);

    List<SupplierObligation> findAllByOrganisationIdAndBookingIdIsNullAndBookingItemIdIsNullOrderByCreatedAtDesc(
            UUID organisationId
    );
}

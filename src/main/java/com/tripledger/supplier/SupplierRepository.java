package com.tripledger.supplier;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupplierRepository extends JpaRepository<Supplier, UUID> {

    Optional<Supplier> findByOrganisationIdAndId(UUID organisationId, UUID id);

    Optional<Supplier> findByOrganisationIdAndExternalReference(UUID organisationId, String externalReference);
}

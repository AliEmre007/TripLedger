package com.tripledger.discrepancy;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DiscrepancyRepository extends JpaRepository<Discrepancy, UUID> {

    Optional<Discrepancy>
            findByOrganisationIdAndBookingIdAndTypeAndComponentAndCauseIdentityAndStatus(
                    UUID organisationId,
                    UUID bookingId,
                    DiscrepancyType type,
                    String component,
                    String causeIdentity,
                    DiscrepancyStatus status
            );
}

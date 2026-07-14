package com.tripledger.booking;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingRepository extends JpaRepository<Booking, UUID> {

    Optional<Booking> findByOrganisationIdAndSourceSystemIdAndExternalBookingId(
            UUID organisationId,
            UUID sourceSystemId,
            String externalBookingId
    );
}

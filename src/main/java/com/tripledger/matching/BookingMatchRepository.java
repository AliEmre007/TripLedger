package com.tripledger.matching;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingMatchRepository extends JpaRepository<BookingMatch, UUID> {

    List<BookingMatch> findAllByOrganisationIdAndBookingIdOrderByCreatedAtDesc(
            UUID organisationId,
            UUID bookingId
    );
}

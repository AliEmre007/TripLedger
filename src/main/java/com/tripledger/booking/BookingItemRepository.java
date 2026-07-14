package com.tripledger.booking;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingItemRepository extends JpaRepository<BookingItem, UUID> {

    Optional<BookingItem> findByOrganisationIdAndBookingIdAndItemExternalId(
            UUID organisationId,
            UUID bookingId,
            String itemExternalId
    );
}

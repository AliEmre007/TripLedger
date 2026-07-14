package com.tripledger.booking;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingItemRepository extends JpaRepository<BookingItem, UUID> {

    List<BookingItem> findAllByOrganisationIdAndBookingIdOrderByItemExternalIdAsc(
            UUID organisationId,
            UUID bookingId
    );

    Optional<BookingItem> findByOrganisationIdAndBookingIdAndItemExternalId(
            UUID organisationId,
            UUID bookingId,
            String itemExternalId
    );
}

package com.tripledger.booking;

import com.tripledger.identity.ActorContext;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class BookingListService {

    private final BookingRepository bookingRepository;

    public BookingListService(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    public List<BookingSummary> list(ActorContext actor) {
        return bookingRepository.findAllByOrganisationIdOrderByCreatedAtDesc(actor.organisationId())
                .stream()
                .map(BookingSummary::from)
                .toList();
    }
}

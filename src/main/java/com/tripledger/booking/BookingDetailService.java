package com.tripledger.booking;

import com.tripledger.authorization.AuthorizationService;
import com.tripledger.authorization.Permission;
import com.tripledger.common.api.ApiException;
import com.tripledger.identity.ActorContext;
import com.tripledger.ingestion.SourceRecord;
import com.tripledger.ingestion.SourceRecordRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookingDetailService {

    private final BookingRepository bookingRepository;
    private final BookingItemRepository bookingItemRepository;
    private final SourceRecordRepository sourceRecordRepository;
    private final AuthorizationService authorizationService;

    public BookingDetailService(BookingRepository bookingRepository,
                                BookingItemRepository bookingItemRepository,
                                SourceRecordRepository sourceRecordRepository,
                                AuthorizationService authorizationService) {
        this.bookingRepository = bookingRepository;
        this.bookingItemRepository = bookingItemRepository;
        this.sourceRecordRepository = sourceRecordRepository;
        this.authorizationService = authorizationService;
    }

    @Transactional(readOnly = true)
    public BookingDetail get(ActorContext actor, UUID bookingId) {
        authorizationService.require(actor, Permission.PROTECTED_READ);

        Booking booking = bookingRepository.findByOrganisationIdAndId(actor.organisationId(), bookingId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "BOOKING_NOT_FOUND",
                        "Booking was not found."
                ));
        SourceRecord currentSourceRecord = sourceRecord(booking.organisationId(), booking.currentSourceRecordId());
        List<BookingItemDetail> items = bookingItemRepository
                .findAllByOrganisationIdAndBookingIdOrderByItemExternalIdAsc(actor.organisationId(), booking.id())
                .stream()
                .map(item -> BookingItemDetail.from(item, sourceRecord(item.organisationId(), item.sourceRecordId())))
                .toList();

        return BookingDetail.from(booking, SourceRecordDetail.from(currentSourceRecord), items);
    }

    private SourceRecord sourceRecord(UUID organisationId, UUID sourceRecordId) {
        if (sourceRecordId == null) {
            return null;
        }

        return sourceRecordRepository.findByOrganisationIdAndId(organisationId, sourceRecordId)
                .orElse(null);
    }
}

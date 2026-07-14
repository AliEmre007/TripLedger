package com.tripledger.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tripledger.authorization.AuthorizationService;
import com.tripledger.authorization.Permission;
import com.tripledger.common.api.ApiException;
import com.tripledger.identity.ActorContext;
import com.tripledger.identity.UserRole;
import com.tripledger.ingestion.SourceRecord;
import com.tripledger.ingestion.SourceRecordRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class BookingDetailServiceTest {

    private static final UUID ORGANISATION_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID OTHER_ORGANISATION_ID = UUID.fromString("99999999-9999-9999-9999-999999999999");
    private static final UUID USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID SOURCE_SYSTEM_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID BATCH_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID BOOKING_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final UUID SOURCE_RECORD_ID = UUID.fromString("66666666-6666-6666-6666-666666666666");
    private static final UUID ITEM_ID = UUID.fromString("77777777-7777-7777-7777-777777777777");
    private static final Instant NOW = Instant.parse("2026-07-14T06:00:00Z");

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private BookingItemRepository bookingItemRepository;

    @Mock
    private SourceRecordRepository sourceRecordRepository;

    @Mock
    private AuthorizationService authorizationService;

    @Test
    void returnsCanonicalBookingDetailWithItemsAndProvenance() {
        when(bookingRepository.findByOrganisationIdAndId(ORGANISATION_ID, BOOKING_ID))
                .thenReturn(Optional.of(booking(ORGANISATION_ID)));
        when(bookingItemRepository.findAllByOrganisationIdAndBookingIdOrderByItemExternalIdAsc(
                ORGANISATION_ID,
                BOOKING_ID
        )).thenReturn(List.of(bookingItem(ORGANISATION_ID)));
        when(sourceRecordRepository.findByOrganisationIdAndId(ORGANISATION_ID, SOURCE_RECORD_ID))
                .thenReturn(Optional.of(sourceRecord(ORGANISATION_ID)));

        BookingDetail detail = service().get(actor(ORGANISATION_ID), BOOKING_ID);

        assertThat(detail.id()).isEqualTo(BOOKING_ID);
        assertThat(detail.externalBookingId()).isEqualTo("TL-BKG-1001");
        assertThat(detail.lifecycleStatus()).isEqualTo(BookingLifecycleStatus.CONFIRMED);
        assertThat(detail.contractedSellingAmount()).isEqualByComparingTo(new BigDecimal("1000.00"));
        assertThat(detail.currentSourceRecord().sourceVersion()).isEqualTo("1");
        assertThat(detail.items()).hasSize(1);
        assertThat(detail.items().getFirst().itemExternalId()).isEqualTo("ITEM-1");
        assertThat(detail.items().getFirst().sourceRecord().importBatchId()).isEqualTo(BATCH_ID);
        verify(authorizationService).require(actor(ORGANISATION_ID), Permission.PROTECTED_READ);
    }

    @Test
    void returnsNotFoundForMissingOrWrongOrganisationBooking() {
        when(bookingRepository.findByOrganisationIdAndId(OTHER_ORGANISATION_ID, BOOKING_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().get(actor(OTHER_ORGANISATION_ID), BOOKING_ID))
                .isInstanceOfSatisfying(ApiException.class, exception -> {
                    assertThat(exception.status()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(exception.code()).isEqualTo("BOOKING_NOT_FOUND");
                });
    }

    private BookingDetailService service() {
        return new BookingDetailService(
                bookingRepository,
                bookingItemRepository,
                sourceRecordRepository,
                authorizationService
        );
    }

    private Booking booking(UUID organisationId) {
        return new Booking(
                BOOKING_ID,
                organisationId,
                SOURCE_SYSTEM_ID,
                "TL-BKG-1001",
                SOURCE_RECORD_ID,
                LocalDate.parse("2026-07-01"),
                LocalDate.parse("2026-08-01"),
                LocalDate.parse("2026-08-07"),
                BookingLifecycleStatus.CONFIRMED,
                "EUR",
                new BigDecimal("1000.00"),
                "CUST-1001",
                NOW,
                NOW
        );
    }

    private BookingItem bookingItem(UUID organisationId) {
        return new BookingItem(
                ITEM_ID,
                organisationId,
                BOOKING_ID,
                SOURCE_RECORD_ID,
                "ITEM-1",
                BookingItemServiceType.HOTEL,
                LocalDate.parse("2026-08-01"),
                LocalDate.parse("2026-08-07"),
                new BigDecimal("1000.00"),
                "EUR",
                BookingItemState.ACTIVE
        );
    }

    private SourceRecord sourceRecord(UUID organisationId) {
        return new SourceRecord(
                SOURCE_RECORD_ID,
                organisationId,
                SOURCE_SYSTEM_ID,
                BATCH_ID,
                SourceRecord.BOOKING_RECORD_TYPE,
                "TL-BKG-1001",
                "1",
                2,
                "sha256:abc",
                null,
                NOW
        );
    }

    private ActorContext actor(UUID organisationId) {
        return new ActorContext(
                USER_ID,
                organisationId,
                "Test User",
                UserRole.READ_ONLY_MANAGER,
                false,
                "corr-123"
        );
    }
}

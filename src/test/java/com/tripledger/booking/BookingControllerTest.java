package com.tripledger.booking;

import static org.hamcrest.Matchers.blankOrNullString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tripledger.common.api.ApiException;
import com.tripledger.identity.ActorContext;
import com.tripledger.identity.ActorContextResolver;
import com.tripledger.identity.UserRole;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(BookingController.class)
class BookingControllerTest {

    private static final UUID ORGANISATION_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID SOURCE_SYSTEM_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID BATCH_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID BOOKING_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final UUID SOURCE_RECORD_ID = UUID.fromString("66666666-6666-6666-6666-666666666666");
    private static final UUID ITEM_ID = UUID.fromString("77777777-7777-7777-7777-777777777777");
    private static final Instant NOW = Instant.parse("2026-07-14T06:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ActorContextResolver actorContextResolver;

    @MockitoBean
    private BookingDetailService bookingDetailService;

    @MockitoBean
    private BookingListService bookingListService;

    @Test
    void returnsBookingList() throws Exception {
        when(actorContextResolver.resolve(any())).thenReturn(actor());
        when(bookingListService.list(eq(actor()))).thenReturn(List.of(summary()));

        mockMvc.perform(get("/api/v1/bookings"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Correlation-Id", not(blankOrNullString())))
                .andExpect(jsonPath("$[0].id").value(BOOKING_ID.toString()))
                .andExpect(jsonPath("$[0].externalBookingId").value("TL-BKG-1001"))
                .andExpect(jsonPath("$[0].lifecycleStatus").value("CONFIRMED"))
                .andExpect(jsonPath("$[0].contractedSellingAmount").value(1000.00))
                .andExpect(jsonPath("$[0].sellingCurrency").value("EUR"));
    }

    @Test
    void returnsBookingDetail() throws Exception {
        when(actorContextResolver.resolve(any())).thenReturn(actor());
        when(bookingDetailService.get(eq(actor()), eq(BOOKING_ID))).thenReturn(detail());

        mockMvc.perform(get("/api/v1/bookings/{bookingId}", BOOKING_ID))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Correlation-Id", not(blankOrNullString())))
                .andExpect(jsonPath("$.id").value(BOOKING_ID.toString()))
                .andExpect(jsonPath("$.organisationId").value(ORGANISATION_ID.toString()))
                .andExpect(jsonPath("$.externalBookingId").value("TL-BKG-1001"))
                .andExpect(jsonPath("$.lifecycleStatus").value("CONFIRMED"))
                .andExpect(jsonPath("$.currentSourceRecord.sourceVersion").value("1"))
                .andExpect(jsonPath("$.items[0].itemExternalId").value("ITEM-1"))
                .andExpect(jsonPath("$.items[0].sourceRecord.importBatchId").value(BATCH_ID.toString()));
    }

    @Test
    void missingBookingReturnsStableErrorShape() throws Exception {
        when(actorContextResolver.resolve(any())).thenReturn(actor());
        when(bookingDetailService.get(any(), eq(BOOKING_ID))).thenThrow(new ApiException(
                HttpStatus.NOT_FOUND,
                "BOOKING_NOT_FOUND",
                "Booking was not found."
        ));

        mockMvc.perform(get("/api/v1/bookings/{bookingId}", BOOKING_ID))
                .andExpect(status().isNotFound())
                .andExpect(header().string("X-Correlation-Id", not(blankOrNullString())))
                .andExpect(jsonPath("$.error.code").value("BOOKING_NOT_FOUND"))
                .andExpect(jsonPath("$.error.correlationId").isNotEmpty());
    }

    private BookingDetail detail() {
        SourceRecordDetail sourceRecord = new SourceRecordDetail(
                SOURCE_RECORD_ID,
                SOURCE_SYSTEM_ID,
                BATCH_ID,
                "BOOKING",
                "TL-BKG-1001",
                "1",
                2,
                "sha256:abc",
                null,
                NOW
        );
        BookingItemDetail item = new BookingItemDetail(
                ITEM_ID,
                "ITEM-1",
                BookingItemServiceType.HOTEL,
                LocalDate.parse("2026-08-01"),
                LocalDate.parse("2026-08-07"),
                new BigDecimal("1000.00"),
                "EUR",
                BookingItemState.ACTIVE,
                sourceRecord
        );

        return new BookingDetail(
                BOOKING_ID,
                ORGANISATION_ID,
                SOURCE_SYSTEM_ID,
                "TL-BKG-1001",
                LocalDate.parse("2026-07-01"),
                LocalDate.parse("2026-08-01"),
                LocalDate.parse("2026-08-07"),
                BookingLifecycleStatus.CONFIRMED,
                new BigDecimal("1000.00"),
                "EUR",
                "CUST-1001",
                sourceRecord,
                List.of(item),
                NOW,
                NOW
        );
    }

    private BookingSummary summary() {
        return new BookingSummary(
                BOOKING_ID,
                ORGANISATION_ID,
                SOURCE_SYSTEM_ID,
                "TL-BKG-1001",
                LocalDate.parse("2026-07-01"),
                LocalDate.parse("2026-08-01"),
                LocalDate.parse("2026-08-07"),
                BookingLifecycleStatus.CONFIRMED,
                new BigDecimal("1000.00"),
                "EUR",
                "CUST-1001"
        );
    }

    private ActorContext actor() {
        return new ActorContext(
                USER_ID,
                ORGANISATION_ID,
                "Test User",
                UserRole.READ_ONLY_MANAGER,
                false,
                "corr-123"
        );
    }
}

package com.tripledger.timeline;

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
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(BookingTimelineController.class)
class BookingTimelineControllerTest {

    private static final UUID ORGANISATION_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID BOOKING_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID EVENT_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ActorContextResolver actorContextResolver;

    @MockitoBean
    private BookingTimelineService bookingTimelineService;

    @Test
    void returnsBookingTimeline() throws Exception {
        when(actorContextResolver.resolve(any())).thenReturn(actor());
        when(bookingTimelineService.get(eq(actor()), eq(BOOKING_ID))).thenReturn(timeline());

        mockMvc.perform(get("/api/v1/bookings/{bookingId}/timeline", BOOKING_ID))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Correlation-Id", not(blankOrNullString())))
                .andExpect(jsonPath("$.bookingId").value(BOOKING_ID.toString()))
                .andExpect(jsonPath("$.events[0].category").value("SOURCE"))
                .andExpect(jsonPath("$.events[0].eventType").value("FINANCIAL_EVENT_ACCEPTED"))
                .andExpect(jsonPath("$.events[0].amount").value(950.00));
    }

    @Test
    void missingBookingReturnsStableErrorShape() throws Exception {
        when(actorContextResolver.resolve(any())).thenReturn(actor());
        when(bookingTimelineService.get(any(), eq(BOOKING_ID))).thenThrow(new ApiException(
                HttpStatus.NOT_FOUND,
                "BOOKING_NOT_FOUND",
                "Booking was not found."
        ));

        mockMvc.perform(get("/api/v1/bookings/{bookingId}/timeline", BOOKING_ID))
                .andExpect(status().isNotFound())
                .andExpect(header().string("X-Correlation-Id", not(blankOrNullString())))
                .andExpect(jsonPath("$.error.code").value("BOOKING_NOT_FOUND"))
                .andExpect(jsonPath("$.error.correlationId").isNotEmpty());
    }

    private BookingTimeline timeline() {
        return new BookingTimeline(
                BOOKING_ID,
                ORGANISATION_ID,
                List.of(new TimelineEvent(
                        EVENT_ID,
                        Instant.parse("2026-07-02T08:01:00Z"),
                        TimelineEventCategory.SOURCE,
                        "FINANCIAL_EVENT_ACCEPTED",
                        "Financial event accepted",
                        "CUSTOMER_PAYMENT EUR 950.00 accepted.",
                        "FINANCIAL_EVENT",
                        EVENT_ID,
                        null,
                        new BigDecimal("950.00"),
                        "EUR",
                        "CUSTOMER_PAYMENT",
                        "financial_event:" + EVENT_ID
                ))
        );
    }

    private ActorContext actor() {
        return new ActorContext(
                USER_ID,
                ORGANISATION_ID,
                "Finance User",
                UserRole.FINANCE,
                true,
                "corr-123"
        );
    }
}

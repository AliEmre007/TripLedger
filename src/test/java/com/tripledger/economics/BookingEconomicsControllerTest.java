package com.tripledger.economics;

import static org.hamcrest.Matchers.blankOrNullString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
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

@WebMvcTest(BookingEconomicsController.class)
class BookingEconomicsControllerTest {

    private static final UUID ORGANISATION_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID BOOKING_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID SNAPSHOT_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final Instant NOW = Instant.parse("2026-07-14T06:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ActorContextResolver actorContextResolver;

    @MockitoBean
    private BookingEconomicsService bookingEconomicsService;

    @Test
    void returnsCurrentBookingEconomics() throws Exception {
        when(actorContextResolver.resolve(any())).thenReturn(actor());
        when(bookingEconomicsService.calculate(any(), any())).thenReturn(detail());

        mockMvc.perform(get("/api/v1/bookings/{bookingId}/economics", BOOKING_ID))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Correlation-Id", not(blankOrNullString())))
                .andExpect(jsonPath("$.snapshotId").value(SNAPSHOT_ID.toString()))
                .andExpect(jsonPath("$.bookingId").value(BOOKING_ID.toString()))
                .andExpect(jsonPath("$.ruleVersion").value("economics-v1"))
                .andExpect(jsonPath("$.expectedCustomerReceivable").value(950.00))
                .andExpect(jsonPath("$.expectedDeductions").value(162.50))
                .andExpect(jsonPath("$.activeSupplierCost").value(500.00))
                .andExpect(jsonPath("$.estimatedGrossMargin").value(287.50))
                .andExpect(jsonPath("$.status").value("READY"));
    }

    @Test
    void missingBookingReturnsStableErrorShape() throws Exception {
        when(actorContextResolver.resolve(any())).thenReturn(actor());
        when(bookingEconomicsService.calculate(any(), any())).thenThrow(new ApiException(
                HttpStatus.NOT_FOUND,
                "BOOKING_NOT_FOUND",
                "Booking was not found."));

        mockMvc.perform(get("/api/v1/bookings/{bookingId}/economics", BOOKING_ID))
                .andExpect(status().isNotFound())
                .andExpect(header().string("X-Correlation-Id", not(blankOrNullString())))
                .andExpect(jsonPath("$.error.code").value("BOOKING_NOT_FOUND"))
                .andExpect(jsonPath("$.error.correlationId").isNotEmpty());
    }

    private BookingEconomicsDetail detail() {
        return new BookingEconomicsDetail(
                SNAPSHOT_ID,
                BOOKING_ID,
                "economics-v1",
                "EUR",
                new BigDecimal("1000.00"),
                new BigDecimal("950.00"),
                new BigDecimal("162.50"),
                new BigDecimal("500.00"),
                new BigDecimal("287.50"),
                CalculationStatus.READY,
                List.of(),
                NOW
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

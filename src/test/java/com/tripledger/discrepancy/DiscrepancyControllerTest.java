package com.tripledger.discrepancy;

import static org.hamcrest.Matchers.blankOrNullString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tripledger.booking.BookingLifecycleStatus;
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

@WebMvcTest(DiscrepancyController.class)
class DiscrepancyControllerTest {

    private static final UUID ORGANISATION_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID BOOKING_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID DISCREPANCY_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID SOURCE_SYSTEM_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final Instant CREATED_AT = Instant.parse("2026-07-14T10:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ActorContextResolver actorContextResolver;

    @MockitoBean
    private DiscrepancyQueryService discrepancyQueryService;

    @Test
    void listsDiscrepanciesWithSummary() throws Exception {
        when(actorContextResolver.resolve(any())).thenReturn(actor());
        when(discrepancyQueryService.list(
                eq(actor()),
                eq("ACTIVE"),
                eq("SHORT_SETTLEMENT"),
                eq(null),
                eq(null),
                eq("EUR"),
                eq(0),
                eq(20)
        )).thenReturn(listResponse());

        mockMvc.perform(get("/api/v1/discrepancies")
                        .param("status", "ACTIVE")
                        .param("type", "SHORT_SETTLEMENT")
                        .param("currency", "EUR")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Correlation-Id", not(blankOrNullString())))
                .andExpect(jsonPath("$.items[0].id").value(DISCREPANCY_ID.toString()))
                .andExpect(jsonPath("$.items[0].type").value("SHORT_SETTLEMENT"))
                .andExpect(jsonPath("$.items[0].amount").value(50.00))
                .andExpect(jsonPath("$.summary.totalCount").value(1))
                .andExpect(jsonPath("$.summary.totalAmount").value(50.00));
    }

    @Test
    void returnsDiscrepancyDetail() throws Exception {
        when(actorContextResolver.resolve(any())).thenReturn(actor());
        when(discrepancyQueryService.get(eq(actor()), eq(DISCREPANCY_ID))).thenReturn(detail());

        mockMvc.perform(get("/api/v1/discrepancies/{discrepancyId}", DISCREPANCY_ID))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Correlation-Id", not(blankOrNullString())))
                .andExpect(jsonPath("$.id").value(DISCREPANCY_ID.toString()))
                .andExpect(jsonPath("$.causeIdentity").value("expected=850.00;matched=800.00;currency=EUR"))
                .andExpect(jsonPath("$.booking.externalBookingId").value("TL-BKG-1001"))
                .andExpect(jsonPath("$.booking.lifecycleStatus").value("CONFIRMED"));
    }

    @Test
    void missingDiscrepancyReturnsStableErrorShape() throws Exception {
        when(actorContextResolver.resolve(any())).thenReturn(actor());
        when(discrepancyQueryService.get(any(), eq(DISCREPANCY_ID))).thenThrow(new ApiException(
                HttpStatus.NOT_FOUND,
                "DISCREPANCY_NOT_FOUND",
                "Discrepancy was not found."
        ));

        mockMvc.perform(get("/api/v1/discrepancies/{discrepancyId}", DISCREPANCY_ID))
                .andExpect(status().isNotFound())
                .andExpect(header().string("X-Correlation-Id", not(blankOrNullString())))
                .andExpect(jsonPath("$.error.code").value("DISCREPANCY_NOT_FOUND"))
                .andExpect(jsonPath("$.error.correlationId").isNotEmpty());
    }

    private DiscrepancyListResponse listResponse() {
        DiscrepancySummary summary = new DiscrepancySummary(
                DISCREPANCY_ID,
                ORGANISATION_ID,
                BOOKING_ID,
                DiscrepancyType.SHORT_SETTLEMENT,
                DiscrepancySeverity.HIGH,
                "EXPECTED_CUSTOMER_RECEIVABLE",
                new BigDecimal("50.00"),
                "EUR",
                DiscrepancyStatus.ACTIVE,
                USER_ID,
                "Expected EUR 850.00 but matched EUR 800.00; variance EUR 50.00.",
                2,
                CREATED_AT,
                null
        );
        return new DiscrepancyListResponse(
                List.of(summary),
                new DiscrepancyQueueSummary(1, 1, 0, new BigDecimal("50.00")),
                0,
                20,
                1,
                1
        );
    }

    private DiscrepancyDetail detail() {
        return new DiscrepancyDetail(
                DISCREPANCY_ID,
                ORGANISATION_ID,
                BOOKING_ID,
                DiscrepancyType.SHORT_SETTLEMENT,
                DiscrepancySeverity.HIGH,
                "EXPECTED_CUSTOMER_RECEIVABLE",
                "expected=850.00;matched=800.00;currency=EUR",
                new BigDecimal("50.00"),
                "EUR",
                DiscrepancyStatus.ACTIVE,
                USER_ID,
                "Expected EUR 850.00 but matched EUR 800.00; variance EUR 50.00.",
                2,
                CREATED_AT,
                null,
                new DiscrepancyBookingEvidence(
                        BOOKING_ID,
                        SOURCE_SYSTEM_ID,
                        "TL-BKG-1001",
                        LocalDate.parse("2026-07-01"),
                        LocalDate.parse("2026-08-01"),
                        LocalDate.parse("2026-08-07"),
                        BookingLifecycleStatus.CONFIRMED,
                        new BigDecimal("1000.00"),
                        "EUR",
                        "CUST-1001"
                )
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

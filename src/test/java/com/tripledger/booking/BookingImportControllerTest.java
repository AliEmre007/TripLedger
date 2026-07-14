package com.tripledger.booking;

import static org.hamcrest.Matchers.blankOrNullString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tripledger.common.api.ApiException;
import com.tripledger.identity.ActorContext;
import com.tripledger.identity.ActorContextResolver;
import com.tripledger.identity.UserRole;
import com.tripledger.ingestion.ImportBatch;
import com.tripledger.ingestion.ImportBatchStatus;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(BookingImportController.class)
class BookingImportControllerTest {

    private static final UUID ORGANISATION_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID SOURCE_SYSTEM_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID BATCH_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final Instant NOW = Instant.parse("2026-07-14T06:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ActorContextResolver actorContextResolver;

    @MockitoBean
    private BookingCsvImportService bookingCsvImportService;

    @Test
    void importsBookingCsv() throws Exception {
        when(actorContextResolver.resolve(any())).thenReturn(actor());
        when(bookingCsvImportService.importCsv(any(), any())).thenReturn(completedBatch());

        mockMvc.perform(post("/api/v1/booking-imports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sourceSystemId": "33333333-3333-3333-3333-333333333333",
                                  "fileName": "bookings.csv",
                                  "fileChecksum": "sha256:abc",
                                  "csvContent": "template_type,template_version,external_booking_id\\\\n"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("X-Correlation-Id", not(blankOrNullString())))
                .andExpect(jsonPath("$.importBatchId").value(BATCH_ID.toString()))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.acceptedCount").value(1));
    }

    @Test
    void inactiveSourceSystemReturnsStableErrorShape() throws Exception {
        when(actorContextResolver.resolve(any())).thenReturn(actor());
        when(bookingCsvImportService.importCsv(any(), any())).thenThrow(new ApiException(
                HttpStatus.CONFLICT,
                "INACTIVE_SOURCE_SYSTEM",
                "Inactive source systems cannot receive new imports."));

        mockMvc.perform(post("/api/v1/booking-imports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sourceSystemId": "33333333-3333-3333-3333-333333333333",
                                  "fileName": "bookings.csv",
                                  "fileChecksum": "sha256:abc",
                                  "csvContent": "template_type,template_version,external_booking_id\\\\n"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(header().string("X-Correlation-Id", not(blankOrNullString())))
                .andExpect(jsonPath("$.error.code").value("INACTIVE_SOURCE_SYSTEM"))
                .andExpect(jsonPath("$.error.correlationId").isNotEmpty());
    }

    private ImportBatch completedBatch() {
        ImportBatch batch = new ImportBatch(
                BATCH_ID,
                ORGANISATION_ID,
                SOURCE_SYSTEM_ID,
                "BOOKING",
                "1",
                ImportBatchStatus.RECEIVED,
                "bookings.csv",
                "sha256:abc",
                USER_ID,
                NOW
        );
        batch.record(com.tripledger.ingestion.ImportRowOutcome.ACCEPTED);
        batch.complete(NOW);
        return batch;
    }

    private ActorContext actor() {
        return new ActorContext(
                USER_ID,
                ORGANISATION_ID,
                "Test User",
                UserRole.OPERATIONS,
                true,
                "corr-123"
        );
    }
}

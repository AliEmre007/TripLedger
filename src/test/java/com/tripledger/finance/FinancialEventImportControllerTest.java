package com.tripledger.finance;

import static org.hamcrest.Matchers.blankOrNullString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tripledger.booking.SourceRecordDetail;
import com.tripledger.common.api.ApiException;
import com.tripledger.identity.ActorContext;
import com.tripledger.identity.ActorContextResolver;
import com.tripledger.identity.UserRole;
import com.tripledger.ingestion.ImportBatch;
import com.tripledger.ingestion.ImportBatchStatus;
import com.tripledger.ingestion.ImportRowOutcome;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(FinancialEventImportController.class)
class FinancialEventImportControllerTest {

    private static final UUID ORGANISATION_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID SOURCE_SYSTEM_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID BATCH_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID EVENT_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final UUID SOURCE_RECORD_ID = UUID.fromString("66666666-6666-6666-6666-666666666666");
    private static final Instant NOW = Instant.parse("2026-07-14T06:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ActorContextResolver actorContextResolver;

    @MockitoBean
    private FinancialEventCsvImportService financialEventCsvImportService;

    @MockitoBean
    private FinancialEventQueryService financialEventQueryService;

    @MockitoBean
    private FinancialEventReversalService financialEventReversalService;

    @Test
    void importsFinancialEventCsv() throws Exception {
        when(actorContextResolver.resolve(any())).thenReturn(actor());
        when(financialEventCsvImportService.importCsv(any(), any())).thenReturn(completedBatch());

        mockMvc.perform(post("/api/v1/financial-event-imports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sourceSystemId": "33333333-3333-3333-3333-333333333333",
                                  "fileName": "financial.csv",
                                  "fileChecksum": "sha256:abc",
                                  "csvContent": "template_type,template_version,external_event_id\\\\n"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("X-Correlation-Id", not(blankOrNullString())))
                .andExpect(jsonPath("$.importBatchId").value(BATCH_ID.toString()))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.acceptedCount").value(1));
    }

    @Test
    void listsUnmatchedFinancialEventsForReview() throws Exception {
        when(actorContextResolver.resolve(any())).thenReturn(actor());
        when(financialEventQueryService.list(any(), any())).thenReturn(List.of(unmatchedDetail()));

        mockMvc.perform(get("/api/v1/financial-events?unmatched=true"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Correlation-Id", not(blankOrNullString())))
                .andExpect(jsonPath("$[0].id").value(EVENT_ID.toString()))
                .andExpect(jsonPath("$[0].bookingId").doesNotExist())
                .andExpect(jsonPath("$[0].eventType").value("CUSTOMER_PAYMENT"))
                .andExpect(jsonPath("$[0].matchedToBooking").value(false))
                .andExpect(jsonPath("$[0].sourceRecord.recordType").value("FINANCIAL_EVENT"));
    }

    @Test
    void mfaRequiredReturnsStableErrorShape() throws Exception {
        when(actorContextResolver.resolve(any())).thenReturn(actor());
        when(financialEventCsvImportService.importCsv(any(), any())).thenThrow(new ApiException(
                HttpStatus.FORBIDDEN,
                "MFA_REQUIRED",
                "MFA is required for this action."));

        mockMvc.perform(post("/api/v1/financial-event-imports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sourceSystemId": "33333333-3333-3333-3333-333333333333",
                                  "fileName": "financial.csv",
                                  "fileChecksum": "sha256:abc",
                                  "csvContent": "template_type,template_version,external_event_id\\\\n"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(header().string("X-Correlation-Id", not(blankOrNullString())))
                .andExpect(jsonPath("$.error.code").value("MFA_REQUIRED"))
                .andExpect(jsonPath("$.error.correlationId").isNotEmpty());
    }

    @Test
    void reversesFinancialEvent() throws Exception {
        when(actorContextResolver.resolve(any())).thenReturn(actor());
        when(financialEventReversalService.reverse(any(), any(), any()))
                .thenReturn(new FinancialEventReversalService.FinancialEventReversalResult(
                        reversalDetail(),
                        null
                ));

        mockMvc.perform(post("/api/v1/financial-events/{eventId}/reversal", EVENT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reason": "Gateway corrected duplicate payment.",
                                  "effectiveAt": "2026-07-15T09:00:00Z"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("X-Correlation-Id", not(blankOrNullString())))
                .andExpect(jsonPath("$.reversal.eventType").value("REVERSAL"))
                .andExpect(jsonPath("$.reversal.reversesEventId").value(EVENT_ID.toString()))
                .andExpect(jsonPath("$.replacementEvent").doesNotExist());
    }

    private ImportBatch completedBatch() {
        ImportBatch batch = new ImportBatch(
                BATCH_ID,
                ORGANISATION_ID,
                SOURCE_SYSTEM_ID,
                "FINANCIAL_EVENT",
                "1",
                ImportBatchStatus.RECEIVED,
                "financial.csv",
                "sha256:abc",
                USER_ID,
                NOW
        );
        batch.record(ImportRowOutcome.ACCEPTED);
        batch.complete(NOW);
        return batch;
    }

    private FinancialEventDetail unmatchedDetail() {
        SourceRecordDetail sourceRecord = new SourceRecordDetail(
                SOURCE_RECORD_ID,
                SOURCE_SYSTEM_ID,
                BATCH_ID,
                "FINANCIAL_EVENT",
                "PAY-UNMATCHED-1",
                "1",
                7,
                "sha256:abc",
                "unknown-booking-reference",
                NOW
        );
        return new FinancialEventDetail(
                EVENT_ID,
                ORGANISATION_ID,
                null,
                FinancialEventType.CUSTOMER_PAYMENT,
                FinancialEventDirection.INCREASE_RECEIVED,
                new BigDecimal("120.00"),
                "EUR",
                Instant.parse("2026-07-09T12:00:00Z"),
                "CUST-UNKNOWN",
                null,
                null,
                false,
                sourceRecord,
                USER_ID,
                NOW
        );
    }

    private FinancialEventDetail reversalDetail() {
        return new FinancialEventDetail(
                UUID.fromString("77777777-7777-7777-7777-777777777777"),
                ORGANISATION_ID,
                null,
                FinancialEventType.REVERSAL,
                FinancialEventDirection.REVERSAL,
                new BigDecimal("120.00"),
                "EUR",
                Instant.parse("2026-07-15T09:00:00Z"),
                "REVERSAL-" + EVENT_ID,
                EVENT_ID,
                "Gateway corrected duplicate payment.",
                false,
                null,
                USER_ID,
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

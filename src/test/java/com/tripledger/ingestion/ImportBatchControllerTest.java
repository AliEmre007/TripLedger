package com.tripledger.ingestion;

import static org.hamcrest.Matchers.blankOrNullString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tripledger.common.api.ApiException;
import com.tripledger.identity.ActorContext;
import com.tripledger.identity.ActorContextResolver;
import com.tripledger.identity.UserRole;
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

@WebMvcTest(ImportBatchController.class)
class ImportBatchControllerTest {

    private static final UUID ORGANISATION_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID SOURCE_SYSTEM_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID BATCH_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID ROW_RESULT_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final Instant NOW = Instant.parse("2026-07-14T06:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ActorContextResolver actorContextResolver;

    @MockitoBean
    private ImportBatchService importBatchService;

    @Test
    void startsImportBatch() throws Exception {
        when(actorContextResolver.resolve(any())).thenReturn(adminActor());
        when(importBatchService.start(any(), any())).thenReturn(batch(ImportBatchStatus.RECEIVED));

        mockMvc.perform(post("/api/v1/import-batches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sourceSystemId": "33333333-3333-3333-3333-333333333333",
                                  "templateType": "BOOKING_CSV",
                                  "templateVersion": "v1",
                                  "fileName": "bookings.csv",
                                  "fileChecksum": "sha256:abc"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("X-Correlation-Id", not(blankOrNullString())))
                .andExpect(jsonPath("$.id").value(BATCH_ID.toString()))
                .andExpect(jsonPath("$.organisationId").value(ORGANISATION_ID.toString()))
                .andExpect(jsonPath("$.sourceSystemId").value(SOURCE_SYSTEM_ID.toString()))
                .andExpect(jsonPath("$.status").value("RECEIVED"))
                .andExpect(jsonPath("$.totalCount").value(0));
    }

    @Test
    void listsImportBatchesForActorOrganisation() throws Exception {
        when(actorContextResolver.resolve(any())).thenReturn(adminActor());
        when(importBatchService.list(any())).thenReturn(List.of(batch(ImportBatchStatus.COMPLETED)));

        mockMvc.perform(get("/api/v1/import-batches"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Correlation-Id", not(blankOrNullString())))
                .andExpect(jsonPath("$[0].id").value(BATCH_ID.toString()))
                .andExpect(jsonPath("$[0].status").value("COMPLETED"))
                .andExpect(jsonPath("$[0].completedAt").isNotEmpty());
    }

    @Test
    void recordsRowResult() throws Exception {
        when(actorContextResolver.resolve(any())).thenReturn(adminActor());
        when(importBatchService.recordRowResult(eq(adminActor()), eq(BATCH_ID), any()))
                .thenReturn(rowResult());

        mockMvc.perform(post("/api/v1/import-batches/{batchId}/row-results", BATCH_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "rowNumber": 3,
                                  "outcome": "REJECTED",
                                  "fieldName": "sellingAmount",
                                  "errorCode": "INVALID_AMOUNT",
                                  "reason": "Amount is required."
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("X-Correlation-Id", not(blankOrNullString())))
                .andExpect(jsonPath("$.id").value(ROW_RESULT_ID.toString()))
                .andExpect(jsonPath("$.importBatchId").value(BATCH_ID.toString()))
                .andExpect(jsonPath("$.rowNumber").value(3))
                .andExpect(jsonPath("$.outcome").value("REJECTED"))
                .andExpect(jsonPath("$.errorCode").value("INVALID_AMOUNT"));
    }

    @Test
    void duplicateRowResultReturnsStableErrorShape() throws Exception {
        when(actorContextResolver.resolve(any())).thenReturn(adminActor());
        when(importBatchService.recordRowResult(any(), any(), any())).thenThrow(new ApiException(
                HttpStatus.CONFLICT,
                "DUPLICATE_IMPORT_ROW_RESULT",
                "Import row result already exists for this row number."));

        mockMvc.perform(post("/api/v1/import-batches/{batchId}/row-results", BATCH_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "rowNumber": 3,
                                  "outcome": "ACCEPTED"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(header().string("X-Correlation-Id", not(blankOrNullString())))
                .andExpect(jsonPath("$.error.code").value("DUPLICATE_IMPORT_ROW_RESULT"))
                .andExpect(jsonPath("$.error.correlationId").isNotEmpty());
    }

    @Test
    void invalidStartRequestReturnsStableErrorShape() throws Exception {
        when(actorContextResolver.resolve(any())).thenReturn(adminActor());

        mockMvc.perform(post("/api/v1/import-batches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sourceSystemId": "33333333-3333-3333-3333-333333333333",
                                  "templateType": "",
                                  "templateVersion": "v1",
                                  "fileName": "bookings.csv",
                                  "fileChecksum": "sha256:abc"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("X-Correlation-Id", not(blankOrNullString())))
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.error.details[0].field").value("templateType"));
    }

    @Test
    void completesImportBatch() throws Exception {
        when(actorContextResolver.resolve(any())).thenReturn(adminActor());
        when(importBatchService.complete(adminActor(), BATCH_ID)).thenReturn(batch(ImportBatchStatus.COMPLETED));

        mockMvc.perform(post("/api/v1/import-batches/{batchId}/complete", BATCH_ID))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Correlation-Id", not(blankOrNullString())))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.completedAt").isNotEmpty());
    }

    private ImportBatch batch(ImportBatchStatus status) {
        ImportBatch batch = new ImportBatch(
                BATCH_ID,
                ORGANISATION_ID,
                SOURCE_SYSTEM_ID,
                "BOOKING_CSV",
                "v1",
                ImportBatchStatus.RECEIVED,
                "bookings.csv",
                "sha256:abc",
                USER_ID,
                NOW
        );
        if (status == ImportBatchStatus.COMPLETED) {
            batch.complete(NOW);
        }
        if (status == ImportBatchStatus.FAILED) {
            batch.fail("UNSUPPORTED_TEMPLATE_VERSION", "Unsupported v9.", NOW);
        }
        return batch;
    }

    private ImportRowResult rowResult() {
        return new ImportRowResult(
                ROW_RESULT_ID,
                ORGANISATION_ID,
                BATCH_ID,
                3,
                ImportRowOutcome.REJECTED,
                "sellingAmount",
                "INVALID_AMOUNT",
                "Amount is required.",
                null,
                NOW
        );
    }

    private ActorContext adminActor() {
        return new ActorContext(
                USER_ID,
                ORGANISATION_ID,
                "Test User",
                UserRole.ADMINISTRATOR,
                true,
                "corr-123"
        );
    }
}

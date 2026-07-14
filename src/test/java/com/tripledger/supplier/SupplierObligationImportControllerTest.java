package com.tripledger.supplier;

import static org.hamcrest.Matchers.blankOrNullString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
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
import com.tripledger.ingestion.ImportBatch;
import com.tripledger.ingestion.ImportBatchStatus;
import com.tripledger.ingestion.ImportRowOutcome;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SupplierObligationImportController.class)
class SupplierObligationImportControllerTest {

    private static final UUID ORGANISATION_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID SOURCE_SYSTEM_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID BATCH_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID OBLIGATION_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final UUID SUPPLIER_ID = UUID.fromString("66666666-6666-6666-6666-666666666666");
    private static final Instant NOW = Instant.parse("2026-07-14T06:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ActorContextResolver actorContextResolver;

    @MockitoBean
    private SupplierObligationCsvImportService supplierObligationCsvImportService;

    @MockitoBean
    private SupplierObligationQueryService supplierObligationQueryService;

    @Test
    void importsSupplierObligationCsv() throws Exception {
        when(actorContextResolver.resolve(any())).thenReturn(actor());
        when(supplierObligationCsvImportService.importCsv(any(), any())).thenReturn(completedBatch());

        mockMvc.perform(post("/api/v1/supplier-obligation-imports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sourceSystemId": "33333333-3333-3333-3333-333333333333",
                                  "fileName": "supplier.csv",
                                  "fileChecksum": "sha256:abc",
                                  "csvContent": "template_type,template_version,external_obligation_id\\\\n"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("X-Correlation-Id", not(blankOrNullString())))
                .andExpect(jsonPath("$.importBatchId").value(BATCH_ID.toString()))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.acceptedCount").value(1));
    }

    @Test
    void listsUnlinkedSupplierObligationsForReview() throws Exception {
        when(actorContextResolver.resolve(any())).thenReturn(actor());
        when(supplierObligationQueryService.list(any(), any())).thenReturn(List.of(unlinkedDetail()));

        mockMvc.perform(get("/api/v1/supplier-obligations?unlinked=true"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Correlation-Id", not(blankOrNullString())))
                .andExpect(jsonPath("$[0].id").value(OBLIGATION_ID.toString()))
                .andExpect(jsonPath("$[0].bookingId").doesNotExist())
                .andExpect(jsonPath("$[0].supplierReference").value("TOUR-NOVA"))
                .andExpect(jsonPath("$[0].linkedToBookingEconomics").value(false))
                .andExpect(jsonPath("$[0].contributesToActiveSupplierCost").value(false));
    }

    @Test
    void inactiveSourceSystemReturnsStableErrorShape() throws Exception {
        when(actorContextResolver.resolve(any())).thenReturn(actor());
        when(supplierObligationCsvImportService.importCsv(any(), any())).thenThrow(new ApiException(
                HttpStatus.CONFLICT,
                "INACTIVE_SOURCE_SYSTEM",
                "Inactive source systems cannot receive new imports."));

        mockMvc.perform(post("/api/v1/supplier-obligation-imports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sourceSystemId": "33333333-3333-3333-3333-333333333333",
                                  "fileName": "supplier.csv",
                                  "fileChecksum": "sha256:abc",
                                  "csvContent": "template_type,template_version,external_obligation_id\\\\n"
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
                "SUPPLIER_OBLIGATION",
                "1",
                ImportBatchStatus.RECEIVED,
                "supplier.csv",
                "sha256:abc",
                USER_ID,
                NOW
        );
        batch.record(ImportRowOutcome.ACCEPTED);
        batch.complete(NOW);
        return batch;
    }

    private SupplierObligationDetail unlinkedDetail() {
        return new SupplierObligationDetail(
                OBLIGATION_ID,
                ORGANISATION_ID,
                null,
                null,
                SUPPLIER_ID,
                "TOUR-NOVA",
                "Tour Nova",
                new BigDecimal("75.00"),
                "EUR",
                LocalDate.parse("2026-08-12"),
                SupplierObligationStatus.EXPECTED,
                false,
                false,
                null,
                NOW
        );
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

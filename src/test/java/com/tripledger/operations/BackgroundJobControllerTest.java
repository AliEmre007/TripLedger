package com.tripledger.operations;

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
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(BackgroundJobController.class)
class BackgroundJobControllerTest {

    private static final UUID ORGANISATION_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID JOB_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID TARGET_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final Instant NOW = Instant.parse("2026-07-16T06:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ActorContextResolver actorContextResolver;

    @MockitoBean
    private BackgroundJobService backgroundJobService;

    @Test
    void returnsBackgroundJobStatusWithDiagnosticCorrelationId() throws Exception {
        BackgroundJob job = job();
        job.startAttempt(NOW);
        job.fail("IMPORT_DEPENDENCY", "Import dependency unavailable.", NOW);
        when(actorContextResolver.resolve(any())).thenReturn(adminActor());
        when(backgroundJobService.get(adminActor(), JOB_ID)).thenReturn(job);

        mockMvc.perform(get("/api/v1/jobs/{jobId}", JOB_ID))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Correlation-Id", not(blankOrNullString())))
                .andExpect(jsonPath("$.id").value(JOB_ID.toString()))
                .andExpect(jsonPath("$.organisationId").value(ORGANISATION_ID.toString()))
                .andExpect(jsonPath("$.jobType").value("IMPORT_PROCESSING"))
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.targetType").value("IMPORT_BATCH"))
                .andExpect(jsonPath("$.targetId").value(TARGET_ID.toString()))
                .andExpect(jsonPath("$.attemptCount").value(1))
                .andExpect(jsonPath("$.maxAttempts").value(3))
                .andExpect(jsonPath("$.diagnosticCategory").value("IMPORT_DEPENDENCY"))
                .andExpect(jsonPath("$.correlationId").value("corr-123"));
    }

    @Test
    void missingOrCrossOrganisationJobReturnsNoProtectedData() throws Exception {
        when(actorContextResolver.resolve(any())).thenReturn(adminActor());
        when(backgroundJobService.get(adminActor(), JOB_ID)).thenThrow(new ApiException(
                HttpStatus.NOT_FOUND,
                "JOB_NOT_FOUND",
                "Background job was not found."));

        mockMvc.perform(get("/api/v1/jobs/{jobId}", JOB_ID))
                .andExpect(status().isNotFound())
                .andExpect(header().string("X-Correlation-Id", not(blankOrNullString())))
                .andExpect(jsonPath("$.error.code").value("JOB_NOT_FOUND"))
                .andExpect(jsonPath("$.organisationId").doesNotExist())
                .andExpect(jsonPath("$.diagnosticCategory").doesNotExist());
    }

    private BackgroundJob job() {
        return new BackgroundJob(
                JOB_ID,
                ORGANISATION_ID,
                "IMPORT_PROCESSING",
                "IMPORT_BATCH",
                TARGET_ID,
                "import-batch-1",
                USER_ID,
                3,
                "corr-123",
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

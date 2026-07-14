package com.tripledger.identity;

import static org.hamcrest.Matchers.blankOrNullString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tripledger.common.api.ApiException;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CurrentActorController.class)
class CurrentActorControllerTest {

    private static final UUID ORGANISATION_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ActorContextResolver actorContextResolver;

    @Test
    void returnsCurrentActorContextAndCorrelationId() throws Exception {
        when(actorContextResolver.resolve(any())).thenReturn(new ActorContext(
                USER_ID,
                ORGANISATION_ID,
                "Finance User",
                UserRole.FINANCE,
                true,
                "corr-123"));

        mockMvc.perform(get("/api/v1/me"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Correlation-Id", not(blankOrNullString())))
                .andExpect(jsonPath("$.userId").value(USER_ID.toString()))
                .andExpect(jsonPath("$.organisationId").value(ORGANISATION_ID.toString()))
                .andExpect(jsonPath("$.displayName").value("Finance User"))
                .andExpect(jsonPath("$.role").value("FINANCE"))
                .andExpect(jsonPath("$.mfaSatisfied").value(true));
    }

    @Test
    void missingActorReturnsStableErrorShape() throws Exception {
        when(actorContextResolver.resolve(any())).thenThrow(new ApiException(
                HttpStatus.UNAUTHORIZED,
                "AUTHENTICATION_REQUIRED",
                "Authentication is required."));

        mockMvc.perform(get("/api/v1/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("X-Correlation-Id", not(blankOrNullString())))
                .andExpect(jsonPath("$.error.code").value("AUTHENTICATION_REQUIRED"))
                .andExpect(jsonPath("$.error.correlationId").isNotEmpty());
    }

    @Test
    void wrongOrganisationReturnsNoProtectedData() throws Exception {
        when(actorContextResolver.resolve(any())).thenThrow(new ApiException(
                HttpStatus.FORBIDDEN,
                "ORG_REFERENCE_MISMATCH",
                "Requested organisation is not available for this actor."));

        mockMvc.perform(get("/api/v1/me"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("ORG_REFERENCE_MISMATCH"))
                .andExpect(jsonPath("$.userId").doesNotExist())
                .andExpect(jsonPath("$.organisationId").doesNotExist());
    }
}

package com.tripledger.source;

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

@WebMvcTest(SourceSystemController.class)
class SourceSystemControllerTest {

    private static final UUID ORGANISATION_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID SOURCE_SYSTEM_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ActorContextResolver actorContextResolver;

    @MockitoBean
    private SourceSystemService sourceSystemService;

    @Test
    void createsSourceSystem() throws Exception {
        when(actorContextResolver.resolve(any())).thenReturn(adminActor());
        when(sourceSystemService.create(any(), any())).thenReturn(sourceSystem());

        mockMvc.perform(post("/api/v1/source-systems")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "OTA Export",
                                  "category": "BOOKING_CHANNEL",
                                  "externalCode": "OTA_EXPORT",
                                  "timeZone": "Europe/Istanbul",
                                  "active": true
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("X-Correlation-Id", not(blankOrNullString())))
                .andExpect(jsonPath("$.id").value(SOURCE_SYSTEM_ID.toString()))
                .andExpect(jsonPath("$.organisationId").value(ORGANISATION_ID.toString()))
                .andExpect(jsonPath("$.externalCode").value("OTA_EXPORT"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void listsSourceSystemsForCurrentActorOrganisation() throws Exception {
        when(actorContextResolver.resolve(any())).thenReturn(adminActor());
        when(sourceSystemService.list(any())).thenReturn(List.of(sourceSystem()));

        mockMvc.perform(get("/api/v1/source-systems"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Correlation-Id", not(blankOrNullString())))
                .andExpect(jsonPath("$[0].id").value(SOURCE_SYSTEM_ID.toString()))
                .andExpect(jsonPath("$[0].organisationId").value(ORGANISATION_ID.toString()))
                .andExpect(jsonPath("$[0].name").value("OTA Export"));
    }

    @Test
    void duplicateExternalCodeReturnsStableErrorShape() throws Exception {
        when(actorContextResolver.resolve(any())).thenReturn(adminActor());
        when(sourceSystemService.create(any(), any())).thenThrow(new ApiException(
                HttpStatus.CONFLICT,
                "DUPLICATE_SOURCE_CODE",
                "Source system external code already exists."));

        mockMvc.perform(post("/api/v1/source-systems")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "OTA Export",
                                  "category": "BOOKING_CHANNEL",
                                  "externalCode": "OTA_EXPORT",
                                  "timeZone": "Europe/Istanbul",
                                  "active": true
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(header().string("X-Correlation-Id", not(blankOrNullString())))
                .andExpect(jsonPath("$.error.code").value("DUPLICATE_SOURCE_CODE"))
                .andExpect(jsonPath("$.error.correlationId").isNotEmpty());
    }

    @Test
    void invalidRequestReturnsErrorDetailsAndCorrelationId() throws Exception {
        when(actorContextResolver.resolve(any())).thenReturn(adminActor());

        mockMvc.perform(post("/api/v1/source-systems")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "",
                                  "category": "BOOKING_CHANNEL",
                                  "externalCode": "OTA_EXPORT",
                                  "timeZone": "Europe/Istanbul",
                                  "active": true
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("X-Correlation-Id", not(blankOrNullString())))
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.error.details[0].field").value("name"));
    }

    @Test
    void operationsUserCannotCreateSourceSystem() throws Exception {
        when(actorContextResolver.resolve(any())).thenReturn(actor(UserRole.OPERATIONS));
        when(sourceSystemService.create(any(), any())).thenThrow(new ApiException(
                HttpStatus.FORBIDDEN,
                "UNAUTHORISED_FINANCIAL_ACTION",
                "Actor is not authorised for this action."));

        mockMvc.perform(post("/api/v1/source-systems")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "OTA Export",
                                  "category": "BOOKING_CHANNEL",
                                  "externalCode": "OTA_EXPORT",
                                  "timeZone": "Europe/Istanbul",
                                  "active": true
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORISED_FINANCIAL_ACTION"));
    }

    private SourceSystem sourceSystem() {
        return new SourceSystem(
                SOURCE_SYSTEM_ID,
                ORGANISATION_ID,
                "OTA Export",
                SourceSystemCategory.BOOKING_CHANNEL,
                "OTA_EXPORT",
                "Europe/Istanbul",
                true,
                Instant.parse("2026-07-14T06:00:00Z")
        );
    }

    private ActorContext adminActor() {
        return actor(UserRole.ADMINISTRATOR);
    }

    private ActorContext actor(UserRole role) {
        return new ActorContext(
                USER_ID,
                ORGANISATION_ID,
                "Test User",
                role,
                true,
                "corr-123"
        );
    }
}

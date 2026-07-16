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

import com.tripledger.common.api.ApiErrorResponse;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ExchangeRateEvidenceController.class)
class ExchangeRateEvidenceControllerTest {

    private static final UUID ORGANISATION_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID EVENT_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID EVIDENCE_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final Instant NOW = Instant.parse("2026-07-14T06:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ActorContextResolver actorContextResolver;

    @MockitoBean
    private ExchangeRateEvidenceService exchangeRateEvidenceService;

    @Test
    void createsExchangeRateEvidence() throws Exception {
        when(actorContextResolver.resolve(any())).thenReturn(actor());
        when(exchangeRateEvidenceService.create(any(), any())).thenReturn(detail());

        mockMvc.perform(post("/api/v1/exchange-rate-evidence")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "financialEventId": "33333333-3333-3333-3333-333333333333",
                                  "sourceAmount": 3500.00,
                                  "sourceCurrency": "TRY",
                                  "targetCurrency": "EUR",
                                  "rate": 0.0285714286,
                                  "effectiveAt": "2026-07-10T09:00:00Z",
                                  "rateSource": "manual-finance-evidence",
                                  "roundingPolicyVersion": "HALF_UP-v1"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("X-Correlation-Id", not(blankOrNullString())))
                .andExpect(jsonPath("$.id").value(EVIDENCE_ID.toString()))
                .andExpect(jsonPath("$.financialEventId").value(EVENT_ID.toString()))
                .andExpect(jsonPath("$.sourceAmount").value(3500.00))
                .andExpect(jsonPath("$.sourceCurrency").value("TRY"))
                .andExpect(jsonPath("$.targetAmount").value(100.00))
                .andExpect(jsonPath("$.targetCurrency").value("EUR"));
    }

    @Test
    void listsExchangeRateEvidenceForFinancialEvent() throws Exception {
        when(actorContextResolver.resolve(any())).thenReturn(actor());
        when(exchangeRateEvidenceService.list(any(), any())).thenReturn(List.of(detail()));

        mockMvc.perform(get("/api/v1/exchange-rate-evidence?financialEventId={eventId}", EVENT_ID))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Correlation-Id", not(blankOrNullString())))
                .andExpect(jsonPath("$[0].id").value(EVIDENCE_ID.toString()))
                .andExpect(jsonPath("$[0].rateSource").value("manual-finance-evidence"));
    }

    @Test
    void mfaRequiredReturnsStableErrorShape() throws Exception {
        when(actorContextResolver.resolve(any())).thenReturn(actor());
        when(exchangeRateEvidenceService.create(any(), any())).thenThrow(new ApiException(
                HttpStatus.FORBIDDEN,
                "MFA_REQUIRED",
                "MFA is required for this action."));

        mockMvc.perform(post("/api/v1/exchange-rate-evidence")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sourceAmount": 3500.00,
                                  "sourceCurrency": "TRY",
                                  "targetCurrency": "EUR",
                                  "rate": 0.0285714286,
                                  "rateSource": "manual-finance-evidence",
                                  "roundingPolicyVersion": "HALF_UP-v1"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(header().string("X-Correlation-Id", not(blankOrNullString())))
                .andExpect(jsonPath("$.error.code").value("MFA_REQUIRED"))
                .andExpect(jsonPath("$.error.correlationId").isNotEmpty());
    }

    @Test
    void invalidEvidenceReturnsErrorDetailsAndCorrelationId() throws Exception {
        when(actorContextResolver.resolve(any())).thenReturn(actor());
        when(exchangeRateEvidenceService.create(any(), any())).thenThrow(new ApiException(
                HttpStatus.BAD_REQUEST,
                "INVALID_EXCHANGE_RATE",
                "Exchange-rate evidence is invalid.",
                List.of(new ApiErrorResponse.ApiErrorDetail("rate", "Rate must be positive."))));

        mockMvc.perform(post("/api/v1/exchange-rate-evidence")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sourceAmount": 3500.00,
                                  "sourceCurrency": "TRY",
                                  "targetCurrency": "EUR",
                                  "rate": -1,
                                  "rateSource": "manual-finance-evidence",
                                  "roundingPolicyVersion": "HALF_UP-v1"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("X-Correlation-Id", not(blankOrNullString())))
                .andExpect(jsonPath("$.error.code").value("INVALID_EXCHANGE_RATE"))
                .andExpect(jsonPath("$.error.details[0].field").value("rate"));
    }

    private ExchangeRateEvidenceDetail detail() {
        return new ExchangeRateEvidenceDetail(
                EVIDENCE_ID,
                ORGANISATION_ID,
                EVENT_ID,
                new BigDecimal("3500.00"),
                "TRY",
                new BigDecimal("100.00"),
                "EUR",
                new BigDecimal("0.028571428600"),
                Instant.parse("2026-07-10T09:00:00Z"),
                "manual-finance-evidence",
                "HALF_UP-v1",
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

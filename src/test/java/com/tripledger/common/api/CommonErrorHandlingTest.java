package com.tripledger.common.api;

import static org.hamcrest.Matchers.blankOrNullString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = CommonErrorTestController.class)
@Import({CorrelationIdFilter.class, GlobalExceptionHandler.class})
class CommonErrorHandlingTest {

    private static final String ENDPOINT = "/api/v1/common-error-test";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void malformedJsonReturnsStableErrorShapeAndClientCorrelationId() throws Exception {
        mockMvc.perform(post(ENDPOINT)
                        .header(CorrelationIdFilter.CORRELATION_ID_HEADER, "client-corr-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(header().string(CorrelationIdFilter.CORRELATION_ID_HEADER, "client-corr-1"))
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.error.correlationId").value("client-corr-1"))
                .andExpect(jsonPath("$.error.details[0].field").value("body"));
    }

    @Test
    void validationFailureReturnsStableErrorShapeAndGeneratedCorrelationId() throws Exception {
        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(header().string(CorrelationIdFilter.CORRELATION_ID_HEADER, not(blankOrNullString())))
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.error.correlationId").isNotEmpty())
                .andExpect(jsonPath("$.error.details[0].field").value("name"));
    }

    @Test
    void unsupportedMethodReturnsStableErrorShape() throws Exception {
        mockMvc.perform(put(ENDPOINT))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().string(CorrelationIdFilter.CORRELATION_ID_HEADER, not(blankOrNullString())))
                .andExpect(jsonPath("$.error.code").value("METHOD_NOT_ALLOWED"))
                .andExpect(jsonPath("$.error.correlationId").isNotEmpty());
    }

    @Test
    void unsupportedMediaTypeReturnsStableErrorShape() throws Exception {
        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("plain text"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(header().string(CorrelationIdFilter.CORRELATION_ID_HEADER, not(blankOrNullString())))
                .andExpect(jsonPath("$.error.code").value("UNSUPPORTED_MEDIA_TYPE"))
                .andExpect(jsonPath("$.error.correlationId").isNotEmpty());
    }

    @Test
    void unexpectedExceptionReturnsSafeErrorShape() throws Exception {
        mockMvc.perform(get(ENDPOINT + "/failure"))
                .andExpect(status().isInternalServerError())
                .andExpect(header().string(CorrelationIdFilter.CORRELATION_ID_HEADER, not(blankOrNullString())))
                .andExpect(jsonPath("$.error.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.error.message")
                        .value("Unexpected error. Use the correlation id when contacting support."))
                .andExpect(jsonPath("$.error.correlationId").isNotEmpty());
    }

    @Test
    void unknownEndpointReturnsStableErrorShape() throws Exception {
        mockMvc.perform(get("/api/v1/missing-endpoint"))
                .andExpect(status().isNotFound())
                .andExpect(header().string(CorrelationIdFilter.CORRELATION_ID_HEADER, not(blankOrNullString())))
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.error.correlationId").isNotEmpty());
    }
}

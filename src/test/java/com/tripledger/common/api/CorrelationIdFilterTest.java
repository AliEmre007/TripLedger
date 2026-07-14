package com.tripledger.common.api;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class CorrelationIdFilterTest {

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @Test
    void keepsSafeClientCorrelationId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader(CorrelationIdFilter.CORRELATION_ID_HEADER, "  client-123:abc.def  ");

        filter.doFilter(request, response, assertCorrelationIdInsideChain("client-123:abc.def"));

        assertThat(request.getAttribute(CorrelationIdFilter.CORRELATION_ID_ATTRIBUTE))
                .isEqualTo("client-123:abc.def");
        assertThat(response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER))
                .isEqualTo("client-123:abc.def");
        assertThat(MDC.get(CorrelationIdFilter.CORRELATION_ID_ATTRIBUTE)).isNull();
    }

    @Test
    void generatesCorrelationIdWhenHeaderIsMissing() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, assertGeneratedCorrelationIdInsideChain());

        assertThat(response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER))
                .isNotBlank()
                .isEqualTo(request.getAttribute(CorrelationIdFilter.CORRELATION_ID_ATTRIBUTE));
        assertThat(MDC.get(CorrelationIdFilter.CORRELATION_ID_ATTRIBUTE)).isNull();
    }

    @Test
    void replacesUnsafeClientCorrelationId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader(CorrelationIdFilter.CORRELATION_ID_HEADER, "unsafe value");

        filter.doFilter(request, response, assertGeneratedCorrelationIdInsideChain());

        assertThat(response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER))
                .isNotBlank()
                .isNotEqualTo("unsafe value");
    }

    @Test
    void replacesOverlongClientCorrelationId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader(CorrelationIdFilter.CORRELATION_ID_HEADER, "a".repeat(129));

        filter.doFilter(request, response, assertGeneratedCorrelationIdInsideChain());

        assertThat(response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER))
                .isNotBlank()
                .isNotEqualTo("a".repeat(129));
    }

    private FilterChain assertCorrelationIdInsideChain(String expectedCorrelationId) {
        return (request, response) -> {
            assertThat(request.getAttribute(CorrelationIdFilter.CORRELATION_ID_ATTRIBUTE))
                    .isEqualTo(expectedCorrelationId);
            assertThat(MDC.get(CorrelationIdFilter.CORRELATION_ID_ATTRIBUTE))
                    .isEqualTo(expectedCorrelationId);
        };
    }

    private FilterChain assertGeneratedCorrelationIdInsideChain() {
        return (request, response) -> {
            Object correlationId = request.getAttribute(CorrelationIdFilter.CORRELATION_ID_ATTRIBUTE);
            assertThat(correlationId).isInstanceOf(String.class);
            assertThat((String) correlationId).isNotBlank();
            assertThat(MDC.get(CorrelationIdFilter.CORRELATION_ID_ATTRIBUTE))
                    .isEqualTo(correlationId);
        };
    }
}

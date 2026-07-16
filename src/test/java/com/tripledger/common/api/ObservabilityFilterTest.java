package com.tripledger.common.api;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.HandlerMapping;

class ObservabilityFilterTest {

    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    private final ObservabilityFilter filter = new ObservabilityFilter(meterRegistryProvider());

    @Test
    void recordsRequestTimerWithRouteAndStatusTags() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/health/live");
        request.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, "/api/v1/health/live");
        request.setAttribute(CorrelationIdFilter.CORRELATION_ID_ATTRIBUTE, "corr-123");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, okChain());

        assertThat(meterRegistry.find("tripledger.http.requests")
                .tag("method", "GET")
                .tag("route", "/api/v1/health/live")
                .tag("status", "200")
                .tag("outcome", "SUCCESS")
                .timer()).isNotNull();
    }

    @Test
    void recordsErrorCounterByStableErrorCode() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/jobs/missing");
        request.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, "/api/v1/jobs/{jobId}");
        request.setAttribute(CorrelationIdFilter.CORRELATION_ID_ATTRIBUTE, "corr-123");
        request.setAttribute(ObservabilityFilter.ERROR_CODE_ATTRIBUTE, "JOB_NOT_FOUND");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, notFoundChain());

        assertThat(meterRegistry.counter(
                "tripledger.http.errors",
                "code",
                "JOB_NOT_FOUND",
                "status",
                "404",
                "route",
                "/api/v1/jobs/{jobId}").count()).isEqualTo(1.0);
    }

    private FilterChain okChain() {
        return (request, response) -> ((MockHttpServletResponse) response).setStatus(200);
    }

    private FilterChain notFoundChain() {
        return (request, response) -> ((MockHttpServletResponse) response).setStatus(404);
    }

    private org.springframework.beans.factory.ObjectProvider<io.micrometer.core.instrument.MeterRegistry> meterRegistryProvider() {
        StaticListableBeanFactory beanFactory = new StaticListableBeanFactory();
        beanFactory.addBean("meterRegistry", meterRegistry);
        return beanFactory.getBeanProvider(io.micrometer.core.instrument.MeterRegistry.class);
    }
}

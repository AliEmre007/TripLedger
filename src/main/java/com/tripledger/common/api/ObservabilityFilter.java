package com.tripledger.common.api;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerMapping;

@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class ObservabilityFilter extends OncePerRequestFilter {

    public static final String ERROR_CODE_ATTRIBUTE = "tripledger.errorCode";

    private static final Logger LOGGER = LoggerFactory.getLogger(ObservabilityFilter.class);
    private static final String UNKNOWN_ERROR_CODE = "NONE";

    private final MeterRegistry meterRegistry;

    public ObservabilityFilter(ObjectProvider<MeterRegistry> meterRegistry) {
        this.meterRegistry = meterRegistry.getIfAvailable(CompositeMeterRegistry::new);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        Instant startedAt = Instant.now();
        try {
            filterChain.doFilter(request, response);
        } finally {
            long durationMillis = Duration.between(startedAt, Instant.now()).toMillis();
            String route = route(request);
            String status = Integer.toString(response.getStatus());
            String outcome = response.getStatus() >= 500 ? "ERROR" : response.getStatus() >= 400 ? "CLIENT_ERROR" : "SUCCESS";
            String errorCode = errorCode(request);

            Timer.builder("tripledger.http.requests")
                    .tag("method", request.getMethod())
                    .tag("route", route)
                    .tag("status", status)
                    .tag("outcome", outcome)
                    .register(meterRegistry)
                    .record(Duration.ofMillis(durationMillis));

            if (!UNKNOWN_ERROR_CODE.equals(errorCode)) {
                meterRegistry.counter(
                        "tripledger.http.errors",
                        "code",
                        errorCode,
                        "status",
                        status,
                        "route",
                        route
                ).increment();
            }

            LOGGER.info(
                    "http_request method={} route={} status={} outcome={} durationMs={} errorCode={} correlationId={}",
                    request.getMethod(),
                    route,
                    status,
                    outcome,
                    durationMillis,
                    errorCode,
                    request.getAttribute(CorrelationIdFilter.CORRELATION_ID_ATTRIBUTE)
            );
        }
    }

    private String route(HttpServletRequest request) {
        Object pattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        if (pattern instanceof String routePattern) {
            return routePattern;
        }
        return request.getRequestURI();
    }

    private String errorCode(HttpServletRequest request) {
        Object code = request.getAttribute(ERROR_CODE_ATTRIBUTE);
        if (code instanceof String value) {
            return value;
        }
        return UNKNOWN_ERROR_CODE;
    }
}

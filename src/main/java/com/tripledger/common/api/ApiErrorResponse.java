package com.tripledger.common.api;

import java.time.Instant;
import java.util.List;

public record ApiErrorResponse(
        ApiError error
) {

    public static ApiErrorResponse of(String code, String message, String correlationId, List<ApiErrorDetail> details) {
        return new ApiErrorResponse(new ApiError(code, message, correlationId, Instant.now(), details));
    }

    public record ApiError(
            String code,
            String message,
            String correlationId,
            Instant timestamp,
            List<ApiErrorDetail> details
    ) {
    }

    public record ApiErrorDetail(
            String field,
            String reason
    ) {
    }
}

package com.tripledger.identity;

import java.util.UUID;

public record ActorContext(
        UUID userId,
        UUID organisationId,
        String displayName,
        UserRole role,
        boolean mfaSatisfied,
        String correlationId
) {
}

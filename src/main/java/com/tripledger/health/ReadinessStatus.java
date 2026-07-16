package com.tripledger.health;

import java.util.List;

public record ReadinessStatus(
        String status,
        List<ReadinessCheck> checks
) {

    public record ReadinessCheck(
            String name,
            String status,
            String detail
    ) {
    }
}

package com.tripledger.health;

import java.time.Instant;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/health")
public class HealthController {

    private final BuildProperties buildProperties;

    public HealthController(ObjectProvider<BuildProperties> buildProperties) {
        this.buildProperties = buildProperties.getIfAvailable();
    }

    @GetMapping
    public ResponseEntity<HealthResponse> health() {
        return ResponseEntity.ok(new HealthResponse(
                "UP",
                "tripledger",
                buildProperties == null ? "local" : buildProperties.getVersion(),
                Instant.now()
        ));
    }

    public record HealthResponse(
            String status,
            String service,
            String version,
            Instant timestamp
    ) {
    }
}

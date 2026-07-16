package com.tripledger.health;

import java.time.Instant;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/health")
public class HealthController {

    private final BuildProperties buildProperties;
    private final ReadinessService readinessService;

    public HealthController(ObjectProvider<BuildProperties> buildProperties,
                            ReadinessService readinessService) {
        this.buildProperties = buildProperties.getIfAvailable();
        this.readinessService = readinessService;
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

    @GetMapping("/live")
    public ResponseEntity<HealthResponse> live() {
        return health();
    }

    @GetMapping("/ready")
    public ResponseEntity<ReadinessResponse> ready() {
        ReadinessStatus readiness = readinessService.readiness();
        HttpStatus status = "UP".equals(readiness.status()) ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;
        return ResponseEntity.status(status).body(new ReadinessResponse(
                readiness.status(),
                "tripledger",
                buildProperties == null ? "local" : buildProperties.getVersion(),
                Instant.now(),
                readiness.checks()
        ));
    }

    public record HealthResponse(
            String status,
            String service,
            String version,
            Instant timestamp
    ) {
    }

    public record ReadinessResponse(
            String status,
            String service,
            String version,
            Instant timestamp,
            java.util.List<ReadinessStatus.ReadinessCheck> checks
    ) {
    }
}

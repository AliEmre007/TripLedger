package com.tripledger.operations;

import com.tripledger.identity.ActorContext;
import com.tripledger.identity.ActorContextResolver;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/jobs")
public class BackgroundJobController {

    private final ActorContextResolver actorContextResolver;
    private final BackgroundJobService backgroundJobService;

    public BackgroundJobController(ActorContextResolver actorContextResolver,
                                   BackgroundJobService backgroundJobService) {
        this.actorContextResolver = actorContextResolver;
        this.backgroundJobService = backgroundJobService;
    }

    @GetMapping("/{jobId}")
    public BackgroundJobResponse get(@PathVariable UUID jobId, HttpServletRequest servletRequest) {
        ActorContext actor = actorContextResolver.resolve(servletRequest);
        return BackgroundJobResponse.from(backgroundJobService.get(actor, jobId));
    }

    public record BackgroundJobResponse(
            UUID id,
            UUID organisationId,
            String jobType,
            BackgroundJobStatus status,
            String targetType,
            UUID targetId,
            UUID requestedByUserId,
            int maxAttempts,
            int attemptCount,
            String diagnosticCategory,
            String diagnosticMessage,
            String correlationId,
            Instant requestedAt,
            Instant lastAttemptAt,
            Instant nextAttemptAt,
            Instant completedAt
    ) {

        static BackgroundJobResponse from(BackgroundJob job) {
            return new BackgroundJobResponse(
                    job.id(),
                    job.organisationId(),
                    job.jobType(),
                    job.status(),
                    job.targetType(),
                    job.targetId(),
                    job.requestedByUserId(),
                    job.maxAttempts(),
                    job.attemptCount(),
                    job.diagnosticCategory(),
                    job.diagnosticMessage(),
                    job.correlationId(),
                    job.requestedAt(),
                    job.lastAttemptAt(),
                    job.nextAttemptAt(),
                    job.completedAt()
            );
        }
    }
}

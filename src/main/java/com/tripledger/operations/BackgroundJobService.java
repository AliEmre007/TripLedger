package com.tripledger.operations;

import com.tripledger.authorization.AuthorizationService;
import com.tripledger.authorization.Permission;
import com.tripledger.common.api.ApiErrorResponse;
import com.tripledger.common.api.ApiException;
import com.tripledger.identity.ActorContext;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class BackgroundJobService {

    private static final int VALIDATION_RELEASE_MAX_ATTEMPTS = 3;

    private final BackgroundJobRepository backgroundJobRepository;
    private final AuthorizationService authorizationService;
    private final MeterRegistry meterRegistry;
    private final Clock clock;

    public BackgroundJobService(BackgroundJobRepository backgroundJobRepository,
                                AuthorizationService authorizationService,
                                MeterRegistry meterRegistry,
                                Clock clock) {
        this.backgroundJobRepository = backgroundJobRepository;
        this.authorizationService = authorizationService;
        this.meterRegistry = meterRegistry;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public BackgroundJob get(ActorContext actor, UUID jobId) {
        authorizationService.require(actor, Permission.PROTECTED_READ);
        return backgroundJobRepository.findByOrganisationIdAndId(actor.organisationId(), jobId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "JOB_NOT_FOUND",
                        "Background job was not found."));
    }

    @Transactional
    public BackgroundJob runWithRetry(ActorContext actor, StartBackgroundJobCommand command, JobWork work) {
        authorizationService.require(actor, Permission.OPERATIONAL_WRITE);
        validate(command);

        BackgroundJob job = backgroundJobRepository
                .findByOrganisationIdAndJobTypeAndIdempotencyKey(
                        actor.organisationId(),
                        command.jobType().trim(),
                        command.idempotencyKey().trim())
                .orElseGet(() -> create(actor, command));

        if (job.terminal()) {
            return job;
        }

        while (job.attemptCount() < job.maxAttempts()) {
            job.startAttempt(Instant.now(clock));
            backgroundJobRepository.save(job);

            try {
                work.run();
                job.succeed(Instant.now(clock));
                return backgroundJobRepository.save(job);
            } catch (TransientJobException exception) {
                String diagnosticCategory = safeCategory(exception);
                if (job.attemptCount() >= job.maxAttempts()) {
                    job.fail(diagnosticCategory, safeMessage(exception), Instant.now(clock));
                    return backgroundJobRepository.save(job);
                }
                job.scheduleRetry(diagnosticCategory, safeMessage(exception), Instant.now(clock));
                recordRetry(job, diagnosticCategory);
                backgroundJobRepository.save(job);
            } catch (RuntimeException exception) {
                job.fail("UNEXPECTED_ERROR", "Unexpected job failure.", Instant.now(clock));
                return backgroundJobRepository.save(job);
            }
        }

        job.fail("RETRY_EXHAUSTED", "Retry attempts were exhausted.", Instant.now(clock));
        return backgroundJobRepository.save(job);
    }

    private BackgroundJob create(ActorContext actor, StartBackgroundJobCommand command) {
        return backgroundJobRepository.save(new BackgroundJob(
                UUID.randomUUID(),
                actor.organisationId(),
                command.jobType().trim(),
                normalizeNullable(command.targetType()),
                command.targetId(),
                command.idempotencyKey().trim(),
                actor.userId(),
                command.maxAttempts(),
                actor.correlationId(),
                Instant.now(clock)
        ));
    }

    private void validate(StartBackgroundJobCommand command) {
        if (!StringUtils.hasText(command.jobType())) {
            throw invalidField("jobType", "Job type is required.");
        }
        if (!StringUtils.hasText(command.idempotencyKey())) {
            throw invalidField("idempotencyKey", "Idempotency key is required.");
        }
        if (command.maxAttempts() < 1 || command.maxAttempts() > VALIDATION_RELEASE_MAX_ATTEMPTS) {
            throw invalidField("maxAttempts", "Max attempts must be between 1 and 3.");
        }
    }

    private void recordRetry(BackgroundJob job, String diagnosticCategory) {
        meterRegistry.counter(
                "tripledger.job.retries",
                "job.type",
                job.jobType(),
                "diagnostic.category",
                diagnosticCategory
        ).increment();
    }

    private ApiException invalidField(String field, String reason) {
        return new ApiException(
                HttpStatus.BAD_REQUEST,
                "INVALID_REQUEST",
                "Request validation failed.",
                List.of(new ApiErrorResponse.ApiErrorDetail(field, reason))
        );
    }

    private String normalizeNullable(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String safeCategory(TransientJobException exception) {
        if (!StringUtils.hasText(exception.diagnosticCategory())) {
            return "TRANSIENT_DEPENDENCY";
        }
        return exception.diagnosticCategory().trim();
    }

    private String safeMessage(RuntimeException exception) {
        if (!StringUtils.hasText(exception.getMessage())) {
            return "Transient job dependency failed.";
        }
        return exception.getMessage();
    }

    public record StartBackgroundJobCommand(
            String jobType,
            String targetType,
            UUID targetId,
            String idempotencyKey,
            int maxAttempts
    ) {
    }

    @FunctionalInterface
    public interface JobWork {

        void run();
    }
}

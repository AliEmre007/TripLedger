package com.tripledger.operations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tripledger.authorization.AuthorizationService;
import com.tripledger.authorization.Permission;
import com.tripledger.common.api.ApiException;
import com.tripledger.identity.ActorContext;
import com.tripledger.identity.UserRole;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class BackgroundJobServiceTest {

    private static final UUID ORGANISATION_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID JOB_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID TARGET_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final Instant NOW = Instant.parse("2026-07-16T06:00:00Z");

    @Mock
    private BackgroundJobRepository backgroundJobRepository;

    @Mock
    private AuthorizationService authorizationService;

    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();

    @Test
    void retriesTransientFailureAndCompletesOnceOnThirdAttempt() {
        when(backgroundJobRepository.findByOrganisationIdAndJobTypeAndIdempotencyKey(
                ORGANISATION_ID,
                "IMPORT_PROCESSING",
                "import-batch-1")).thenReturn(Optional.empty());
        when(backgroundJobRepository.save(any(BackgroundJob.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        AtomicInteger attempts = new AtomicInteger();
        AtomicInteger acceptedEffects = new AtomicInteger();

        BackgroundJob job = service().runWithRetry(adminActor(), validCommand(), () -> {
            if (attempts.incrementAndGet() < 3) {
                throw new TransientJobException("IMPORT_DEPENDENCY", "Import dependency unavailable.");
            }
            acceptedEffects.incrementAndGet();
        });

        assertThat(job.status()).isEqualTo(BackgroundJobStatus.SUCCEEDED);
        assertThat(job.attemptCount()).isEqualTo(3);
        assertThat(job.completedAt()).isEqualTo(NOW);
        assertThat(acceptedEffects).hasValue(1);
        assertThat(meterRegistry.counter(
                "tripledger.job.retries",
                "job.type",
                "IMPORT_PROCESSING",
                "diagnostic.category",
                "IMPORT_DEPENDENCY").count()).isEqualTo(2.0);
    }

    @Test
    void persistentTransientFailureEndsInFinalFailedStateWithDiagnosticEvidence() {
        when(backgroundJobRepository.findByOrganisationIdAndJobTypeAndIdempotencyKey(
                ORGANISATION_ID,
                "IMPORT_PROCESSING",
                "import-batch-1")).thenReturn(Optional.empty());
        when(backgroundJobRepository.save(any(BackgroundJob.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        BackgroundJob job = service().runWithRetry(adminActor(), validCommand(), () -> {
            throw new TransientJobException("IMPORT_DEPENDENCY", "Import dependency unavailable.");
        });

        assertThat(job.status()).isEqualTo(BackgroundJobStatus.FAILED);
        assertThat(job.attemptCount()).isEqualTo(3);
        assertThat(job.diagnosticCategory()).isEqualTo("IMPORT_DEPENDENCY");
        assertThat(job.diagnosticMessage()).isEqualTo("Import dependency unavailable.");
        assertThat(job.correlationId()).isEqualTo("corr-123");
        assertThat(job.completedAt()).isEqualTo(NOW);
        assertThat(meterRegistry.counter(
                "tripledger.job.retries",
                "job.type",
                "IMPORT_PROCESSING",
                "diagnostic.category",
                "IMPORT_DEPENDENCY").count()).isEqualTo(2.0);
    }

    @Test
    void doesNotRerunTerminalIdempotentJob() {
        BackgroundJob existing = job();
        existing.startAttempt(NOW);
        existing.succeed(NOW);
        when(backgroundJobRepository.findByOrganisationIdAndJobTypeAndIdempotencyKey(
                ORGANISATION_ID,
                "IMPORT_PROCESSING",
                "import-batch-1")).thenReturn(Optional.of(existing));
        AtomicInteger effects = new AtomicInteger();

        BackgroundJob job = service().runWithRetry(adminActor(), validCommand(), effects::incrementAndGet);

        assertThat(job).isSameAs(existing);
        assertThat(effects).hasValue(0);
        verify(backgroundJobRepository, never()).save(any(BackgroundJob.class));
    }

    @Test
    void rejectsMoreThanThreeAttempts() {
        assertThatThrownBy(() -> service().runWithRetry(
                adminActor(),
                new BackgroundJobService.StartBackgroundJobCommand(
                        "IMPORT_PROCESSING",
                        "IMPORT_BATCH",
                        TARGET_ID,
                        "import-batch-1",
                        4),
                () -> {
                }
        )).isInstanceOfSatisfying(ApiException.class, exception -> {
            assertThat(exception.status()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(exception.code()).isEqualTo("INVALID_REQUEST");
            assertThat(exception.details().getFirst().field()).isEqualTo("maxAttempts");
        });
    }

    @Test
    void getsJobInsideActorOrganisation() {
        BackgroundJob existing = job();
        when(backgroundJobRepository.findByOrganisationIdAndId(ORGANISATION_ID, JOB_ID))
                .thenReturn(Optional.of(existing));

        BackgroundJob result = service().get(adminActor(), JOB_ID);

        assertThat(result).isSameAs(existing);
        verify(authorizationService).require(adminActor(), Permission.PROTECTED_READ);
    }

    private BackgroundJobService service() {
        return new BackgroundJobService(
                backgroundJobRepository,
                authorizationService,
                meterRegistry,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    private BackgroundJobService.StartBackgroundJobCommand validCommand() {
        return new BackgroundJobService.StartBackgroundJobCommand(
                " IMPORT_PROCESSING ",
                " IMPORT_BATCH ",
                TARGET_ID,
                " import-batch-1 ",
                3
        );
    }

    private BackgroundJob job() {
        return new BackgroundJob(
                JOB_ID,
                ORGANISATION_ID,
                "IMPORT_PROCESSING",
                "IMPORT_BATCH",
                TARGET_ID,
                "import-batch-1",
                USER_ID,
                3,
                "corr-123",
                NOW
        );
    }

    private ActorContext adminActor() {
        return new ActorContext(
                USER_ID,
                ORGANISATION_ID,
                "Test User",
                UserRole.ADMINISTRATOR,
                true,
                "corr-123"
        );
    }
}

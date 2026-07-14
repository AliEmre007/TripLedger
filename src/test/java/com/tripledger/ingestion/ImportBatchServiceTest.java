package com.tripledger.ingestion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tripledger.authorization.AuthorizationService;
import com.tripledger.authorization.Permission;
import com.tripledger.common.api.ApiException;
import com.tripledger.identity.ActorContext;
import com.tripledger.identity.UserRole;
import com.tripledger.source.SourceSystem;
import com.tripledger.source.SourceSystemCategory;
import com.tripledger.source.SourceSystemRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class ImportBatchServiceTest {

    private static final UUID ORGANISATION_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID SOURCE_SYSTEM_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID BATCH_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final Instant NOW = Instant.parse("2026-07-14T06:00:00Z");

    @Mock
    private ImportBatchRepository importBatchRepository;

    @Mock
    private ImportRowResultRepository importRowResultRepository;

    @Mock
    private SourceSystemRepository sourceSystemRepository;

    @Mock
    private AuthorizationService authorizationService;

    @Test
    void startsBatchForActiveSourceSystemInsideActorOrganisation() {
        when(sourceSystemRepository.findByOrganisationIdAndId(ORGANISATION_ID, SOURCE_SYSTEM_ID))
                .thenReturn(Optional.of(sourceSystem(true)));
        when(importBatchRepository.save(any(ImportBatch.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ImportBatch batch = service().start(adminActor(), new ImportBatchService.StartImportBatchCommand(
                SOURCE_SYSTEM_ID,
                " booking_csv ",
                " v1 ",
                " bookings.csv ",
                " sha256:abc "
        ));

        assertThat(batch.organisationId()).isEqualTo(ORGANISATION_ID);
        assertThat(batch.sourceSystemId()).isEqualTo(SOURCE_SYSTEM_ID);
        assertThat(batch.templateType()).isEqualTo("booking_csv");
        assertThat(batch.templateVersion()).isEqualTo("v1");
        assertThat(batch.fileName()).isEqualTo("bookings.csv");
        assertThat(batch.fileChecksum()).isEqualTo("sha256:abc");
        assertThat(batch.status()).isEqualTo(ImportBatchStatus.RECEIVED);
        assertThat(batch.receivedByUserId()).isEqualTo(USER_ID);
        assertThat(batch.receivedAt()).isEqualTo(NOW);
        assertThat(batch.totalCount()).isZero();
        verify(authorizationService).require(adminActor(), Permission.OPERATIONAL_WRITE);
    }

    @Test
    void rejectsInactiveSourceSystem() {
        when(sourceSystemRepository.findByOrganisationIdAndId(ORGANISATION_ID, SOURCE_SYSTEM_ID))
                .thenReturn(Optional.of(sourceSystem(false)));

        assertThatThrownBy(() -> service().start(adminActor(), validStartCommand()))
                .isInstanceOfSatisfying(ApiException.class, exception -> {
                    assertThat(exception.status()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(exception.code()).isEqualTo("INACTIVE_SOURCE_SYSTEM");
                });
    }

    @Test
    void recordsRowResultAndUpdatesBatchCounts() {
        ImportBatch batch = batch(ImportBatchStatus.RECEIVED);
        when(importBatchRepository.findByOrganisationIdAndId(ORGANISATION_ID, BATCH_ID))
                .thenReturn(Optional.of(batch));
        when(importRowResultRepository.existsByOrganisationIdAndImportBatchIdAndRowNumber(
                ORGANISATION_ID,
                BATCH_ID,
                7)).thenReturn(false);
        when(importRowResultRepository.save(any(ImportRowResult.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ImportRowResult rowResult = service().recordRowResult(
                adminActor(),
                BATCH_ID,
                new ImportBatchService.RecordRowResultCommand(
                        7,
                        ImportRowOutcome.REJECTED,
                        " sellingAmount ",
                        " INVALID_AMOUNT ",
                        " Amount is required. ",
                        null
                )
        );

        assertThat(rowResult.organisationId()).isEqualTo(ORGANISATION_ID);
        assertThat(rowResult.importBatchId()).isEqualTo(BATCH_ID);
        assertThat(rowResult.rowNumber()).isEqualTo(7);
        assertThat(rowResult.outcome()).isEqualTo(ImportRowOutcome.REJECTED);
        assertThat(rowResult.fieldName()).isEqualTo("sellingAmount");
        assertThat(rowResult.errorCode()).isEqualTo("INVALID_AMOUNT");
        assertThat(rowResult.reason()).isEqualTo("Amount is required.");
        assertThat(rowResult.recordedAt()).isEqualTo(NOW);
        assertThat(batch.totalCount()).isEqualTo(1);
        assertThat(batch.rejectedCount()).isEqualTo(1);
    }

    @Test
    void rejectsDuplicateRowNumberInsideBatch() {
        when(importBatchRepository.findByOrganisationIdAndId(ORGANISATION_ID, BATCH_ID))
                .thenReturn(Optional.of(batch(ImportBatchStatus.RECEIVED)));
        when(importRowResultRepository.existsByOrganisationIdAndImportBatchIdAndRowNumber(
                ORGANISATION_ID,
                BATCH_ID,
                1)).thenReturn(true);

        assertThatThrownBy(() -> service().recordRowResult(adminActor(), BATCH_ID, validRowCommand()))
                .isInstanceOfSatisfying(ApiException.class, exception -> {
                    assertThat(exception.status()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(exception.code()).isEqualTo("DUPLICATE_IMPORT_ROW_RESULT");
                });
    }

    @Test
    void rejectsRowResultWithoutErrorEvidenceForRejectedRows() {
        assertThatThrownBy(() -> service().recordRowResult(
                adminActor(),
                BATCH_ID,
                new ImportBatchService.RecordRowResultCommand(1, ImportRowOutcome.REJECTED, "amount", "", "", null)
        )).isInstanceOfSatisfying(ApiException.class, exception -> {
            assertThat(exception.status()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(exception.code()).isEqualTo("INVALID_REQUEST");
            assertThat(exception.details().getFirst().field()).isEqualTo("errorCode");
        });
    }

    @Test
    void terminalBatchCannotReceiveMoreRows() {
        when(importBatchRepository.findByOrganisationIdAndId(ORGANISATION_ID, BATCH_ID))
                .thenReturn(Optional.of(batch(ImportBatchStatus.COMPLETED)));

        assertThatThrownBy(() -> service().recordRowResult(adminActor(), BATCH_ID, validRowCommand()))
                .isInstanceOfSatisfying(ApiException.class, exception -> {
                    assertThat(exception.status()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(exception.code()).isEqualTo("IMPORT_BATCH_TERMINAL");
                });
    }

    @Test
    void completesOpenBatch() {
        ImportBatch batch = batch(ImportBatchStatus.RECEIVED);
        when(importBatchRepository.findByOrganisationIdAndId(ORGANISATION_ID, BATCH_ID))
                .thenReturn(Optional.of(batch));

        ImportBatch completed = service().complete(adminActor(), BATCH_ID);

        assertThat(completed.status()).isEqualTo(ImportBatchStatus.COMPLETED);
        assertThat(completed.completedAt()).isEqualTo(NOW);
    }

    @Test
    void failsOpenBatchWithSafeReason() {
        ImportBatch batch = batch(ImportBatchStatus.RECEIVED);
        when(importBatchRepository.findByOrganisationIdAndId(ORGANISATION_ID, BATCH_ID))
                .thenReturn(Optional.of(batch));

        ImportBatch failed = service().fail(
                adminActor(),
                BATCH_ID,
                new ImportBatchService.FailImportBatchCommand(" UNSUPPORTED_TEMPLATE_VERSION ", " Unsupported v9. ")
        );

        assertThat(failed.status()).isEqualTo(ImportBatchStatus.FAILED);
        assertThat(failed.failureCode()).isEqualTo("UNSUPPORTED_TEMPLATE_VERSION");
        assertThat(failed.failureReason()).isEqualTo("Unsupported v9.");
        assertThat(failed.completedAt()).isEqualTo(NOW);
    }

    @Test
    void listsBatchesForActorOrganisation() {
        ImportBatch batch = batch(ImportBatchStatus.RECEIVED);
        when(importBatchRepository.findByOrganisationIdOrderByReceivedAtDesc(ORGANISATION_ID))
                .thenReturn(List.of(batch));

        List<ImportBatch> results = service().list(adminActor());

        assertThat(results).containsExactly(batch);
        verify(authorizationService).require(adminActor(), Permission.PROTECTED_READ);
    }

    private ImportBatchService service() {
        return new ImportBatchService(
                importBatchRepository,
                importRowResultRepository,
                sourceSystemRepository,
                authorizationService,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    private ImportBatchService.StartImportBatchCommand validStartCommand() {
        return new ImportBatchService.StartImportBatchCommand(
                SOURCE_SYSTEM_ID,
                "BOOKING_CSV",
                "v1",
                "bookings.csv",
                "sha256:abc"
        );
    }

    private ImportBatchService.RecordRowResultCommand validRowCommand() {
        return new ImportBatchService.RecordRowResultCommand(
                1,
                ImportRowOutcome.ACCEPTED,
                null,
                null,
                null,
                null
        );
    }

    private ImportBatch batch(ImportBatchStatus status) {
        ImportBatch batch = new ImportBatch(
                BATCH_ID,
                ORGANISATION_ID,
                SOURCE_SYSTEM_ID,
                "BOOKING_CSV",
                "v1",
                ImportBatchStatus.RECEIVED,
                "bookings.csv",
                "sha256:abc",
                USER_ID,
                NOW
        );
        if (status == ImportBatchStatus.COMPLETED) {
            batch.complete(NOW);
        }
        if (status == ImportBatchStatus.FAILED) {
            batch.fail("UNSUPPORTED_TEMPLATE_VERSION", "Unsupported v9.", NOW);
        }
        return batch;
    }

    private SourceSystem sourceSystem(boolean active) {
        return new SourceSystem(
                SOURCE_SYSTEM_ID,
                ORGANISATION_ID,
                "OTA Export",
                SourceSystemCategory.BOOKING_CHANNEL,
                "OTA_EXPORT",
                "Europe/Istanbul",
                active,
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

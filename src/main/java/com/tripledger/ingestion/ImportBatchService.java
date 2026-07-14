package com.tripledger.ingestion;

import com.tripledger.authorization.AuthorizationService;
import com.tripledger.authorization.Permission;
import com.tripledger.common.api.ApiErrorResponse;
import com.tripledger.common.api.ApiException;
import com.tripledger.identity.ActorContext;
import com.tripledger.source.SourceSystem;
import com.tripledger.source.SourceSystemRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ImportBatchService {

    private final ImportBatchRepository importBatchRepository;
    private final ImportRowResultRepository importRowResultRepository;
    private final SourceSystemRepository sourceSystemRepository;
    private final AuthorizationService authorizationService;
    private final Clock clock;

    public ImportBatchService(ImportBatchRepository importBatchRepository,
                              ImportRowResultRepository importRowResultRepository,
                              SourceSystemRepository sourceSystemRepository,
                              AuthorizationService authorizationService,
                              Clock clock) {
        this.importBatchRepository = importBatchRepository;
        this.importRowResultRepository = importRowResultRepository;
        this.sourceSystemRepository = sourceSystemRepository;
        this.authorizationService = authorizationService;
        this.clock = clock;
    }

    @Transactional
    public ImportBatch start(ActorContext actor, StartImportBatchCommand command) {
        authorizationService.require(actor, Permission.OPERATIONAL_WRITE);
        validateStart(command);

        SourceSystem sourceSystem = sourceSystemRepository
                .findByOrganisationIdAndId(actor.organisationId(), command.sourceSystemId())
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "SOURCE_SYSTEM_NOT_FOUND",
                        "Source system was not found."));

        if (!sourceSystem.active()) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "INACTIVE_SOURCE_SYSTEM",
                    "Inactive source systems cannot receive new imports."
            );
        }

        ImportBatch batch = new ImportBatch(
                UUID.randomUUID(),
                actor.organisationId(),
                command.sourceSystemId(),
                command.templateType().trim(),
                command.templateVersion().trim(),
                ImportBatchStatus.RECEIVED,
                command.fileName().trim(),
                command.fileChecksum().trim(),
                actor.userId(),
                Instant.now(clock)
        );
        return importBatchRepository.save(batch);
    }

    @Transactional(readOnly = true)
    public List<ImportBatch> list(ActorContext actor) {
        authorizationService.require(actor, Permission.PROTECTED_READ);
        return importBatchRepository.findByOrganisationIdOrderByReceivedAtDesc(actor.organisationId());
    }

    @Transactional(readOnly = true)
    public ImportBatch get(ActorContext actor, UUID batchId) {
        authorizationService.require(actor, Permission.PROTECTED_READ);
        return findBatch(actor, batchId);
    }

    @Transactional
    public ImportRowResult recordRowResult(ActorContext actor, UUID batchId, RecordRowResultCommand command) {
        authorizationService.require(actor, Permission.OPERATIONAL_WRITE);
        validateRowResult(command);

        ImportBatch batch = findBatch(actor, batchId);
        requireOpen(batch);

        if (importRowResultRepository.existsByOrganisationIdAndImportBatchIdAndRowNumber(
                actor.organisationId(),
                batchId,
                command.rowNumber())) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "DUPLICATE_IMPORT_ROW_RESULT",
                    "Import row result already exists for this row number."
            );
        }

        ImportRowResult rowResult = new ImportRowResult(
                UUID.randomUUID(),
                actor.organisationId(),
                batchId,
                command.rowNumber(),
                command.outcome(),
                normalizeNullable(command.fieldName()),
                normalizeNullable(command.errorCode()),
                normalizeNullable(command.reason()),
                command.sourceRecordId(),
                Instant.now(clock)
        );
        batch.record(command.outcome());
        return importRowResultRepository.save(rowResult);
    }

    @Transactional(readOnly = true)
    public List<ImportRowResult> listRowResults(ActorContext actor, UUID batchId) {
        authorizationService.require(actor, Permission.PROTECTED_READ);
        findBatch(actor, batchId);
        return importRowResultRepository.findByOrganisationIdAndImportBatchIdOrderByRowNumberAsc(
                actor.organisationId(),
                batchId
        );
    }

    @Transactional
    public ImportBatch complete(ActorContext actor, UUID batchId) {
        authorizationService.require(actor, Permission.OPERATIONAL_WRITE);
        ImportBatch batch = findBatch(actor, batchId);
        requireOpen(batch);
        batch.complete(Instant.now(clock));
        return batch;
    }

    @Transactional
    public ImportBatch fail(ActorContext actor, UUID batchId, FailImportBatchCommand command) {
        authorizationService.require(actor, Permission.OPERATIONAL_WRITE);
        validateFailure(command);
        ImportBatch batch = findBatch(actor, batchId);
        requireOpen(batch);
        batch.fail(command.errorCode().trim(), command.reason().trim(), Instant.now(clock));
        return batch;
    }

    private ImportBatch findBatch(ActorContext actor, UUID batchId) {
        return importBatchRepository.findByOrganisationIdAndId(actor.organisationId(), batchId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "IMPORT_BATCH_NOT_FOUND",
                        "Import batch was not found."));
    }

    private void requireOpen(ImportBatch batch) {
        if (batch.status() != ImportBatchStatus.RECEIVED) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "IMPORT_BATCH_TERMINAL",
                    "Terminal import batches cannot be changed."
            );
        }
    }

    private void validateStart(StartImportBatchCommand command) {
        if (command.sourceSystemId() == null) {
            throw invalidField("sourceSystemId", "Source system id is required.");
        }
        if (!StringUtils.hasText(command.templateType())) {
            throw invalidField("templateType", "Template type is required.");
        }
        if (!StringUtils.hasText(command.templateVersion())) {
            throw invalidField("templateVersion", "Template version is required.");
        }
        if (!StringUtils.hasText(command.fileName())) {
            throw invalidField("fileName", "File name is required.");
        }
        if (!StringUtils.hasText(command.fileChecksum())) {
            throw invalidField("fileChecksum", "File checksum is required.");
        }
    }

    private void validateRowResult(RecordRowResultCommand command) {
        if (command.rowNumber() <= 0) {
            throw invalidField("rowNumber", "Row number must be positive.");
        }
        if (command.outcome() == null) {
            throw invalidField("outcome", "Outcome is required.");
        }
        if (command.outcome() == ImportRowOutcome.REJECTED || command.outcome() == ImportRowOutcome.FAILED) {
            if (!StringUtils.hasText(command.errorCode())) {
                throw invalidField("errorCode", "Error code is required for rejected or failed rows.");
            }
            if (!StringUtils.hasText(command.reason())) {
                throw invalidField("reason", "Reason is required for rejected or failed rows.");
            }
        }
    }

    private void validateFailure(FailImportBatchCommand command) {
        if (!StringUtils.hasText(command.errorCode())) {
            throw invalidField("errorCode", "Error code is required.");
        }
        if (!StringUtils.hasText(command.reason())) {
            throw invalidField("reason", "Reason is required.");
        }
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

    public record StartImportBatchCommand(
            UUID sourceSystemId,
            String templateType,
            String templateVersion,
            String fileName,
            String fileChecksum
    ) {
    }

    public record RecordRowResultCommand(
            int rowNumber,
            ImportRowOutcome outcome,
            String fieldName,
            String errorCode,
            String reason,
            UUID sourceRecordId
    ) {
    }

    public record FailImportBatchCommand(
            String errorCode,
            String reason
    ) {
    }
}

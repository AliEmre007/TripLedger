package com.tripledger.ingestion;

import com.tripledger.identity.ActorContext;
import com.tripledger.identity.ActorContextResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/import-batches")
public class ImportBatchController {

    private final ActorContextResolver actorContextResolver;
    private final ImportBatchService importBatchService;

    public ImportBatchController(ActorContextResolver actorContextResolver,
                                 ImportBatchService importBatchService) {
        this.actorContextResolver = actorContextResolver;
        this.importBatchService = importBatchService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ImportBatchResponse start(@Valid @RequestBody StartImportBatchRequest request,
                                     HttpServletRequest servletRequest) {
        ActorContext actor = actorContextResolver.resolve(servletRequest);
        ImportBatch batch = importBatchService.start(
                actor,
                new ImportBatchService.StartImportBatchCommand(
                        request.sourceSystemId(),
                        request.templateType(),
                        request.templateVersion(),
                        request.fileName(),
                        request.fileChecksum()
                )
        );
        return ImportBatchResponse.from(batch);
    }

    @GetMapping
    public List<ImportBatchResponse> list(HttpServletRequest servletRequest) {
        ActorContext actor = actorContextResolver.resolve(servletRequest);
        return importBatchService.list(actor).stream()
                .map(ImportBatchResponse::from)
                .toList();
    }

    @GetMapping("/{batchId}")
    public ImportBatchResponse get(@PathVariable UUID batchId, HttpServletRequest servletRequest) {
        ActorContext actor = actorContextResolver.resolve(servletRequest);
        return ImportBatchResponse.from(importBatchService.get(actor, batchId));
    }

    @PostMapping("/{batchId}/row-results")
    @ResponseStatus(HttpStatus.CREATED)
    public ImportRowResultResponse recordRowResult(@PathVariable UUID batchId,
                                                   @Valid @RequestBody RecordRowResultRequest request,
                                                   HttpServletRequest servletRequest) {
        ActorContext actor = actorContextResolver.resolve(servletRequest);
        ImportRowResult rowResult = importBatchService.recordRowResult(
                actor,
                batchId,
                new ImportBatchService.RecordRowResultCommand(
                        request.rowNumber(),
                        request.outcome(),
                        request.fieldName(),
                        request.errorCode(),
                        request.reason(),
                        request.sourceRecordId()
                )
        );
        return ImportRowResultResponse.from(rowResult);
    }

    @GetMapping("/{batchId}/row-results")
    public List<ImportRowResultResponse> listRowResults(@PathVariable UUID batchId,
                                                        HttpServletRequest servletRequest) {
        ActorContext actor = actorContextResolver.resolve(servletRequest);
        return importBatchService.listRowResults(actor, batchId).stream()
                .map(ImportRowResultResponse::from)
                .toList();
    }

    @PostMapping("/{batchId}/complete")
    public ImportBatchResponse complete(@PathVariable UUID batchId, HttpServletRequest servletRequest) {
        ActorContext actor = actorContextResolver.resolve(servletRequest);
        return ImportBatchResponse.from(importBatchService.complete(actor, batchId));
    }

    @PostMapping("/{batchId}/fail")
    public ImportBatchResponse fail(@PathVariable UUID batchId,
                                    @Valid @RequestBody FailImportBatchRequest request,
                                    HttpServletRequest servletRequest) {
        ActorContext actor = actorContextResolver.resolve(servletRequest);
        ImportBatch batch = importBatchService.fail(
                actor,
                batchId,
                new ImportBatchService.FailImportBatchCommand(request.errorCode(), request.reason())
        );
        return ImportBatchResponse.from(batch);
    }

    public record StartImportBatchRequest(
            @NotNull UUID sourceSystemId,
            @NotBlank String templateType,
            @NotBlank String templateVersion,
            @NotBlank String fileName,
            @NotBlank String fileChecksum
    ) {
    }

    public record RecordRowResultRequest(
            @Positive int rowNumber,
            @NotNull ImportRowOutcome outcome,
            String fieldName,
            String errorCode,
            String reason,
            UUID sourceRecordId
    ) {
    }

    public record FailImportBatchRequest(
            @NotBlank String errorCode,
            @NotBlank String reason
    ) {
    }

    public record ImportBatchResponse(
            UUID id,
            UUID organisationId,
            UUID sourceSystemId,
            String templateType,
            String templateVersion,
            ImportBatchStatus status,
            String fileName,
            String fileChecksum,
            UUID receivedByUserId,
            Instant receivedAt,
            Instant completedAt,
            String failureCode,
            String failureReason,
            int totalCount,
            int acceptedCount,
            int duplicateCount,
            int rejectedCount,
            int failedCount
    ) {

        static ImportBatchResponse from(ImportBatch batch) {
            return new ImportBatchResponse(
                    batch.id(),
                    batch.organisationId(),
                    batch.sourceSystemId(),
                    batch.templateType(),
                    batch.templateVersion(),
                    batch.status(),
                    batch.fileName(),
                    batch.fileChecksum(),
                    batch.receivedByUserId(),
                    batch.receivedAt(),
                    batch.completedAt(),
                    batch.failureCode(),
                    batch.failureReason(),
                    batch.totalCount(),
                    batch.acceptedCount(),
                    batch.duplicateCount(),
                    batch.rejectedCount(),
                    batch.failedCount()
            );
        }
    }

    public record ImportRowResultResponse(
            UUID id,
            UUID organisationId,
            UUID importBatchId,
            int rowNumber,
            ImportRowOutcome outcome,
            String fieldName,
            String errorCode,
            String reason,
            UUID sourceRecordId,
            Instant recordedAt
    ) {

        static ImportRowResultResponse from(ImportRowResult rowResult) {
            return new ImportRowResultResponse(
                    rowResult.id(),
                    rowResult.organisationId(),
                    rowResult.importBatchId(),
                    rowResult.rowNumber(),
                    rowResult.outcome(),
                    rowResult.fieldName(),
                    rowResult.errorCode(),
                    rowResult.reason(),
                    rowResult.sourceRecordId(),
                    rowResult.recordedAt()
            );
        }
    }
}

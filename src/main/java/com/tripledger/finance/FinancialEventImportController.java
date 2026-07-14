package com.tripledger.finance;

import com.tripledger.identity.ActorContext;
import com.tripledger.identity.ActorContextResolver;
import com.tripledger.ingestion.ImportBatch;
import com.tripledger.ingestion.ImportBatchStatus;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class FinancialEventImportController {

    private final ActorContextResolver actorContextResolver;
    private final FinancialEventCsvImportService financialEventCsvImportService;
    private final FinancialEventQueryService financialEventQueryService;

    public FinancialEventImportController(ActorContextResolver actorContextResolver,
                                          FinancialEventCsvImportService financialEventCsvImportService,
                                          FinancialEventQueryService financialEventQueryService) {
        this.actorContextResolver = actorContextResolver;
        this.financialEventCsvImportService = financialEventCsvImportService;
        this.financialEventQueryService = financialEventQueryService;
    }

    @PostMapping("/financial-event-imports")
    @ResponseStatus(HttpStatus.CREATED)
    public FinancialEventImportResponse importFinancialEvents(
            @Valid @RequestBody FinancialEventImportRequest request,
            HttpServletRequest servletRequest) {
        ActorContext actor = actorContextResolver.resolve(servletRequest);
        ImportBatch batch = financialEventCsvImportService.importCsv(
                actor,
                new FinancialEventCsvImportService.ImportFinancialEventCsvCommand(
                        request.sourceSystemId(),
                        request.fileName(),
                        request.fileChecksum(),
                        request.csvContent()
                )
        );
        return FinancialEventImportResponse.from(batch);
    }

    @GetMapping("/financial-events")
    public List<FinancialEventDetail> listFinancialEvents(
            @RequestParam(required = false) Boolean unmatched,
            HttpServletRequest servletRequest) {
        ActorContext actor = actorContextResolver.resolve(servletRequest);
        return financialEventQueryService.list(actor, unmatched);
    }

    public record FinancialEventImportRequest(
            @NotNull UUID sourceSystemId,
            @NotBlank String fileName,
            @NotBlank String fileChecksum,
            @NotBlank String csvContent
    ) {
    }

    public record FinancialEventImportResponse(
            UUID importBatchId,
            ImportBatchStatus status,
            int totalCount,
            int acceptedCount,
            int duplicateCount,
            int rejectedCount,
            int failedCount,
            Instant completedAt
    ) {

        static FinancialEventImportResponse from(ImportBatch batch) {
            return new FinancialEventImportResponse(
                    batch.id(),
                    batch.status(),
                    batch.totalCount(),
                    batch.acceptedCount(),
                    batch.duplicateCount(),
                    batch.rejectedCount(),
                    batch.failedCount(),
                    batch.completedAt()
            );
        }
    }
}

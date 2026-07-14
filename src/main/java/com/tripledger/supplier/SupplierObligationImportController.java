package com.tripledger.supplier;

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
public class SupplierObligationImportController {

    private final ActorContextResolver actorContextResolver;
    private final SupplierObligationCsvImportService supplierObligationCsvImportService;
    private final SupplierObligationQueryService supplierObligationQueryService;

    public SupplierObligationImportController(ActorContextResolver actorContextResolver,
                                              SupplierObligationCsvImportService supplierObligationCsvImportService,
                                              SupplierObligationQueryService supplierObligationQueryService) {
        this.actorContextResolver = actorContextResolver;
        this.supplierObligationCsvImportService = supplierObligationCsvImportService;
        this.supplierObligationQueryService = supplierObligationQueryService;
    }

    @PostMapping("/supplier-obligation-imports")
    @ResponseStatus(HttpStatus.CREATED)
    public SupplierObligationImportResponse importSupplierObligations(
            @Valid @RequestBody SupplierObligationImportRequest request,
            HttpServletRequest servletRequest) {
        ActorContext actor = actorContextResolver.resolve(servletRequest);
        ImportBatch batch = supplierObligationCsvImportService.importCsv(
                actor,
                new SupplierObligationCsvImportService.ImportSupplierObligationCsvCommand(
                        request.sourceSystemId(),
                        request.fileName(),
                        request.fileChecksum(),
                        request.csvContent()
                )
        );
        return SupplierObligationImportResponse.from(batch);
    }

    @GetMapping("/supplier-obligations")
    public List<SupplierObligationDetail> listSupplierObligations(
            @RequestParam(required = false) Boolean unlinked,
            HttpServletRequest servletRequest) {
        ActorContext actor = actorContextResolver.resolve(servletRequest);
        return supplierObligationQueryService.list(actor, unlinked);
    }

    public record SupplierObligationImportRequest(
            @NotNull UUID sourceSystemId,
            @NotBlank String fileName,
            @NotBlank String fileChecksum,
            @NotBlank String csvContent
    ) {
    }

    public record SupplierObligationImportResponse(
            UUID importBatchId,
            ImportBatchStatus status,
            int totalCount,
            int acceptedCount,
            int duplicateCount,
            int rejectedCount,
            int failedCount,
            Instant completedAt
    ) {

        static SupplierObligationImportResponse from(ImportBatch batch) {
            return new SupplierObligationImportResponse(
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

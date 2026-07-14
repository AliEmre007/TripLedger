package com.tripledger.booking;

import com.tripledger.identity.ActorContext;
import com.tripledger.identity.ActorContextResolver;
import com.tripledger.ingestion.ImportBatch;
import com.tripledger.ingestion.ImportBatchStatus;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/booking-imports")
public class BookingImportController {

    private final ActorContextResolver actorContextResolver;
    private final BookingCsvImportService bookingCsvImportService;

    public BookingImportController(ActorContextResolver actorContextResolver,
                                   BookingCsvImportService bookingCsvImportService) {
        this.actorContextResolver = actorContextResolver;
        this.bookingCsvImportService = bookingCsvImportService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BookingImportResponse importBookings(@Valid @RequestBody BookingImportRequest request,
                                                HttpServletRequest servletRequest) {
        ActorContext actor = actorContextResolver.resolve(servletRequest);
        ImportBatch batch = bookingCsvImportService.importCsv(
                actor,
                new BookingCsvImportService.ImportBookingCsvCommand(
                        request.sourceSystemId(),
                        request.fileName(),
                        request.fileChecksum(),
                        request.csvContent()
                )
        );
        return BookingImportResponse.from(batch);
    }

    public record BookingImportRequest(
            @NotNull UUID sourceSystemId,
            @NotBlank String fileName,
            @NotBlank String fileChecksum,
            @NotBlank String csvContent
    ) {
    }

    public record BookingImportResponse(
            UUID importBatchId,
            ImportBatchStatus status,
            int totalCount,
            int acceptedCount,
            int duplicateCount,
            int rejectedCount,
            int failedCount,
            Instant completedAt
    ) {

        static BookingImportResponse from(ImportBatch batch) {
            return new BookingImportResponse(
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

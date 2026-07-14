package com.tripledger.source;

import com.tripledger.identity.ActorContext;
import com.tripledger.identity.ActorContextResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/source-systems")
public class SourceSystemController {

    private final ActorContextResolver actorContextResolver;
    private final SourceSystemService sourceSystemService;

    public SourceSystemController(ActorContextResolver actorContextResolver,
                                  SourceSystemService sourceSystemService) {
        this.actorContextResolver = actorContextResolver;
        this.sourceSystemService = sourceSystemService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SourceSystemResponse create(@Valid @RequestBody CreateSourceSystemRequest request,
                                       HttpServletRequest servletRequest) {
        ActorContext actor = actorContextResolver.resolve(servletRequest);
        SourceSystem sourceSystem = sourceSystemService.create(
                actor,
                new SourceSystemService.CreateSourceSystemCommand(
                        request.name(),
                        request.category(),
                        request.externalCode(),
                        request.timeZone(),
                        request.active()
                )
        );
        return SourceSystemResponse.from(sourceSystem);
    }

    @GetMapping
    public List<SourceSystemResponse> list(HttpServletRequest servletRequest) {
        ActorContext actor = actorContextResolver.resolve(servletRequest);
        return sourceSystemService.list(actor).stream()
                .map(SourceSystemResponse::from)
                .toList();
    }

    public record CreateSourceSystemRequest(
            @NotBlank String name,
            @NotNull SourceSystemCategory category,
            @NotBlank String externalCode,
            @NotBlank String timeZone,
            boolean active
    ) {
    }

    public record SourceSystemResponse(
            UUID id,
            UUID organisationId,
            String name,
            SourceSystemCategory category,
            String externalCode,
            String timeZone,
            boolean active
    ) {

        static SourceSystemResponse from(SourceSystem sourceSystem) {
            return new SourceSystemResponse(
                    sourceSystem.id(),
                    sourceSystem.organisationId(),
                    sourceSystem.name(),
                    sourceSystem.category(),
                    sourceSystem.externalCode(),
                    sourceSystem.timeZone(),
                    sourceSystem.active()
            );
        }
    }
}

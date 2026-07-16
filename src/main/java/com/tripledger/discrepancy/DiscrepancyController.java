package com.tripledger.discrepancy;

import com.tripledger.identity.ActorContext;
import com.tripledger.identity.ActorContextResolver;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/discrepancies")
public class DiscrepancyController {

    private final ActorContextResolver actorContextResolver;
    private final DiscrepancyQueryService discrepancyQueryService;

    public DiscrepancyController(ActorContextResolver actorContextResolver,
                                 DiscrepancyQueryService discrepancyQueryService) {
        this.actorContextResolver = actorContextResolver;
        this.discrepancyQueryService = discrepancyQueryService;
    }

    @GetMapping
    public DiscrepancyListResponse list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) UUID ownerUserId,
            @RequestParam(required = false) String currency,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            HttpServletRequest servletRequest) {
        ActorContext actor = actorContextResolver.resolve(servletRequest);
        return discrepancyQueryService.list(actor, status, type, severity, ownerUserId, currency, page, size);
    }

    @GetMapping("/{discrepancyId}")
    public DiscrepancyDetail get(@PathVariable UUID discrepancyId, HttpServletRequest servletRequest) {
        ActorContext actor = actorContextResolver.resolve(servletRequest);
        return discrepancyQueryService.get(actor, discrepancyId);
    }
}

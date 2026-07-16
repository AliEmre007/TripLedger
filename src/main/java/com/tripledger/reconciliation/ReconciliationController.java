package com.tripledger.reconciliation;

import com.tripledger.identity.ActorContext;
import com.tripledger.identity.ActorContextResolver;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/bookings")
public class ReconciliationController {

    private final ActorContextResolver actorContextResolver;
    private final ReconciliationService reconciliationService;

    public ReconciliationController(ActorContextResolver actorContextResolver,
                                    ReconciliationService reconciliationService) {
        this.actorContextResolver = actorContextResolver;
        this.reconciliationService = reconciliationService;
    }

    @PostMapping("/{bookingId}/reconciliation-runs")
    @ResponseStatus(HttpStatus.CREATED)
    public ReconciliationResultDetail run(@PathVariable UUID bookingId, HttpServletRequest servletRequest) {
        ActorContext actor = actorContextResolver.resolve(servletRequest);
        return reconciliationService.run(actor, bookingId);
    }
}

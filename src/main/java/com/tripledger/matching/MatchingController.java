package com.tripledger.matching;

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
public class MatchingController {

    private final ActorContextResolver actorContextResolver;
    private final DeterministicMatcherService deterministicMatcherService;

    public MatchingController(ActorContextResolver actorContextResolver,
                              DeterministicMatcherService deterministicMatcherService) {
        this.actorContextResolver = actorContextResolver;
        this.deterministicMatcherService = deterministicMatcherService;
    }

    @PostMapping("/{bookingId}/matching-runs")
    @ResponseStatus(HttpStatus.CREATED)
    public MatchingRunResult run(@PathVariable UUID bookingId, HttpServletRequest servletRequest) {
        ActorContext actor = actorContextResolver.resolve(servletRequest);
        return deterministicMatcherService.run(actor, bookingId);
    }
}

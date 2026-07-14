package com.tripledger.identity;

import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class CurrentActorController {

    private final ActorContextResolver actorContextResolver;

    public CurrentActorController(ActorContextResolver actorContextResolver) {
        this.actorContextResolver = actorContextResolver;
    }

    @GetMapping("/me")
    public CurrentActorResponse currentActor(HttpServletRequest request) {
        ActorContext actor = actorContextResolver.resolve(request);
        return new CurrentActorResponse(
                actor.userId(),
                actor.organisationId(),
                actor.displayName(),
                actor.role(),
                actor.mfaSatisfied()
        );
    }

    public record CurrentActorResponse(
            UUID userId,
            UUID organisationId,
            String displayName,
            UserRole role,
            boolean mfaSatisfied
    ) {
    }
}

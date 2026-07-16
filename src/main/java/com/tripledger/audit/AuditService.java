package com.tripledger.audit;

import com.tripledger.identity.ActorContext;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class AuditService {

    public static final String TARGET_BOOKING = "BOOKING";
    public static final String TARGET_FINANCIAL_EVENT = "FINANCIAL_EVENT";

    private final AuditEventRepository auditEventRepository;
    private final Clock clock;

    public AuditService(AuditEventRepository auditEventRepository, Clock clock) {
        this.auditEventRepository = auditEventRepository;
        this.clock = clock;
    }

    public AuditEvent recordSuccess(ActorContext actor,
                                    String action,
                                    String targetType,
                                    UUID targetId,
                                    String beforeReference,
                                    String afterReference,
                                    String reason) {
        return auditEventRepository.save(new AuditEvent(
                UUID.randomUUID(),
                actor.organisationId(),
                actor.userId(),
                null,
                action,
                targetType,
                targetId,
                AuditOutcome.SUCCESS,
                beforeReference,
                afterReference,
                reason,
                actor.correlationId(),
                Instant.now(clock)
        ));
    }
}

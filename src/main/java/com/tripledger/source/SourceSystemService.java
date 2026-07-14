package com.tripledger.source;

import com.tripledger.authorization.AuthorizationService;
import com.tripledger.authorization.Permission;
import com.tripledger.common.api.ApiErrorResponse;
import com.tripledger.common.api.ApiException;
import com.tripledger.identity.ActorContext;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.zone.ZoneRulesException;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class SourceSystemService {

    private final SourceSystemRepository sourceSystemRepository;
    private final AuthorizationService authorizationService;
    private final Clock clock;

    public SourceSystemService(SourceSystemRepository sourceSystemRepository,
                               AuthorizationService authorizationService,
                               Clock clock) {
        this.sourceSystemRepository = sourceSystemRepository;
        this.authorizationService = authorizationService;
        this.clock = clock;
    }

    @Transactional
    public SourceSystem create(ActorContext actor, CreateSourceSystemCommand command) {
        authorizationService.require(actor, Permission.SOURCE_SYSTEM_MANAGE);

        String externalCode = normalizeExternalCode(command.externalCode());
        validate(command, externalCode);

        if (sourceSystemRepository.existsByOrganisationIdAndExternalCode(actor.organisationId(), externalCode)) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "DUPLICATE_SOURCE_CODE",
                    "Source system external code already exists."
            );
        }

        SourceSystem sourceSystem = new SourceSystem(
                UUID.randomUUID(),
                actor.organisationId(),
                command.name().trim(),
                command.category(),
                externalCode,
                command.timeZone().trim(),
                command.active(),
                Instant.now(clock)
        );
        return sourceSystemRepository.save(sourceSystem);
    }

    @Transactional(readOnly = true)
    public List<SourceSystem> list(ActorContext actor) {
        authorizationService.require(actor, Permission.PROTECTED_READ);
        return sourceSystemRepository.findByOrganisationIdOrderByExternalCodeAsc(actor.organisationId());
    }

    private void validate(CreateSourceSystemCommand command, String externalCode) {
        if (!StringUtils.hasText(command.name())) {
            throw invalidField("name", "Name is required.");
        }
        if (command.category() == null) {
            throw invalidField("category", "Category is required.");
        }
        if (!StringUtils.hasText(externalCode)) {
            throw invalidField("externalCode", "External code is required.");
        }
        if (!StringUtils.hasText(command.timeZone())) {
            throw invalidField("timeZone", "Time zone is required.");
        }

        try {
            ZoneId.of(command.timeZone().trim());
        } catch (ZoneRulesException exception) {
            throw invalidField("timeZone", "Time zone must be a valid IANA zone.");
        }
    }

    private ApiException invalidField(String field, String reason) {
        return new ApiException(
                HttpStatus.BAD_REQUEST,
                "INVALID_REQUEST",
                "Request validation failed.",
                List.of(new ApiErrorResponse.ApiErrorDetail(field, reason))
        );
    }

    private String normalizeExternalCode(String externalCode) {
        if (externalCode == null) {
            return "";
        }
        return externalCode.trim().toUpperCase(Locale.ROOT);
    }

    public record CreateSourceSystemCommand(
            String name,
            SourceSystemCategory category,
            String externalCode,
            String timeZone,
            boolean active
    ) {
    }
}

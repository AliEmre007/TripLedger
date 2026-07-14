package com.tripledger.identity;

import com.tripledger.common.api.ApiErrorResponse;
import com.tripledger.common.api.ApiException;
import com.tripledger.common.api.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ActorContextResolver {

    public static final String ACTOR_SUBJECT_HEADER = "X-TripLedger-Actor-Subject";
    public static final String ORGANISATION_ID_HEADER = "X-TripLedger-Organisation-Id";
    public static final String MFA_SATISFIED_HEADER = "X-TripLedger-Mfa-Satisfied";

    private final AppUserRepository appUserRepository;

    public ActorContextResolver(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    public ActorContext resolve(HttpServletRequest request) {
        String identitySubject = request.getHeader(ACTOR_SUBJECT_HEADER);
        if (!StringUtils.hasText(identitySubject)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "AUTHENTICATION_REQUIRED", "Authentication is required.");
        }

        UUID organisationId = parseOrganisationId(request.getHeader(ORGANISATION_ID_HEADER));
        AppUser user = appUserRepository.findByOrganisationIdAndIdentitySubject(organisationId, identitySubject)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.FORBIDDEN,
                        "ORG_REFERENCE_MISMATCH",
                        "Requested organisation is not available for this actor."
                ));

        if (user.status() != UserStatus.ACTIVE) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "AUTHENTICATION_REQUIRED", "Authentication is required.");
        }

        String correlationId = (String) request.getAttribute(CorrelationIdFilter.CORRELATION_ID_ATTRIBUTE);
        return new ActorContext(
                user.id(),
                user.organisationId(),
                user.displayName(),
                user.role(),
                Boolean.parseBoolean(request.getHeader(MFA_SATISFIED_HEADER)),
                correlationId
        );
    }

    private UUID parseOrganisationId(String rawOrganisationId) {
        if (!StringUtils.hasText(rawOrganisationId)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "AUTHENTICATION_REQUIRED", "Authentication is required.");
        }

        try {
            return UUID.fromString(rawOrganisationId);
        } catch (IllegalArgumentException exception) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_REQUEST",
                    "Request validation failed.",
                    List.of(new ApiErrorResponse.ApiErrorDetail(
                            ORGANISATION_ID_HEADER,
                            "Organisation id must be a valid UUID."))
            );
        }
    }
}

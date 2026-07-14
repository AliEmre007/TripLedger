package com.tripledger.authorization;

import com.tripledger.common.api.ApiException;
import com.tripledger.identity.ActorContext;
import com.tripledger.identity.UserRole;
import java.util.EnumSet;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class AuthorizationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthorizationService.class);
    private static final Set<UserRole> ALL_ROLES = EnumSet.allOf(UserRole.class);
    private static final Set<UserRole> FINANCIAL_ROLES = EnumSet.of(UserRole.ADMINISTRATOR, UserRole.FINANCE);

    public void require(ActorContext actor, Permission permission) {
        if (!allowedRoles(permission).contains(actor.role())) {
            logDenied(actor, permission, "role");
            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    "UNAUTHORISED_FINANCIAL_ACTION",
                    "Actor is not authorised for this action."
            );
        }

        if (permission == Permission.FINANCIAL_ACTION_WITH_MFA && !actor.mfaSatisfied()) {
            logDenied(actor, permission, "mfa");
            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    "MFA_REQUIRED",
                    "MFA is required for this action."
            );
        }
    }

    private Set<UserRole> allowedRoles(Permission permission) {
        return switch (permission) {
            case PROTECTED_READ -> ALL_ROLES;
            case SOURCE_SYSTEM_MANAGE -> EnumSet.of(UserRole.ADMINISTRATOR);
            case OPERATIONAL_WRITE -> EnumSet.of(UserRole.ADMINISTRATOR, UserRole.FINANCE, UserRole.OPERATIONS);
            case FINANCIAL_ACTION, FINANCIAL_ACTION_WITH_MFA -> FINANCIAL_ROLES;
        };
    }

    private void logDenied(ActorContext actor, Permission permission, String reason) {
        LOGGER.warn(
                "authorisation_denied permission={} reason={} actorId={} organisationId={} role={} correlationId={}",
                permission,
                reason,
                actor.userId(),
                actor.organisationId(),
                actor.role(),
                actor.correlationId()
        );
    }
}

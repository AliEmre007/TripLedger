package com.tripledger.authorization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tripledger.common.api.ApiException;
import com.tripledger.identity.ActorContext;
import com.tripledger.identity.UserRole;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class AuthorizationServiceTest {

    private static final UUID USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID ORGANISATION_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private final AuthorizationService authorizationService = new AuthorizationService();

    @Test
    void allAuthenticatedRolesCanReadProtectedData() {
        for (UserRole role : UserRole.values()) {
            ActorContext actor = actor(role, false);

            assertThatCode(() -> authorizationService.require(actor, Permission.PROTECTED_READ))
                    .doesNotThrowAnyException();
        }
    }

    @Test
    void onlyAdministratorCanManageSourceSystems() {
        assertThatCode(() -> authorizationService.require(
                actor(UserRole.ADMINISTRATOR, false),
                Permission.SOURCE_SYSTEM_MANAGE
        )).doesNotThrowAnyException();

        assertDenied(UserRole.FINANCE, Permission.SOURCE_SYSTEM_MANAGE, "UNAUTHORISED_FINANCIAL_ACTION");
        assertDenied(UserRole.OPERATIONS, Permission.SOURCE_SYSTEM_MANAGE, "UNAUTHORISED_FINANCIAL_ACTION");
        assertDenied(UserRole.READ_ONLY_MANAGER, Permission.SOURCE_SYSTEM_MANAGE, "UNAUTHORISED_FINANCIAL_ACTION");
    }

    @Test
    void operationsCannotPerformFinanceOnlyActions() {
        assertDenied(UserRole.OPERATIONS, Permission.FINANCIAL_ACTION, "UNAUTHORISED_FINANCIAL_ACTION");
        assertDenied(UserRole.READ_ONLY_MANAGER, Permission.FINANCIAL_ACTION, "UNAUTHORISED_FINANCIAL_ACTION");
    }

    @Test
    void financeAndAdministratorCanPerformFinanceActionsWithoutMfaWhenPolicyDoesNotRequireMfa() {
        assertThatCode(() -> authorizationService.require(
                actor(UserRole.FINANCE, false),
                Permission.FINANCIAL_ACTION
        )).doesNotThrowAnyException();
        assertThatCode(() -> authorizationService.require(
                actor(UserRole.ADMINISTRATOR, false),
                Permission.FINANCIAL_ACTION
        )).doesNotThrowAnyException();
    }

    @Test
    void financeAndAdministratorNeedMfaForMfaRequiredFinancialActions() {
        assertDenied(UserRole.FINANCE, Permission.FINANCIAL_ACTION_WITH_MFA, "MFA_REQUIRED");
        assertDenied(UserRole.ADMINISTRATOR, Permission.FINANCIAL_ACTION_WITH_MFA, "MFA_REQUIRED");

        assertThatCode(() -> authorizationService.require(
                actor(UserRole.FINANCE, true),
                Permission.FINANCIAL_ACTION_WITH_MFA
        )).doesNotThrowAnyException();
        assertThatCode(() -> authorizationService.require(
                actor(UserRole.ADMINISTRATOR, true),
                Permission.FINANCIAL_ACTION_WITH_MFA
        )).doesNotThrowAnyException();
    }

    @Test
    void readOnlyManagerCannotWriteOperationalData() {
        assertThatCode(() -> authorizationService.require(
                actor(UserRole.OPERATIONS, false),
                Permission.OPERATIONAL_WRITE
        )).doesNotThrowAnyException();

        assertDenied(UserRole.READ_ONLY_MANAGER, Permission.OPERATIONAL_WRITE, "UNAUTHORISED_FINANCIAL_ACTION");
    }

    private void assertDenied(UserRole role, Permission permission, String expectedCode) {
        assertThatThrownBy(() -> authorizationService.require(actor(role, false), permission))
                .isInstanceOfSatisfying(ApiException.class, exception -> {
                    assertThat(exception.status()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(exception.code()).isEqualTo(expectedCode);
                });
    }

    private ActorContext actor(UserRole role, boolean mfaSatisfied) {
        return new ActorContext(
                USER_ID,
                ORGANISATION_ID,
                "Test User",
                role,
                mfaSatisfied,
                "corr-123"
        );
    }
}

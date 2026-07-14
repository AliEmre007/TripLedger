package com.tripledger.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.tripledger.common.api.ApiException;
import com.tripledger.common.api.CorrelationIdFilter;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class ActorContextResolverTest {

    private static final UUID ORGANISATION_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Mock
    private AppUserRepository appUserRepository;

    @Test
    void resolvesActiveActorContextFromHeadersAndPersistedUser() {
        AppUser user = activeUser();
        when(appUserRepository.findByOrganisationIdAndIdentitySubject(ORGANISATION_ID, "finance@example.com"))
                .thenReturn(Optional.of(user));

        MockHttpServletRequest request = request("finance@example.com", ORGANISATION_ID.toString());
        request.addHeader(ActorContextResolver.MFA_SATISFIED_HEADER, "true");
        request.setAttribute(CorrelationIdFilter.CORRELATION_ID_ATTRIBUTE, "corr-123");

        ActorContext actor = new ActorContextResolver(appUserRepository).resolve(request);

        assertThat(actor.userId()).isEqualTo(USER_ID);
        assertThat(actor.organisationId()).isEqualTo(ORGANISATION_ID);
        assertThat(actor.displayName()).isEqualTo("Finance User");
        assertThat(actor.role()).isEqualTo(UserRole.FINANCE);
        assertThat(actor.mfaSatisfied()).isTrue();
        assertThat(actor.correlationId()).isEqualTo("corr-123");
    }

    @Test
    void rejectsMissingActorSubject() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(ActorContextResolver.ORGANISATION_ID_HEADER, ORGANISATION_ID.toString());

        assertThatThrownBy(() -> new ActorContextResolver(appUserRepository).resolve(request))
                .isInstanceOfSatisfying(ApiException.class, exception -> {
                    assertThat(exception.status()).isEqualTo(HttpStatus.UNAUTHORIZED);
                    assertThat(exception.code()).isEqualTo("AUTHENTICATION_REQUIRED");
                });
    }

    @Test
    void rejectsMalformedOrganisationHeader() {
        MockHttpServletRequest request = request("finance@example.com", "not-a-uuid");

        assertThatThrownBy(() -> new ActorContextResolver(appUserRepository).resolve(request))
                .isInstanceOfSatisfying(ApiException.class, exception -> {
                    assertThat(exception.status()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(exception.code()).isEqualTo("INVALID_REQUEST");
                });
    }

    @Test
    void rejectsActorOutsideRequestedOrganisationWithoutRevealingUserDetails() {
        when(appUserRepository.findByOrganisationIdAndIdentitySubject(ORGANISATION_ID, "finance@example.com"))
                .thenReturn(Optional.empty());

        MockHttpServletRequest request = request("finance@example.com", ORGANISATION_ID.toString());

        assertThatThrownBy(() -> new ActorContextResolver(appUserRepository).resolve(request))
                .isInstanceOfSatisfying(ApiException.class, exception -> {
                    assertThat(exception.status()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(exception.code()).isEqualTo("ORG_REFERENCE_MISMATCH");
                    assertThat(exception.getMessage()).doesNotContain("finance@example.com");
                });
    }

    @Test
    void rejectsInactiveUser() {
        AppUser inactiveUser = new AppUser(
                USER_ID,
                ORGANISATION_ID,
                "finance@example.com",
                "Finance User",
                UserRole.FINANCE,
                UserStatus.INACTIVE,
                Instant.parse("2026-07-13T00:00:00Z"),
                Instant.parse("2026-07-14T00:00:00Z"));
        when(appUserRepository.findByOrganisationIdAndIdentitySubject(ORGANISATION_ID, "finance@example.com"))
                .thenReturn(Optional.of(inactiveUser));

        MockHttpServletRequest request = request("finance@example.com", ORGANISATION_ID.toString());

        assertThatThrownBy(() -> new ActorContextResolver(appUserRepository).resolve(request))
                .isInstanceOfSatisfying(ApiException.class, exception -> {
                    assertThat(exception.status()).isEqualTo(HttpStatus.UNAUTHORIZED);
                    assertThat(exception.code()).isEqualTo("AUTHENTICATION_REQUIRED");
                });
    }

    private MockHttpServletRequest request(String subject, String organisationId) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(ActorContextResolver.ACTOR_SUBJECT_HEADER, subject);
        request.addHeader(ActorContextResolver.ORGANISATION_ID_HEADER, organisationId);
        return request;
    }

    private AppUser activeUser() {
        return new AppUser(
                USER_ID,
                ORGANISATION_ID,
                "finance@example.com",
                "Finance User",
                UserRole.FINANCE,
                UserStatus.ACTIVE,
                Instant.parse("2026-07-13T00:00:00Z"),
                null);
    }
}

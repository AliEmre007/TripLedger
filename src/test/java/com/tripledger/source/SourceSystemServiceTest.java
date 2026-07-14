package com.tripledger.source;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tripledger.authorization.AuthorizationService;
import com.tripledger.authorization.Permission;
import com.tripledger.common.api.ApiException;
import com.tripledger.identity.ActorContext;
import com.tripledger.identity.UserRole;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class SourceSystemServiceTest {

    private static final UUID ORGANISATION_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final Instant NOW = Instant.parse("2026-07-14T06:00:00Z");

    @Mock
    private SourceSystemRepository sourceSystemRepository;

    @Mock
    private AuthorizationService authorizationService;

    @Test
    void createsSourceSystemScopedToActorOrganisation() {
        SourceSystemService service = service();
        when(sourceSystemRepository.existsByOrganisationIdAndExternalCode(ORGANISATION_ID, "OTA_EXPORT"))
                .thenReturn(false);
        when(sourceSystemRepository.save(any(SourceSystem.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SourceSystem sourceSystem = service.create(adminActor(), new SourceSystemService.CreateSourceSystemCommand(
                "OTA Export",
                SourceSystemCategory.BOOKING_CHANNEL,
                " ota_export ",
                "Europe/Istanbul",
                true
        ));

        assertThat(sourceSystem.organisationId()).isEqualTo(ORGANISATION_ID);
        assertThat(sourceSystem.name()).isEqualTo("OTA Export");
        assertThat(sourceSystem.externalCode()).isEqualTo("OTA_EXPORT");
        assertThat(sourceSystem.timeZone()).isEqualTo("Europe/Istanbul");
        assertThat(sourceSystem.createdAt()).isEqualTo(NOW);
        verify(authorizationService).require(adminActor(), Permission.SOURCE_SYSTEM_MANAGE);
    }

    @Test
    void rejectsDuplicateExternalCodeInsideOrganisation() {
        when(sourceSystemRepository.existsByOrganisationIdAndExternalCode(ORGANISATION_ID, "OTA_EXPORT"))
                .thenReturn(true);

        assertThatThrownBy(() -> service().create(adminActor(), new SourceSystemService.CreateSourceSystemCommand(
                "OTA Export",
                SourceSystemCategory.BOOKING_CHANNEL,
                "OTA_EXPORT",
                "Europe/Istanbul",
                true
        ))).isInstanceOfSatisfying(ApiException.class, exception -> {
            assertThat(exception.status()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(exception.code()).isEqualTo("DUPLICATE_SOURCE_CODE");
        });
    }

    @Test
    void rejectsInvalidTimeZone() {
        assertThatThrownBy(() -> service().create(adminActor(), new SourceSystemService.CreateSourceSystemCommand(
                "OTA Export",
                SourceSystemCategory.BOOKING_CHANNEL,
                "OTA_EXPORT",
                "Not/AZone",
                true
        ))).isInstanceOfSatisfying(ApiException.class, exception -> {
            assertThat(exception.status()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(exception.code()).isEqualTo("INVALID_REQUEST");
            assertThat(exception.details()).hasSize(1);
            assertThat(exception.details().getFirst().field()).isEqualTo("timeZone");
        });
    }

    @Test
    void propagatesRoleDenialFromAuthorizationService() {
        ApiException denial = new ApiException(
                HttpStatus.FORBIDDEN,
                "UNAUTHORISED_FINANCIAL_ACTION",
                "Actor is not authorised for this action.");
        org.mockito.Mockito.doThrow(denial)
                .when(authorizationService)
                .require(operationsActor(), Permission.SOURCE_SYSTEM_MANAGE);

        assertThatThrownBy(() -> service().create(operationsActor(), new SourceSystemService.CreateSourceSystemCommand(
                "OTA Export",
                SourceSystemCategory.BOOKING_CHANNEL,
                "OTA_EXPORT",
                "Europe/Istanbul",
                true
        ))).isSameAs(denial);
    }

    @Test
    void listsOnlyActorOrganisationSourceSystems() {
        SourceSystem sourceSystem = new SourceSystem(
                UUID.randomUUID(),
                ORGANISATION_ID,
                "OTA Export",
                SourceSystemCategory.BOOKING_CHANNEL,
                "OTA_EXPORT",
                "Europe/Istanbul",
                true,
                NOW);
        when(sourceSystemRepository.findByOrganisationIdOrderByExternalCodeAsc(ORGANISATION_ID))
                .thenReturn(List.of(sourceSystem));

        List<SourceSystem> results = service().list(adminActor());

        assertThat(results).containsExactly(sourceSystem);
        verify(sourceSystemRepository).findByOrganisationIdOrderByExternalCodeAsc(ORGANISATION_ID);
        verify(authorizationService).require(adminActor(), Permission.PROTECTED_READ);
    }

    @Test
    void savesNormalisedExternalCode() {
        SourceSystemService service = service();
        when(sourceSystemRepository.save(any(SourceSystem.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        ArgumentCaptor<SourceSystem> captor = ArgumentCaptor.forClass(SourceSystem.class);

        assertThatCode(() -> service.create(adminActor(), new SourceSystemService.CreateSourceSystemCommand(
                "Manual Source",
                SourceSystemCategory.MANUAL,
                " manual-one ",
                "UTC",
                false
        ))).doesNotThrowAnyException();

        verify(sourceSystemRepository).save(captor.capture());
        assertThat(captor.getValue().externalCode()).isEqualTo("MANUAL-ONE");
    }

    private SourceSystemService service() {
        return new SourceSystemService(
                sourceSystemRepository,
                authorizationService,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    private ActorContext adminActor() {
        return actor(UserRole.ADMINISTRATOR);
    }

    private ActorContext operationsActor() {
        return actor(UserRole.OPERATIONS);
    }

    private ActorContext actor(UserRole role) {
        return new ActorContext(
                USER_ID,
                ORGANISATION_ID,
                "Test User",
                role,
                true,
                "corr-123"
        );
    }
}

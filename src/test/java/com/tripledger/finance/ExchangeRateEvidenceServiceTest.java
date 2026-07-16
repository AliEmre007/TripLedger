package com.tripledger.finance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tripledger.authorization.AuthorizationService;
import com.tripledger.authorization.Permission;
import com.tripledger.common.api.ApiException;
import com.tripledger.identity.ActorContext;
import com.tripledger.identity.UserRole;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class ExchangeRateEvidenceServiceTest {

    private static final UUID ORGANISATION_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID EVENT_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final Instant NOW = Instant.parse("2026-07-14T06:00:00Z");

    @Mock
    private ExchangeRateEvidenceRepository exchangeRateEvidenceRepository;

    @Mock
    private FinancialEventRepository financialEventRepository;

    @Mock
    private AuthorizationService authorizationService;

    @Test
    void createsExchangeRateEvidenceForFinancialEvent() {
        when(financialEventRepository.findByOrganisationIdAndId(ORGANISATION_ID, EVENT_ID))
                .thenReturn(Optional.of(financialEvent()));
        when(exchangeRateEvidenceRepository.save(any(ExchangeRateEvidence.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ExchangeRateEvidenceDetail result = service().create(actor(), command(
                EVENT_ID,
                new BigDecimal("3500.00"),
                "try",
                "EUR",
                new BigDecimal("0.0285714286")
        ));

        assertThat(result.organisationId()).isEqualTo(ORGANISATION_ID);
        assertThat(result.financialEventId()).isEqualTo(EVENT_ID);
        assertThat(result.sourceAmount()).isEqualByComparingTo(new BigDecimal("3500.00"));
        assertThat(result.sourceCurrency()).isEqualTo("TRY");
        assertThat(result.targetAmount()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(result.targetCurrency()).isEqualTo("EUR");
        assertThat(result.rate()).isEqualByComparingTo(new BigDecimal("0.028571428600"));
        assertThat(result.createdByUserId()).isEqualTo(USER_ID);
        assertThat(result.createdAt()).isEqualTo(NOW);
        verify(authorizationService).require(actor(), Permission.FINANCIAL_ACTION_WITH_MFA);
    }

    @Test
    void rejectsEvidenceWhenFinancialEventBelongsToAnotherOrganisation() {
        when(financialEventRepository.findByOrganisationIdAndId(ORGANISATION_ID, EVENT_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().create(actor(), command(
                EVENT_ID,
                new BigDecimal("3500.00"),
                "TRY",
                "EUR",
                new BigDecimal("0.0285714286")
        ))).isInstanceOfSatisfying(ApiException.class, exception -> {
            assertThat(exception.status()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(exception.code()).isEqualTo("FINANCIAL_EVENT_NOT_FOUND");
        });
    }

    @Test
    void rejectsEvidenceThatDoesNotMatchFinancialEventCurrency() {
        when(financialEventRepository.findByOrganisationIdAndId(ORGANISATION_ID, EVENT_ID))
                .thenReturn(Optional.of(financialEvent()));

        assertThatThrownBy(() -> service().create(actor(), command(
                EVENT_ID,
                new BigDecimal("3500.00"),
                "USD",
                "EUR",
                new BigDecimal("0.92")
        ))).isInstanceOfSatisfying(ApiException.class, exception -> {
            assertThat(exception.status()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(exception.code()).isEqualTo("INVALID_EXCHANGE_RATE");
            assertThat(exception.details().getFirst().field()).isEqualTo("sourceCurrency");
        });
    }

    @Test
    void rejectsInvalidRateScale() {
        assertThatThrownBy(() -> service().create(actor(), command(
                null,
                new BigDecimal("3500.00"),
                "TRY",
                "EUR",
                new BigDecimal("0.0285714286123")
        ))).isInstanceOfSatisfying(ApiException.class, exception -> {
            assertThat(exception.status()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(exception.code()).isEqualTo("INVALID_EXCHANGE_RATE");
            assertThat(exception.details().getFirst().field()).isEqualTo("rate");
        });
    }

    @Test
    void listsEvidenceInsideActorOrganisation() {
        ExchangeRateEvidence evidence = new ExchangeRateEvidence(
                UUID.randomUUID(),
                ORGANISATION_ID,
                EVENT_ID,
                new BigDecimal("3500.00"),
                "TRY",
                new BigDecimal("100.00"),
                "EUR",
                new BigDecimal("0.028571428600"),
                NOW,
                "manual-finance-evidence",
                "HALF_UP-v1",
                USER_ID,
                NOW
        );
        when(exchangeRateEvidenceRepository
                .findAllByOrganisationIdAndFinancialEventIdOrderByEffectiveAtDescCreatedAtDesc(
                        ORGANISATION_ID,
                        EVENT_ID))
                .thenReturn(List.of(evidence));

        List<ExchangeRateEvidenceDetail> results = service().list(actor(), EVENT_ID);

        assertThat(results).hasSize(1);
        assertThat(results.getFirst().financialEventId()).isEqualTo(EVENT_ID);
        verify(authorizationService).require(actor(), Permission.PROTECTED_READ);
    }

    private ExchangeRateEvidenceService service() {
        return new ExchangeRateEvidenceService(
                exchangeRateEvidenceRepository,
                financialEventRepository,
                authorizationService,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    private ExchangeRateEvidenceService.CreateExchangeRateEvidenceCommand command(UUID financialEventId,
                                                                                  BigDecimal sourceAmount,
                                                                                  String sourceCurrency,
                                                                                  String targetCurrency,
                                                                                  BigDecimal rate) {
        return new ExchangeRateEvidenceService.CreateExchangeRateEvidenceCommand(
                financialEventId,
                sourceAmount,
                sourceCurrency,
                targetCurrency,
                rate,
                Instant.parse("2026-07-10T09:00:00Z"),
                "manual-finance-evidence",
                "HALF_UP-v1"
        );
    }

    private FinancialEvent financialEvent() {
        return new FinancialEvent(
                EVENT_ID,
                ORGANISATION_ID,
                UUID.randomUUID(),
                UUID.randomUUID(),
                FinancialEventType.CUSTOMER_PAYMENT,
                FinancialEventDirection.INCREASE_RECEIVED,
                new BigDecimal("3500.00"),
                "TRY",
                Instant.parse("2026-07-09T12:00:00Z"),
                "PAY-TRY-100",
                null,
                null,
                USER_ID,
                NOW
        );
    }

    private ActorContext actor() {
        return new ActorContext(
                USER_ID,
                ORGANISATION_ID,
                "Finance User",
                UserRole.FINANCE,
                true,
                "corr-123"
        );
    }
}

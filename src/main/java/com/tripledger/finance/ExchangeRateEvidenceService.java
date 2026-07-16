package com.tripledger.finance;

import com.tripledger.authorization.AuthorizationService;
import com.tripledger.authorization.Permission;
import com.tripledger.common.api.ApiErrorResponse;
import com.tripledger.common.api.ApiException;
import com.tripledger.common.money.MoneyPolicy;
import com.tripledger.common.money.MoneyValidationException;
import com.tripledger.identity.ActorContext;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ExchangeRateEvidenceService {

    private static final int RATE_SCALE = 12;

    private final ExchangeRateEvidenceRepository exchangeRateEvidenceRepository;
    private final FinancialEventRepository financialEventRepository;
    private final AuthorizationService authorizationService;
    private final Clock clock;

    public ExchangeRateEvidenceService(ExchangeRateEvidenceRepository exchangeRateEvidenceRepository,
                                       FinancialEventRepository financialEventRepository,
                                       AuthorizationService authorizationService,
                                       Clock clock) {
        this.exchangeRateEvidenceRepository = exchangeRateEvidenceRepository;
        this.financialEventRepository = financialEventRepository;
        this.authorizationService = authorizationService;
        this.clock = clock;
    }

    @Transactional
    public ExchangeRateEvidenceDetail create(ActorContext actor, CreateExchangeRateEvidenceCommand command) {
        authorizationService.require(actor, Permission.FINANCIAL_ACTION_WITH_MFA);

        String sourceCurrency = currency("sourceCurrency", command.sourceCurrency());
        String targetCurrency = currency("targetCurrency", command.targetCurrency());
        if (sourceCurrency.equals(targetCurrency)) {
            throw invalid("targetCurrency", "Target currency must differ from source currency.");
        }

        BigDecimal sourceAmount = positiveMoney("sourceAmount", command.sourceAmount(), sourceCurrency);
        BigDecimal rate = rate(command.rate());
        BigDecimal targetAmount = MoneyPolicy.convert(sourceAmount, sourceCurrency, targetCurrency, rate);
        Instant effectiveAt = command.effectiveAt() == null ? Instant.now(clock) : command.effectiveAt();
        String rateSource = required("rateSource", command.rateSource());
        String roundingPolicyVersion = required("roundingPolicyVersion", command.roundingPolicyVersion());

        if (command.financialEventId() != null) {
            FinancialEvent event = financialEventRepository
                    .findByOrganisationIdAndId(actor.organisationId(), command.financialEventId())
                    .orElseThrow(() -> new ApiException(
                            HttpStatus.NOT_FOUND,
                            "FINANCIAL_EVENT_NOT_FOUND",
                            "Financial event was not found."));
            validateFinancialEventEvidence(event, sourceAmount, sourceCurrency);
        }

        ExchangeRateEvidence evidence = new ExchangeRateEvidence(
                UUID.randomUUID(),
                actor.organisationId(),
                command.financialEventId(),
                sourceAmount,
                sourceCurrency,
                targetAmount,
                targetCurrency,
                rate,
                effectiveAt,
                rateSource,
                roundingPolicyVersion,
                actor.userId(),
                Instant.now(clock)
        );
        return ExchangeRateEvidenceDetail.from(exchangeRateEvidenceRepository.save(evidence));
    }

    @Transactional(readOnly = true)
    public List<ExchangeRateEvidenceDetail> list(ActorContext actor, UUID financialEventId) {
        authorizationService.require(actor, Permission.PROTECTED_READ);
        List<ExchangeRateEvidence> evidence = financialEventId == null
                ? exchangeRateEvidenceRepository.findAllByOrganisationIdOrderByEffectiveAtDescCreatedAtDesc(
                        actor.organisationId())
                : exchangeRateEvidenceRepository
                        .findAllByOrganisationIdAndFinancialEventIdOrderByEffectiveAtDescCreatedAtDesc(
                                actor.organisationId(),
                                financialEventId);

        return evidence.stream()
                .map(ExchangeRateEvidenceDetail::from)
                .toList();
    }

    private void validateFinancialEventEvidence(FinancialEvent event, BigDecimal sourceAmount, String sourceCurrency) {
        if (!event.currency().equals(sourceCurrency)) {
            throw invalid("sourceCurrency", "Source currency must match the financial event currency.");
        }
        if (event.amount().compareTo(sourceAmount) != 0) {
            throw invalid("sourceAmount", "Source amount must match the financial event amount.");
        }
    }

    private BigDecimal rate(BigDecimal rawRate) {
        if (rawRate == null) {
            throw invalid("rate", "Rate is required.");
        }
        if (rawRate.signum() <= 0) {
            throw invalid("rate", "Rate must be positive.");
        }
        if (rawRate.stripTrailingZeros().scale() > RATE_SCALE) {
            throw invalid("rate", "Rate supports up to 12 fractional digits.");
        }
        return rawRate.setScale(RATE_SCALE);
    }

    private String currency(String field, String rawCurrency) {
        try {
            return MoneyPolicy.currency(rawCurrency);
        } catch (MoneyValidationException exception) {
            throw invalid(field, exception.getMessage(), exception.code());
        }
    }

    private BigDecimal positiveMoney(String field, BigDecimal amount, String currency) {
        try {
            return MoneyPolicy.positiveAmount(amount, currency);
        } catch (MoneyValidationException exception) {
            throw invalid(field, exception.getMessage(), exception.code());
        }
    }

    private String required(String field, String value) {
        if (!StringUtils.hasText(value)) {
            throw invalid(field, field + " is required.");
        }
        return value.trim();
    }

    private ApiException invalid(String field, String reason) {
        return invalid(field, reason, "INVALID_EXCHANGE_RATE");
    }

    private ApiException invalid(String field, String reason, String code) {
        return new ApiException(
                HttpStatus.BAD_REQUEST,
                code,
                "Exchange-rate evidence is invalid.",
                List.of(new ApiErrorResponse.ApiErrorDetail(field, reason))
        );
    }

    public record CreateExchangeRateEvidenceCommand(
            UUID financialEventId,
            BigDecimal sourceAmount,
            String sourceCurrency,
            String targetCurrency,
            BigDecimal rate,
            Instant effectiveAt,
            String rateSource,
            String roundingPolicyVersion
    ) {
    }
}

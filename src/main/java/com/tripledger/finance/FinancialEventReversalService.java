package com.tripledger.finance;

import com.tripledger.authorization.AuthorizationService;
import com.tripledger.authorization.Permission;
import com.tripledger.common.api.ApiErrorResponse;
import com.tripledger.common.api.ApiException;
import com.tripledger.identity.ActorContext;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class FinancialEventReversalService {

    private static final Set<String> SUPPORTED_CURRENCIES = Set.of("EUR", "GBP", "TRY", "USD");

    private final FinancialEventRepository financialEventRepository;
    private final AuthorizationService authorizationService;
    private final Clock clock;

    public FinancialEventReversalService(FinancialEventRepository financialEventRepository,
                                         AuthorizationService authorizationService,
                                         Clock clock) {
        this.financialEventRepository = financialEventRepository;
        this.authorizationService = authorizationService;
        this.clock = clock;
    }

    @Transactional
    public FinancialEventReversalResult reverse(ActorContext actor, UUID eventId, ReverseFinancialEventCommand command) {
        authorizationService.require(actor, Permission.FINANCIAL_ACTION_WITH_MFA);
        validateCommand(command);

        FinancialEvent original = financialEventRepository.findByOrganisationIdAndId(actor.organisationId(), eventId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "FINANCIAL_EVENT_NOT_FOUND",
                        "Financial event was not found."
                ));

        if (original.reversesEventId() != null || original.eventType() == FinancialEventType.REVERSAL) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "INVALID_FINANCIAL_REVERSAL",
                    "Reversal events cannot be reversed through this path."
            );
        }
        if (financialEventRepository.existsByOrganisationIdAndReversesEventId(actor.organisationId(), original.id())) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "FINANCIAL_EVENT_ALREADY_REVERSED",
                    "Financial event already has a reversal."
            );
        }

        Instant now = Instant.now(clock);
        FinancialEvent reversal = financialEventRepository.save(new FinancialEvent(
                UUID.randomUUID(),
                actor.organisationId(),
                null,
                original.bookingId(),
                FinancialEventType.REVERSAL,
                FinancialEventDirection.REVERSAL,
                original.amount(),
                original.currency(),
                command.effectiveAt() == null ? now : command.effectiveAt(),
                "REVERSAL-" + original.id(),
                original.id(),
                command.reason().trim(),
                actor.userId(),
                now
        ));

        FinancialEvent replacement = null;
        if (command.replacementEvent() != null) {
            ReplacementFinancialEventCommand replacementCommand = command.replacementEvent();
            replacement = financialEventRepository.save(new FinancialEvent(
                    UUID.randomUUID(),
                    actor.organisationId(),
                    null,
                    original.bookingId(),
                    replacementCommand.eventType(),
                    directionFor(replacementCommand.eventType()),
                    money(replacementCommand.amount()),
                    currency(replacementCommand.currency()),
                    replacementCommand.effectiveAt(),
                    normalizeNullable(replacementCommand.externalReference()),
                    null,
                    replacementCommand.eventType() == FinancialEventType.MANUAL_ADJUSTMENT
                            ? command.reason().trim()
                            : null,
                    actor.userId(),
                    now
            ));
        }

        return new FinancialEventReversalResult(
                FinancialEventDetail.from(reversal, null),
                replacement == null ? null : FinancialEventDetail.from(replacement, null)
        );
    }

    private void validateCommand(ReverseFinancialEventCommand command) {
        if (command == null) {
            throw invalidField("body", "Request body is required.");
        }
        if (!StringUtils.hasText(command.reason())) {
            throw invalidField("reason", "Reason is required.");
        }
        if (command.replacementEvent() != null) {
            validateReplacement(command.replacementEvent());
        }
    }

    private void validateReplacement(ReplacementFinancialEventCommand command) {
        if (command.eventType() == null) {
            throw invalidField("replacementEvent.eventType", "Replacement event type is required.");
        }
        if (command.eventType() == FinancialEventType.REVERSAL) {
            throw invalidField("replacementEvent.eventType", "Replacement event cannot be a reversal.");
        }
        if (command.amount() == null) {
            throw invalidField("replacementEvent.amount", "Replacement amount is required.");
        }
        money(command.amount());
        currency(command.currency());
        if (command.effectiveAt() == null) {
            throw invalidField("replacementEvent.effectiveAt", "Replacement effective timestamp is required.");
        }
    }

    private BigDecimal money(BigDecimal amount) {
        if (amount.signum() <= 0) {
            throw invalidField("replacementEvent.amount", "Amount must be positive.");
        }
        if (amount.stripTrailingZeros().scale() > 2) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_CURRENCY_PRECISION",
                    "Currency amount has too many fractional digits.",
                    List.of(new ApiErrorResponse.ApiErrorDetail(
                            "replacementEvent.amount",
                            "Currency amount has too many fractional digits."))
            );
        }
        return amount.setScale(2, RoundingMode.UNNECESSARY);
    }

    private String currency(String currency) {
        if (!StringUtils.hasText(currency)) {
            throw invalidField("replacementEvent.currency", "Currency is required.");
        }
        String normalized = currency.trim().toUpperCase(Locale.ROOT);
        if (!normalized.matches("[A-Z]{3}") || !SUPPORTED_CURRENCIES.contains(normalized)) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_CURRENCY",
                    "Currency is not supported.",
                    List.of(new ApiErrorResponse.ApiErrorDetail(
                            "replacementEvent.currency",
                            "Currency is not supported."))
            );
        }
        return normalized;
    }

    private FinancialEventDirection directionFor(FinancialEventType eventType) {
        return switch (eventType) {
            case CUSTOMER_PAYMENT, CHANNEL_SETTLEMENT -> FinancialEventDirection.INCREASE_RECEIVED;
            case REFUND, PAYMENT_REVERSAL -> FinancialEventDirection.DECREASE_RECEIVED;
            case CHANNEL_COMMISSION, PAYMENT_FEE -> FinancialEventDirection.INCREASE_DEDUCTION;
            case SUPPLIER_PAYMENT -> FinancialEventDirection.INCREASE_SUPPLIER_SETTLEMENT;
            case SUPPLIER_CREDIT -> FinancialEventDirection.DECREASE_SUPPLIER_COST;
            case REVERSAL -> FinancialEventDirection.REVERSAL;
            case MANUAL_ADJUSTMENT -> FinancialEventDirection.ADJUSTMENT;
        };
    }

    private ApiException invalidField(String field, String reason) {
        return new ApiException(
                HttpStatus.BAD_REQUEST,
                "INVALID_REQUEST",
                "Request validation failed.",
                List.of(new ApiErrorResponse.ApiErrorDetail(field, reason))
        );
    }

    private String normalizeNullable(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    public record ReverseFinancialEventCommand(
            String reason,
            Instant effectiveAt,
            ReplacementFinancialEventCommand replacementEvent
    ) {
    }

    public record ReplacementFinancialEventCommand(
            FinancialEventType eventType,
            BigDecimal amount,
            String currency,
            Instant effectiveAt,
            String externalReference
    ) {
    }

    public record FinancialEventReversalResult(
            FinancialEventDetail reversal,
            FinancialEventDetail replacementEvent
    ) {
    }
}

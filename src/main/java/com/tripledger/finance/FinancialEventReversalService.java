package com.tripledger.finance;

import com.tripledger.audit.AuditService;
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
public class FinancialEventReversalService {

    private final FinancialEventRepository financialEventRepository;
    private final AuthorizationService authorizationService;
    private final AuditService auditService;
    private final Clock clock;

    public FinancialEventReversalService(FinancialEventRepository financialEventRepository,
                                         AuthorizationService authorizationService,
                                         AuditService auditService,
                                         Clock clock) {
        this.financialEventRepository = financialEventRepository;
        this.authorizationService = authorizationService;
        this.auditService = auditService;
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
                    replacementAmount(replacementCommand),
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

        auditService.recordSuccess(
                actor,
                "FINANCIAL_EVENT_REVERSED",
                original.bookingId() == null ? AuditService.TARGET_FINANCIAL_EVENT : AuditService.TARGET_BOOKING,
                original.bookingId() == null ? original.id() : original.bookingId(),
                "financial_event:" + original.id(),
                replacement == null
                        ? "financial_event:" + reversal.id()
                        : "financial_event:" + reversal.id() + ",financial_event:" + replacement.id(),
                command.reason().trim()
        );

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
        currency(command.currency());
        replacementAmount(command);
        if (command.effectiveAt() == null) {
            throw invalidField("replacementEvent.effectiveAt", "Replacement effective timestamp is required.");
        }
    }

    private String currency(String currency) {
        try {
            return MoneyPolicy.currency(currency);
        } catch (MoneyValidationException exception) {
            throw moneyException("replacementEvent.currency", exception);
        }
    }

    private BigDecimal replacementAmount(ReplacementFinancialEventCommand command) {
        try {
            return MoneyPolicy.positiveAmount(command.amount(), command.currency());
        } catch (MoneyValidationException exception) {
            throw moneyException("replacementEvent.amount", exception);
        }
    }

    private ApiException moneyException(String field, MoneyValidationException exception) {
        return new ApiException(
                HttpStatus.BAD_REQUEST,
                exception.code(),
                exception.reason(),
                List.of(new ApiErrorResponse.ApiErrorDetail(field, exception.reason()))
        );
    }

    private FinancialEventDirection directionFor(FinancialEventType eventType) {
        return switch (eventType) {
            case CUSTOMER_PAYMENT, CHANNEL_SETTLEMENT -> FinancialEventDirection.INCREASE_RECEIVED;
            case REFUND, PAYMENT_REVERSAL, APPROVED_DISCOUNT -> FinancialEventDirection.DECREASE_RECEIVED;
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

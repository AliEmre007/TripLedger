package com.tripledger.finance;

import com.tripledger.identity.ActorContext;
import com.tripledger.identity.ActorContextResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/exchange-rate-evidence")
public class ExchangeRateEvidenceController {

    private final ActorContextResolver actorContextResolver;
    private final ExchangeRateEvidenceService exchangeRateEvidenceService;

    public ExchangeRateEvidenceController(ActorContextResolver actorContextResolver,
                                          ExchangeRateEvidenceService exchangeRateEvidenceService) {
        this.actorContextResolver = actorContextResolver;
        this.exchangeRateEvidenceService = exchangeRateEvidenceService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ExchangeRateEvidenceDetail create(@Valid @RequestBody CreateExchangeRateEvidenceRequest request,
                                             HttpServletRequest servletRequest) {
        ActorContext actor = actorContextResolver.resolve(servletRequest);
        return exchangeRateEvidenceService.create(
                actor,
                new ExchangeRateEvidenceService.CreateExchangeRateEvidenceCommand(
                        request.financialEventId(),
                        request.sourceAmount(),
                        request.sourceCurrency(),
                        request.targetCurrency(),
                        request.rate(),
                        request.effectiveAt(),
                        request.rateSource(),
                        request.roundingPolicyVersion()
                )
        );
    }

    @GetMapping
    public List<ExchangeRateEvidenceDetail> list(@RequestParam(required = false) UUID financialEventId,
                                                 HttpServletRequest servletRequest) {
        ActorContext actor = actorContextResolver.resolve(servletRequest);
        return exchangeRateEvidenceService.list(actor, financialEventId);
    }

    public record CreateExchangeRateEvidenceRequest(
            UUID financialEventId,
            @NotNull BigDecimal sourceAmount,
            @NotBlank String sourceCurrency,
            @NotBlank String targetCurrency,
            @NotNull BigDecimal rate,
            Instant effectiveAt,
            @NotBlank String rateSource,
            @NotBlank String roundingPolicyVersion
    ) {
    }
}

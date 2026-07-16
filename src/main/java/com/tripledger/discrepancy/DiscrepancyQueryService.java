package com.tripledger.discrepancy;

import com.tripledger.authorization.AuthorizationService;
import com.tripledger.authorization.Permission;
import com.tripledger.booking.Booking;
import com.tripledger.booking.BookingRepository;
import com.tripledger.common.api.ApiErrorResponse;
import com.tripledger.common.api.ApiException;
import com.tripledger.identity.ActorContext;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class DiscrepancyQueryService {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 50;
    private static final int MAX_SIZE = 100;

    private final DiscrepancyRepository discrepancyRepository;
    private final BookingRepository bookingRepository;
    private final AuthorizationService authorizationService;
    private final Clock clock;

    public DiscrepancyQueryService(DiscrepancyRepository discrepancyRepository,
                                   BookingRepository bookingRepository,
                                   AuthorizationService authorizationService,
                                   Clock clock) {
        this.discrepancyRepository = discrepancyRepository;
        this.bookingRepository = bookingRepository;
        this.authorizationService = authorizationService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public DiscrepancyListResponse list(ActorContext actor,
                                        String status,
                                        String type,
                                        String severity,
                                        UUID ownerUserId,
                                        String currency,
                                        Integer page,
                                        Integer size) {
        authorizationService.require(actor, Permission.PROTECTED_READ);
        DiscrepancyFilter filter = filter(status, type, severity, ownerUserId, currency, page, size);
        Page<Discrepancy> discrepancies = discrepancyRepository.search(
                actor.organisationId(),
                filter.status(),
                filter.type(),
                filter.severity(),
                filter.ownerUserId(),
                filter.currency(),
                PageRequest.of(filter.page(), filter.size())
        );
        List<Discrepancy> summaryRows = discrepancyRepository.searchForSummary(
                actor.organisationId(),
                filter.status(),
                filter.type(),
                filter.severity(),
                filter.ownerUserId(),
                filter.currency()
        );

        return new DiscrepancyListResponse(
                discrepancies.stream()
                        .map(discrepancy -> DiscrepancySummary.from(discrepancy, ageDays(discrepancy.createdAt())))
                        .toList(),
                summary(summaryRows),
                filter.page(),
                filter.size(),
                discrepancies.getTotalElements(),
                discrepancies.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public DiscrepancyDetail get(ActorContext actor, UUID discrepancyId) {
        authorizationService.require(actor, Permission.PROTECTED_READ);
        Discrepancy discrepancy = discrepancyRepository.findByOrganisationIdAndId(actor.organisationId(), discrepancyId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "DISCREPANCY_NOT_FOUND",
                        "Discrepancy was not found."
                ));
        Booking booking = discrepancy.bookingId() == null
                ? null
                : bookingRepository.findByOrganisationIdAndId(actor.organisationId(), discrepancy.bookingId())
                        .orElse(null);

        return DiscrepancyDetail.from(
                discrepancy,
                ageDays(discrepancy.createdAt()),
                DiscrepancyBookingEvidence.from(booking)
        );
    }

    private DiscrepancyFilter filter(String status,
                                     String type,
                                     String severity,
                                     UUID ownerUserId,
                                     String currency,
                                     Integer page,
                                     Integer size) {
        int resolvedPage = page == null ? DEFAULT_PAGE : page;
        int resolvedSize = size == null ? DEFAULT_SIZE : size;
        if (resolvedPage < 0) {
            throw invalidFilter("page", "Page must be zero or greater.");
        }
        if (resolvedSize < 1 || resolvedSize > MAX_SIZE) {
            throw invalidFilter("size", "Size must be between 1 and " + MAX_SIZE + ".");
        }

        return new DiscrepancyFilter(
                parseEnum(status, DiscrepancyStatus.class, "status"),
                parseEnum(type, DiscrepancyType.class, "type"),
                parseEnum(severity, DiscrepancySeverity.class, "severity"),
                ownerUserId,
                normalizeCurrency(currency),
                resolvedPage,
                resolvedSize
        );
    }

    private DiscrepancyQueueSummary summary(List<Discrepancy> discrepancies) {
        BigDecimal totalAmount = discrepancies.stream()
                .map(Discrepancy::amount)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add);
        long activeCount = discrepancies.stream()
                .filter(discrepancy -> discrepancy.status() == DiscrepancyStatus.ACTIVE)
                .count();
        long resolvedCount = discrepancies.stream()
                .filter(discrepancy -> discrepancy.status() == DiscrepancyStatus.RESOLVED)
                .count();

        return new DiscrepancyQueueSummary(discrepancies.size(), activeCount, resolvedCount, totalAmount);
    }

    private long ageDays(Instant createdAt) {
        return Math.max(0, Duration.between(createdAt, Instant.now(clock)).toDays());
    }

    private String normalizeCurrency(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (!normalized.matches("[A-Z]{3}")) {
            throw invalidFilter("currency", "Currency must be a three-letter ISO code.");
        }
        return normalized;
    }

    private <T extends Enum<T>> T parseEnum(String value, Class<T> enumType, String field) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return Enum.valueOf(enumType, value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw invalidFilter(field, "Unsupported " + field + " filter value.");
        }
    }

    private ApiException invalidFilter(String field, String reason) {
        return new ApiException(
                HttpStatus.BAD_REQUEST,
                "INVALID_REQUEST",
                "Request validation failed.",
                List.of(new ApiErrorResponse.ApiErrorDetail(field, reason))
        );
    }
}

package com.tripledger.discrepancy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tripledger.authorization.AuthorizationService;
import com.tripledger.authorization.Permission;
import com.tripledger.booking.Booking;
import com.tripledger.booking.BookingLifecycleStatus;
import com.tripledger.booking.BookingRepository;
import com.tripledger.common.api.ApiException;
import com.tripledger.identity.ActorContext;
import com.tripledger.identity.UserRole;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class DiscrepancyQueryServiceTest {

    private static final UUID ORGANISATION_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID BOOKING_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID DISCREPANCY_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID SOURCE_SYSTEM_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final Instant NOW = Instant.parse("2026-07-16T12:00:00Z");
    private static final Instant CREATED_AT = Instant.parse("2026-07-14T10:00:00Z");

    @Mock
    private DiscrepancyRepository discrepancyRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private AuthorizationService authorizationService;

    @Test
    void listsFilteredDiscrepanciesWithMatchingSummary() {
        Discrepancy active = shortSettlement(DiscrepancyStatus.ACTIVE, CREATED_AT);
        Discrepancy resolved = new Discrepancy(
                UUID.fromString("66666666-6666-6666-6666-666666666666"),
                ORGANISATION_ID,
                BOOKING_ID,
                DiscrepancyType.SHORT_SETTLEMENT,
                DiscrepancySeverity.HIGH,
                "EXPECTED_CUSTOMER_RECEIVABLE",
                "expected=900.00;matched=875.00;currency=EUR",
                new BigDecimal("25.00"),
                "EUR",
                DiscrepancyStatus.RESOLVED,
                USER_ID,
                "Expected EUR 900.00 but matched EUR 875.00; variance EUR 25.00.",
                CREATED_AT,
                NOW
        );
        when(discrepancyRepository.search(
                ORGANISATION_ID,
                DiscrepancyStatus.ACTIVE,
                DiscrepancyType.SHORT_SETTLEMENT,
                DiscrepancySeverity.HIGH,
                USER_ID,
                "EUR",
                PageRequest.of(0, 20)
        )).thenReturn(new PageImpl<>(List.of(active), PageRequest.of(0, 20), 1));
        when(discrepancyRepository.searchForSummary(
                ORGANISATION_ID,
                DiscrepancyStatus.ACTIVE,
                DiscrepancyType.SHORT_SETTLEMENT,
                DiscrepancySeverity.HIGH,
                USER_ID,
                "EUR"
        )).thenReturn(List.of(active, resolved));

        DiscrepancyListResponse response = service().list(
                actor(),
                "active",
                "short_settlement",
                "high",
                USER_ID,
                "eur",
                0,
                20
        );

        assertThat(response.items()).hasSize(1);
        assertThat(response.items().getFirst().id()).isEqualTo(DISCREPANCY_ID);
        assertThat(response.items().getFirst().ageDays()).isEqualTo(2);
        assertThat(response.summary().totalCount()).isEqualTo(2);
        assertThat(response.summary().activeCount()).isEqualTo(1);
        assertThat(response.summary().resolvedCount()).isEqualTo(1);
        assertThat(response.summary().totalAmount()).isEqualByComparingTo(new BigDecimal("75.00"));
        verify(authorizationService).require(actor(), Permission.PROTECTED_READ);
    }

    @Test
    void returnsDetailWithRelatedBookingEvidence() {
        when(discrepancyRepository.findByOrganisationIdAndId(ORGANISATION_ID, DISCREPANCY_ID))
                .thenReturn(Optional.of(shortSettlement(DiscrepancyStatus.ACTIVE, CREATED_AT)));
        when(bookingRepository.findByOrganisationIdAndId(ORGANISATION_ID, BOOKING_ID))
                .thenReturn(Optional.of(booking()));

        DiscrepancyDetail detail = service().get(actor(), DISCREPANCY_ID);

        assertThat(detail.id()).isEqualTo(DISCREPANCY_ID);
        assertThat(detail.causeIdentity()).isEqualTo("expected=850.00;matched=800.00;currency=EUR");
        assertThat(detail.booking().externalBookingId()).isEqualTo("TL-BKG-1001");
        assertThat(detail.booking().contractedSellingAmount()).isEqualByComparingTo(new BigDecimal("1000.00"));
        verify(authorizationService).require(actor(), Permission.PROTECTED_READ);
    }

    @Test
    void returnsNotFoundForMissingOrWrongOrganisationDiscrepancy() {
        when(discrepancyRepository.findByOrganisationIdAndId(ORGANISATION_ID, DISCREPANCY_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().get(actor(), DISCREPANCY_ID))
                .isInstanceOfSatisfying(ApiException.class, exception -> {
                    assertThat(exception.status()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(exception.code()).isEqualTo("DISCREPANCY_NOT_FOUND");
                });
    }

    @Test
    void rejectsUnsupportedFilterValues() {
        assertThatThrownBy(() -> service().list(actor(), "waiting", null, null, null, null, null, null))
                .isInstanceOfSatisfying(ApiException.class, exception -> {
                    assertThat(exception.status()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(exception.code()).isEqualTo("INVALID_REQUEST");
                    assertThat(exception.details()).hasSize(1);
                    assertThat(exception.details().getFirst().field()).isEqualTo("status");
                });
    }

    private DiscrepancyQueryService service() {
        return new DiscrepancyQueryService(
                discrepancyRepository,
                bookingRepository,
                authorizationService,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    private Discrepancy shortSettlement(DiscrepancyStatus status, Instant createdAt) {
        return new Discrepancy(
                DISCREPANCY_ID,
                ORGANISATION_ID,
                BOOKING_ID,
                DiscrepancyType.SHORT_SETTLEMENT,
                DiscrepancySeverity.HIGH,
                "EXPECTED_CUSTOMER_RECEIVABLE",
                "expected=850.00;matched=800.00;currency=EUR",
                new BigDecimal("50.00"),
                "EUR",
                status,
                USER_ID,
                "Expected EUR 850.00 but matched EUR 800.00; variance EUR 50.00.",
                createdAt,
                null
        );
    }

    private Booking booking() {
        return new Booking(
                BOOKING_ID,
                ORGANISATION_ID,
                SOURCE_SYSTEM_ID,
                "TL-BKG-1001",
                null,
                LocalDate.parse("2026-07-01"),
                LocalDate.parse("2026-08-01"),
                LocalDate.parse("2026-08-07"),
                BookingLifecycleStatus.CONFIRMED,
                "EUR",
                new BigDecimal("1000.00"),
                "CUST-1001",
                CREATED_AT,
                CREATED_AT
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

package com.tripledger.discrepancy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DiscrepancyGenerationServiceTest {

    private static final UUID ORGANISATION_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID BOOKING_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final Instant NOW = Instant.parse("2026-07-14T06:00:00Z");

    @Mock
    private DiscrepancyRepository discrepancyRepository;

    @Test
    void createsShortSettlementDiscrepancyWhenVarianceIsMaterial() {
        when(discrepancyRepository
                .findByOrganisationIdAndBookingIdAndTypeAndComponentAndCauseIdentityAndStatus(
                        any(), any(), any(), any(), any(), any()))
                .thenReturn(Optional.empty());
        when(discrepancyRepository.save(any(Discrepancy.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Discrepancy discrepancy = service().recordShortSettlement(
                ORGANISATION_ID,
                BOOKING_ID,
                new BigDecimal("850.00"),
                new BigDecimal("800.00"),
                "EUR");

        assertThat(discrepancy.type()).isEqualTo(DiscrepancyType.SHORT_SETTLEMENT);
        assertThat(discrepancy.component())
                .isEqualTo(DiscrepancyGenerationService.EXPECTED_RECEIVABLE_COMPONENT);
        assertThat(discrepancy.amount()).isEqualByComparingTo(new BigDecimal("50.00"));
        assertThat(discrepancy.currency()).isEqualTo("EUR");
        assertThat(discrepancy.status()).isEqualTo(DiscrepancyStatus.ACTIVE);
        assertThat(discrepancy.ownerUserId()).isNull();
        assertThat(discrepancy.explanation()).contains("Expected EUR 850.00");
    }

    @Test
    void returnsExistingActiveDiscrepancyInsteadOfCreatingDuplicate() {
        Discrepancy existing = existingShortSettlement();
        when(discrepancyRepository
                .findByOrganisationIdAndBookingIdAndTypeAndComponentAndCauseIdentityAndStatus(
                        any(), any(), any(), any(), any(), any()))
                .thenReturn(Optional.of(existing));

        Discrepancy discrepancy = service().recordShortSettlement(
                ORGANISATION_ID,
                BOOKING_ID,
                new BigDecimal("850.00"),
                new BigDecimal("800.00"),
                "EUR");

        assertThat(discrepancy).isSameAs(existing);
        verify(discrepancyRepository, never()).save(any(Discrepancy.class));
    }

    @Test
    void ignoresNonMaterialShortSettlementVariance() {
        Discrepancy discrepancy = service().recordShortSettlement(
                ORGANISATION_ID,
                BOOKING_ID,
                new BigDecimal("850.00"),
                new BigDecimal("849.50"),
                "EUR");

        assertThat(discrepancy).isNull();
        verify(discrepancyRepository, never()).save(any(Discrepancy.class));
    }

    @Test
    void createsAmbiguousMatchDiscrepancy() {
        when(discrepancyRepository
                .findByOrganisationIdAndBookingIdAndTypeAndComponentAndCauseIdentityAndStatus(
                        any(), any(), any(), any(), any(), any()))
                .thenReturn(Optional.empty());
        when(discrepancyRepository.save(any(Discrepancy.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Discrepancy discrepancy = service().recordAmbiguousMatch(
                ORGANISATION_ID,
                BOOKING_ID,
                "Ambiguous deterministic match requires review.");

        assertThat(discrepancy.type()).isEqualTo(DiscrepancyType.AMBIGUOUS_MATCH);
        assertThat(discrepancy.component()).isEqualTo(DiscrepancyGenerationService.MATCHING_COMPONENT);
        assertThat(discrepancy.causeIdentity()).isEqualTo("AMBIGUOUS_MATCH");
    }

    private DiscrepancyGenerationService service() {
        return new DiscrepancyGenerationService(
                discrepancyRepository,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    private Discrepancy existingShortSettlement() {
        return new Discrepancy(
                UUID.randomUUID(),
                ORGANISATION_ID,
                BOOKING_ID,
                DiscrepancyType.SHORT_SETTLEMENT,
                DiscrepancySeverity.HIGH,
                DiscrepancyGenerationService.EXPECTED_RECEIVABLE_COMPONENT,
                "expected=850.00;matched=800.00;currency=EUR",
                new BigDecimal("50.00"),
                "EUR",
                DiscrepancyStatus.ACTIVE,
                null,
                "Existing discrepancy.",
                NOW,
                null
        );
    }
}

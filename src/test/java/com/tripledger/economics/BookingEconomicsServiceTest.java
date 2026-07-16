package com.tripledger.economics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tripledger.authorization.AuthorizationService;
import com.tripledger.authorization.Permission;
import com.tripledger.booking.Booking;
import com.tripledger.booking.BookingLifecycleStatus;
import com.tripledger.booking.BookingRepository;
import com.tripledger.common.api.ApiException;
import com.tripledger.finance.ExchangeRateEvidenceRepository;
import com.tripledger.finance.FinancialEvent;
import com.tripledger.finance.FinancialEventDirection;
import com.tripledger.finance.FinancialEventRepository;
import com.tripledger.finance.FinancialEventType;
import com.tripledger.identity.ActorContext;
import com.tripledger.identity.UserRole;
import com.tripledger.supplier.SupplierObligation;
import com.tripledger.supplier.SupplierObligationRepository;
import com.tripledger.supplier.SupplierObligationStatus;
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
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class BookingEconomicsServiceTest {

    private static final UUID ORGANISATION_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID BOOKING_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID SOURCE_SYSTEM_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final Instant NOW = Instant.parse("2026-07-14T06:00:00Z");

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private FinancialEventRepository financialEventRepository;

    @Mock
    private SupplierObligationRepository supplierObligationRepository;

    @Mock
    private ExchangeRateEvidenceRepository exchangeRateEvidenceRepository;

    @Mock
    private CalculationSnapshotRepository calculationSnapshotRepository;

    @Mock
    private AuthorizationService authorizationService;

    @Test
    void calculatesNormalBookingEconomics() {
        when(bookingRepository.findByOrganisationIdAndId(ORGANISATION_ID, BOOKING_ID))
                .thenReturn(Optional.of(booking(BookingLifecycleStatus.CONFIRMED, new BigDecimal("1000.00"))));
        when(financialEventRepository.findAllByOrganisationIdAndBookingIdOrderByEffectiveAtAsc(
                ORGANISATION_ID,
                BOOKING_ID))
                .thenReturn(List.of(
                        event(FinancialEventType.APPROVED_DISCOUNT, new BigDecimal("50.00")),
                        event(FinancialEventType.CHANNEL_COMMISSION, new BigDecimal("142.50")),
                        event(FinancialEventType.PAYMENT_FEE, new BigDecimal("20.00"))
                ));
        when(supplierObligationRepository.findAllByOrganisationIdAndBookingIdOrderByCreatedAtAsc(
                ORGANISATION_ID,
                BOOKING_ID))
                .thenReturn(List.of(obligation(new BigDecimal("500.00"), SupplierObligationStatus.CONFIRMED)));
        when(calculationSnapshotRepository.save(any(CalculationSnapshot.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        BookingEconomicsDetail result = service().calculate(actor(), BOOKING_ID);

        assertThat(result.ruleVersion()).isEqualTo("economics-v1");
        assertThat(result.currency()).isEqualTo("EUR");
        assertThat(result.contractedGrossSale()).isEqualByComparingTo(new BigDecimal("1000.00"));
        assertThat(result.expectedCustomerReceivable()).isEqualByComparingTo(new BigDecimal("950.00"));
        assertThat(result.expectedDeductions()).isEqualByComparingTo(new BigDecimal("162.50"));
        assertThat(result.activeSupplierCost()).isEqualByComparingTo(new BigDecimal("500.00"));
        assertThat(result.estimatedGrossMargin()).isEqualByComparingTo(new BigDecimal("287.50"));
        assertThat(result.status()).isEqualTo(CalculationStatus.READY);
        assertThat(result.unknownComponents()).isEmpty();
        verify(authorizationService).require(actor(), Permission.PROTECTED_READ);
    }

    @Test
    void calculatesCancellationRefundReceivableWithoutRewritingOriginalGrossSale() {
        when(bookingRepository.findByOrganisationIdAndId(ORGANISATION_ID, BOOKING_ID))
                .thenReturn(Optional.of(booking(BookingLifecycleStatus.CANCELLED, new BigDecimal("1000.00"))));
        when(financialEventRepository.findAllByOrganisationIdAndBookingIdOrderByEffectiveAtAsc(
                ORGANISATION_ID,
                BOOKING_ID))
                .thenReturn(List.of(event(FinancialEventType.REFUND, new BigDecimal("800.00"))));
        when(supplierObligationRepository.findAllByOrganisationIdAndBookingIdOrderByCreatedAtAsc(
                ORGANISATION_ID,
                BOOKING_ID))
                .thenReturn(List.of(obligation(new BigDecimal("1.00"), SupplierObligationStatus.CANCELLED)));
        when(calculationSnapshotRepository.save(any(CalculationSnapshot.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        BookingEconomicsDetail result = service().calculate(actor(), BOOKING_ID);

        assertThat(result.contractedGrossSale()).isEqualByComparingTo(new BigDecimal("1000.00"));
        assertThat(result.expectedCustomerReceivable()).isEqualByComparingTo(new BigDecimal("200.00"));
    }

    @Test
    void missingSupplierCostProducesNotReadyWithoutInventingZeroMargin() {
        when(bookingRepository.findByOrganisationIdAndId(ORGANISATION_ID, BOOKING_ID))
                .thenReturn(Optional.of(booking(BookingLifecycleStatus.CONFIRMED, new BigDecimal("300.00"))));
        when(financialEventRepository.findAllByOrganisationIdAndBookingIdOrderByEffectiveAtAsc(
                ORGANISATION_ID,
                BOOKING_ID))
                .thenReturn(List.of());
        when(supplierObligationRepository.findAllByOrganisationIdAndBookingIdOrderByCreatedAtAsc(
                ORGANISATION_ID,
                BOOKING_ID))
                .thenReturn(List.of());
        when(calculationSnapshotRepository.save(any(CalculationSnapshot.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        BookingEconomicsDetail result = service().calculate(actor(), BOOKING_ID);

        assertThat(result.activeSupplierCost()).isNull();
        assertThat(result.estimatedGrossMargin()).isNull();
        assertThat(result.status()).isEqualTo(CalculationStatus.NOT_READY);
        assertThat(result.unknownComponents()).containsExactly("ACTIVE_SUPPLIER_COST");
    }

    @Test
    void crossOrganisationBookingReturnsNotFound() {
        when(bookingRepository.findByOrganisationIdAndId(ORGANISATION_ID, BOOKING_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().calculate(actor(), BOOKING_ID))
                .isInstanceOfSatisfying(ApiException.class, exception -> {
                    assertThat(exception.status()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(exception.code()).isEqualTo("BOOKING_NOT_FOUND");
                });
    }

    private BookingEconomicsService service() {
        return new BookingEconomicsService(
                bookingRepository,
                financialEventRepository,
                supplierObligationRepository,
                exchangeRateEvidenceRepository,
                calculationSnapshotRepository,
                authorizationService,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    private Booking booking(BookingLifecycleStatus status, BigDecimal amount) {
        return new Booking(
                BOOKING_ID,
                ORGANISATION_ID,
                SOURCE_SYSTEM_ID,
                "TL-BKG-1001",
                UUID.randomUUID(),
                LocalDate.parse("2026-07-01"),
                LocalDate.parse("2026-08-01"),
                LocalDate.parse("2026-08-07"),
                status,
                "EUR",
                amount,
                "CUST-1001",
                NOW,
                NOW
        );
    }

    private FinancialEvent event(FinancialEventType type, BigDecimal amount) {
        return new FinancialEvent(
                UUID.randomUUID(),
                ORGANISATION_ID,
                UUID.randomUUID(),
                BOOKING_ID,
                type,
                direction(type),
                amount,
                "EUR",
                NOW,
                type.name(),
                null,
                null,
                USER_ID,
                NOW
        );
    }

    private FinancialEventDirection direction(FinancialEventType type) {
        return switch (type) {
            case APPROVED_DISCOUNT, REFUND -> FinancialEventDirection.DECREASE_RECEIVED;
            case CHANNEL_COMMISSION, PAYMENT_FEE -> FinancialEventDirection.INCREASE_DEDUCTION;
            default -> FinancialEventDirection.INCREASE_RECEIVED;
        };
    }

    private SupplierObligation obligation(BigDecimal amount, SupplierObligationStatus status) {
        return new SupplierObligation(
                UUID.randomUUID(),
                ORGANISATION_ID,
                BOOKING_ID,
                null,
                UUID.randomUUID(),
                UUID.randomUUID(),
                amount,
                "EUR",
                LocalDate.parse("2026-08-01"),
                status,
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

package com.tripledger.economics;

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
import com.tripledger.finance.ExchangeRateEvidence;
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
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class BookingEconomicsExplanationServiceTest {

    private static final UUID ORGANISATION_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID BOOKING_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID SOURCE_SYSTEM_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID BOOKING_SOURCE_RECORD_ID =
            UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final UUID EVENT_ID = UUID.fromString("66666666-6666-6666-6666-666666666666");
    private static final UUID EVENT_SOURCE_RECORD_ID = UUID.fromString("77777777-7777-7777-7777-777777777777");
    private static final UUID OBLIGATION_ID = UUID.fromString("88888888-8888-8888-8888-888888888888");
    private static final UUID OBLIGATION_SOURCE_RECORD_ID =
            UUID.fromString("99999999-9999-9999-9999-999999999999");
    private static final UUID EXCHANGE_RATE_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
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
    private AuthorizationService authorizationService;

    @Test
    void explainsFormulasComponentsCurrenciesAndRounding() {
        when(bookingRepository.findByOrganisationIdAndId(ORGANISATION_ID, BOOKING_ID))
                .thenReturn(Optional.of(booking()));
        when(financialEventRepository.findAllByOrganisationIdAndBookingIdOrderByEffectiveAtAsc(
                ORGANISATION_ID,
                BOOKING_ID))
                .thenReturn(List.of(discountEvent()));
        when(exchangeRateEvidenceRepository
                .findAllByOrganisationIdAndFinancialEventIdOrderByEffectiveAtDescCreatedAtDesc(
                        ORGANISATION_ID,
                        EVENT_ID))
                .thenReturn(List.of(exchangeRate()));
        when(supplierObligationRepository.findAllByOrganisationIdAndBookingIdOrderByCreatedAtAsc(
                ORGANISATION_ID,
                BOOKING_ID))
                .thenReturn(List.of(supplierObligation()));

        BookingEconomicsExplanationDetail result = service().explain(actor(), BOOKING_ID);

        assertThat(result.ruleVersion()).isEqualTo("economics-v1");
        assertThat(result.rounding()).contains("HALF_UP");
        assertThat(result.formulas())
                .extracting(BookingEconomicsExplanationDetail.FormulaDetail::ruleReference)
                .contains("BR-ECO-001", "BR-ECO-002", "BR-ECO-003", "BR-ECO-004", "BR-ECO-005");
        assertThat(result.components())
                .extracting(BookingEconomicsExplanationDetail.ComponentDetail::componentType)
                .contains("CONTRACTED_GROSS_SALE", "APPROVED_DISCOUNT", "ACTIVE_SUPPLIER_COST");
        assertThat(result.components())
                .filteredOn(component -> component.componentType().equals("APPROVED_DISCOUNT"))
                .singleElement()
                .satisfies(component -> {
                    assertThat(component.sourceTable()).isEqualTo("financial_event");
                    assertThat(component.sourceRecordId()).isEqualTo(EVENT_SOURCE_RECORD_ID);
                    assertThat(component.originalCurrency()).isEqualTo("TRY");
                    assertThat(component.currency()).isEqualTo("EUR");
                    assertThat(component.exchangeRateId()).isEqualTo(EXCHANGE_RATE_ID);
                });
        assertThat(result.exchangeRates()).singleElement().satisfies(exchangeRate -> {
            assertThat(exchangeRate.id()).isEqualTo(EXCHANGE_RATE_ID);
            assertThat(exchangeRate.sourceCurrency()).isEqualTo("TRY");
            assertThat(exchangeRate.targetCurrency()).isEqualTo("EUR");
            assertThat(exchangeRate.roundingPolicyVersion()).isEqualTo("HALF_UP-v1");
        });
        verify(authorizationService).require(actor(), Permission.PROTECTED_READ);
    }

    @Test
    void crossOrganisationBookingReturnsNotFound() {
        when(bookingRepository.findByOrganisationIdAndId(ORGANISATION_ID, BOOKING_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().explain(actor(), BOOKING_ID))
                .isInstanceOfSatisfying(ApiException.class, exception -> {
                    assertThat(exception.status()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(exception.code()).isEqualTo("BOOKING_NOT_FOUND");
                });
    }

    private BookingEconomicsExplanationService service() {
        return new BookingEconomicsExplanationService(
                bookingRepository,
                financialEventRepository,
                supplierObligationRepository,
                exchangeRateEvidenceRepository,
                authorizationService
        );
    }

    private Booking booking() {
        return new Booking(
                BOOKING_ID,
                ORGANISATION_ID,
                SOURCE_SYSTEM_ID,
                "TL-BKG-1001",
                BOOKING_SOURCE_RECORD_ID,
                LocalDate.parse("2026-07-01"),
                LocalDate.parse("2026-08-01"),
                LocalDate.parse("2026-08-07"),
                BookingLifecycleStatus.CONFIRMED,
                "EUR",
                new BigDecimal("1000.00"),
                "CUST-1001",
                NOW,
                NOW
        );
    }

    private FinancialEvent discountEvent() {
        return new FinancialEvent(
                EVENT_ID,
                ORGANISATION_ID,
                EVENT_SOURCE_RECORD_ID,
                BOOKING_ID,
                FinancialEventType.APPROVED_DISCOUNT,
                FinancialEventDirection.DECREASE_RECEIVED,
                new BigDecimal("3500.00"),
                "TRY",
                NOW,
                "DISCOUNT-TRY-1",
                null,
                null,
                USER_ID,
                NOW
        );
    }

    private ExchangeRateEvidence exchangeRate() {
        return new ExchangeRateEvidence(
                EXCHANGE_RATE_ID,
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
    }

    private SupplierObligation supplierObligation() {
        return new SupplierObligation(
                OBLIGATION_ID,
                ORGANISATION_ID,
                BOOKING_ID,
                null,
                UUID.randomUUID(),
                OBLIGATION_SOURCE_RECORD_ID,
                new BigDecimal("500.00"),
                "EUR",
                LocalDate.parse("2026-08-01"),
                SupplierObligationStatus.CONFIRMED,
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

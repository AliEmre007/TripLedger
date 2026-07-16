package com.tripledger.economics;

import com.tripledger.authorization.AuthorizationService;
import com.tripledger.authorization.Permission;
import com.tripledger.booking.Booking;
import com.tripledger.booking.BookingRepository;
import com.tripledger.common.api.ApiException;
import com.tripledger.finance.ExchangeRateEvidence;
import com.tripledger.finance.ExchangeRateEvidenceRepository;
import com.tripledger.finance.FinancialEvent;
import com.tripledger.finance.FinancialEventRepository;
import com.tripledger.finance.FinancialEventType;
import com.tripledger.identity.ActorContext;
import com.tripledger.supplier.SupplierObligation;
import com.tripledger.supplier.SupplierObligationRepository;
import com.tripledger.supplier.SupplierObligationStatus;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookingEconomicsService {

    static final String RULE_VERSION = "economics-v1";

    private final BookingRepository bookingRepository;
    private final FinancialEventRepository financialEventRepository;
    private final SupplierObligationRepository supplierObligationRepository;
    private final ExchangeRateEvidenceRepository exchangeRateEvidenceRepository;
    private final CalculationSnapshotRepository calculationSnapshotRepository;
    private final AuthorizationService authorizationService;
    private final Clock clock;

    public BookingEconomicsService(BookingRepository bookingRepository,
                                   FinancialEventRepository financialEventRepository,
                                   SupplierObligationRepository supplierObligationRepository,
                                   ExchangeRateEvidenceRepository exchangeRateEvidenceRepository,
                                   CalculationSnapshotRepository calculationSnapshotRepository,
                                   AuthorizationService authorizationService,
                                   Clock clock) {
        this.bookingRepository = bookingRepository;
        this.financialEventRepository = financialEventRepository;
        this.supplierObligationRepository = supplierObligationRepository;
        this.exchangeRateEvidenceRepository = exchangeRateEvidenceRepository;
        this.calculationSnapshotRepository = calculationSnapshotRepository;
        this.authorizationService = authorizationService;
        this.clock = clock;
    }

    @Transactional
    public BookingEconomicsDetail calculate(ActorContext actor, UUID bookingId) {
        authorizationService.require(actor, Permission.PROTECTED_READ);

        Booking booking = bookingRepository.findByOrganisationIdAndId(actor.organisationId(), bookingId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "BOOKING_NOT_FOUND",
                        "Booking was not found."
                ));

        EconomicsAccumulator accumulator = new EconomicsAccumulator(booking.sellingCurrency());
        List<FinancialEvent> events = financialEventRepository
                .findAllByOrganisationIdAndBookingIdOrderByEffectiveAtAsc(actor.organisationId(), booking.id());
        for (FinancialEvent event : events) {
            applyFinancialEvent(accumulator, event);
        }

        BigDecimal activeSupplierCost = activeSupplierCost(actor, booking, accumulator);
        BigDecimal expectedCustomerReceivable = booking.contractedSellingAmount()
                .subtract(accumulator.approvedDiscount)
                .subtract(accumulator.expectedCustomerRefund);
        BigDecimal expectedDeductions = accumulator.expectedChannelCommission.add(accumulator.expectedPaymentFee);
        BigDecimal estimatedGrossMargin = accumulator.unknownComponents.isEmpty()
                ? expectedCustomerReceivable.subtract(expectedDeductions).subtract(activeSupplierCost)
                : null;
        CalculationStatus status = accumulator.unknownComponents.isEmpty()
                ? CalculationStatus.READY
                : CalculationStatus.NOT_READY;

        CalculationSnapshot snapshot = calculationSnapshotRepository.save(new CalculationSnapshot(
                UUID.randomUUID(),
                booking.organisationId(),
                booking.id(),
                RULE_VERSION,
                booking.contractedSellingAmount(),
                expectedCustomerReceivable,
                expectedDeductions,
                activeSupplierCost,
                estimatedGrossMargin,
                booking.sellingCurrency(),
                status,
                unknownComponentsJson(accumulator.unknownComponents),
                Instant.now(clock)
        ));
        return BookingEconomicsDetail.from(snapshot, accumulator.unknownComponents);
    }

    private void applyFinancialEvent(EconomicsAccumulator accumulator, FinancialEvent event) {
        BigDecimal amount = amountInCalculationCurrency(accumulator, event);
        if (amount == null) {
            return;
        }

        if (event.eventType() == FinancialEventType.APPROVED_DISCOUNT) {
            accumulator.approvedDiscount = accumulator.approvedDiscount.add(amount);
        } else if (event.eventType() == FinancialEventType.REFUND) {
            accumulator.expectedCustomerRefund = accumulator.expectedCustomerRefund.add(amount);
        } else if (event.eventType() == FinancialEventType.CHANNEL_COMMISSION) {
            accumulator.expectedChannelCommission = accumulator.expectedChannelCommission.add(amount);
        } else if (event.eventType() == FinancialEventType.PAYMENT_FEE) {
            accumulator.expectedPaymentFee = accumulator.expectedPaymentFee.add(amount);
        }
    }

    private BigDecimal amountInCalculationCurrency(EconomicsAccumulator accumulator, FinancialEvent event) {
        if (event.currency().equals(accumulator.currency)) {
            return event.amount();
        }

        return exchangeRateEvidenceRepository
                .findAllByOrganisationIdAndFinancialEventIdOrderByEffectiveAtDescCreatedAtDesc(
                        event.organisationId(),
                        event.id())
                .stream()
                .filter(evidence -> evidence.sourceCurrency().equals(event.currency()))
                .filter(evidence -> evidence.targetCurrency().equals(accumulator.currency))
                .findFirst()
                .map(ExchangeRateEvidence::targetAmount)
                .orElseGet(() -> {
                    accumulator.addUnknown("EXCHANGE_RATE");
                    return null;
                });
    }

    private BigDecimal activeSupplierCost(ActorContext actor,
                                          Booking booking,
                                          EconomicsAccumulator accumulator) {
        List<SupplierObligation> obligations = supplierObligationRepository
                .findAllByOrganisationIdAndBookingIdOrderByCreatedAtAsc(actor.organisationId(), booking.id());
        if (obligations.isEmpty()) {
            accumulator.addUnknown("ACTIVE_SUPPLIER_COST");
            return null;
        }

        BigDecimal total = BigDecimal.ZERO.setScale(2);
        boolean hasReadyCost = false;
        for (SupplierObligation obligation : obligations) {
            if (obligation.status() == SupplierObligationStatus.EXPECTED) {
                accumulator.addUnknown("ACTIVE_SUPPLIER_COST");
                continue;
            }
            if (!obligation.contributesToActiveSupplierCost()) {
                continue;
            }
            if (!obligation.currency().equals(booking.sellingCurrency())) {
                accumulator.addUnknown("EXCHANGE_RATE");
                continue;
            }
            total = total.add(obligation.amount());
            hasReadyCost = true;
        }

        if (!hasReadyCost) {
            accumulator.addUnknown("ACTIVE_SUPPLIER_COST");
            return null;
        }
        return total;
    }

    private String unknownComponentsJson(List<String> unknownComponents) {
        if (unknownComponents.isEmpty()) {
            return "[]";
        }
        return "[\"" + String.join("\",\"", unknownComponents) + "\"]";
    }

    private static final class EconomicsAccumulator {
        private final String currency;
        private final List<String> unknownComponents = new ArrayList<>();
        private BigDecimal approvedDiscount = BigDecimal.ZERO.setScale(2);
        private BigDecimal expectedCustomerRefund = BigDecimal.ZERO.setScale(2);
        private BigDecimal expectedChannelCommission = BigDecimal.ZERO.setScale(2);
        private BigDecimal expectedPaymentFee = BigDecimal.ZERO.setScale(2);

        private EconomicsAccumulator(String currency) {
            this.currency = currency;
        }

        private void addUnknown(String component) {
            if (!unknownComponents.contains(component)) {
                unknownComponents.add(component);
            }
        }
    }
}

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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookingEconomicsExplanationService {

    private static final String ROUNDING = "HALF_UP to target currency minor unit";

    private final BookingRepository bookingRepository;
    private final FinancialEventRepository financialEventRepository;
    private final SupplierObligationRepository supplierObligationRepository;
    private final ExchangeRateEvidenceRepository exchangeRateEvidenceRepository;
    private final AuthorizationService authorizationService;

    public BookingEconomicsExplanationService(BookingRepository bookingRepository,
                                              FinancialEventRepository financialEventRepository,
                                              SupplierObligationRepository supplierObligationRepository,
                                              ExchangeRateEvidenceRepository exchangeRateEvidenceRepository,
                                              AuthorizationService authorizationService) {
        this.bookingRepository = bookingRepository;
        this.financialEventRepository = financialEventRepository;
        this.supplierObligationRepository = supplierObligationRepository;
        this.exchangeRateEvidenceRepository = exchangeRateEvidenceRepository;
        this.authorizationService = authorizationService;
    }

    @Transactional(readOnly = true)
    public BookingEconomicsExplanationDetail explain(ActorContext actor, UUID bookingId) {
        authorizationService.require(actor, Permission.PROTECTED_READ);
        Booking booking = bookingRepository.findByOrganisationIdAndId(actor.organisationId(), bookingId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "BOOKING_NOT_FOUND",
                        "Booking was not found."
                ));

        ExplanationAccumulator accumulator = new ExplanationAccumulator(booking);
        accumulator.components.add(new BookingEconomicsExplanationDetail.ComponentDetail(
                "CONTRACTED_GROSS_SALE",
                "contractedGrossSale",
                "booking",
                booking.id(),
                booking.currentSourceRecordId(),
                booking.contractedSellingAmount(),
                booking.sellingCurrency(),
                booking.contractedSellingAmount(),
                booking.sellingCurrency(),
                null,
                "BR-ECO-001"
        ));

        financialEventRepository
                .findAllByOrganisationIdAndBookingIdOrderByEffectiveAtAsc(actor.organisationId(), booking.id())
                .forEach(accumulator::addFinancialEvent);
        supplierObligationRepository
                .findAllByOrganisationIdAndBookingIdOrderByCreatedAtAsc(actor.organisationId(), booking.id())
                .forEach(accumulator::addSupplierObligation);

        return new BookingEconomicsExplanationDetail(
                booking.id(),
                BookingEconomicsService.RULE_VERSION,
                booking.sellingCurrency(),
                formulas(),
                accumulator.components,
                accumulator.exchangeRates,
                ROUNDING
        );
    }

    private List<BookingEconomicsExplanationDetail.FormulaDetail> formulas() {
        return List.of(
                new BookingEconomicsExplanationDetail.FormulaDetail(
                        "contractedGrossSale",
                        "sum(original active and historically contracted booking-item selling amounts)",
                        "BR-ECO-001"
                ),
                new BookingEconomicsExplanationDetail.FormulaDetail(
                        "expectedCustomerReceivable",
                        "contractedGrossSale - approvedDiscounts - expectedCustomerRefunds - waivedAmounts",
                        "BR-ECO-002"
                ),
                new BookingEconomicsExplanationDetail.FormulaDetail(
                        "expectedDeductions",
                        "expectedChannelCommissions + expectedPaymentFees",
                        "BR-ECO-003"
                ),
                new BookingEconomicsExplanationDetail.FormulaDetail(
                        "activeSupplierCost",
                        "confirmedOrInvoicedObligations - supplierCredits - cancelledObligations",
                        "BR-ECO-004"
                ),
                new BookingEconomicsExplanationDetail.FormulaDetail(
                        "estimatedGrossMargin",
                        "expectedCustomerReceivable - expectedDeductions - activeSupplierCost",
                        "BR-ECO-005"
                )
        );
    }

    private final class ExplanationAccumulator {
        private final Booking booking;
        private final List<BookingEconomicsExplanationDetail.ComponentDetail> components = new ArrayList<>();
        private final List<BookingEconomicsExplanationDetail.ExchangeRateDetail> exchangeRates = new ArrayList<>();

        private ExplanationAccumulator(Booking booking) {
            this.booking = booking;
        }

        private void addFinancialEvent(FinancialEvent event) {
            ComponentMapping mapping = componentMapping(event.eventType());
            if (mapping == null) {
                return;
            }

            ConvertedAmount converted = convert(event);
            components.add(new BookingEconomicsExplanationDetail.ComponentDetail(
                    mapping.componentType(),
                    mapping.subtotal(),
                    "financial_event",
                    event.id(),
                    event.sourceRecordId(),
                    event.amount(),
                    event.currency(),
                    converted.amount(),
                    converted.currency(),
                    converted.exchangeRateId(),
                    mapping.ruleReference()
            ));
        }

        private void addSupplierObligation(SupplierObligation obligation) {
            if (obligation.status() == SupplierObligationStatus.EXPECTED) {
                components.add(supplierComponent("UNKNOWN_ACTIVE_SUPPLIER_COST", obligation, null, null, null));
                return;
            }
            if (!obligation.contributesToActiveSupplierCost()) {
                return;
            }
            if (!obligation.currency().equals(booking.sellingCurrency())) {
                components.add(supplierComponent("MISSING_EXCHANGE_RATE", obligation, null, null, null));
                return;
            }
            components.add(supplierComponent(
                    "ACTIVE_SUPPLIER_COST",
                    obligation,
                    obligation.amount(),
                    obligation.currency(),
                    null
            ));
        }

        private BookingEconomicsExplanationDetail.ComponentDetail supplierComponent(String componentType,
                                                                                   SupplierObligation obligation,
                                                                                   BigDecimal amount,
                                                                                   String currency,
                                                                                   UUID exchangeRateId) {
            return new BookingEconomicsExplanationDetail.ComponentDetail(
                    componentType,
                    "activeSupplierCost",
                    "supplier_obligation",
                    obligation.id(),
                    obligation.sourceRecordId(),
                    obligation.amount(),
                    obligation.currency(),
                    amount,
                    currency,
                    exchangeRateId,
                    "BR-ECO-004"
            );
        }

        private ConvertedAmount convert(FinancialEvent event) {
            if (event.currency().equals(booking.sellingCurrency())) {
                return new ConvertedAmount(event.amount(), event.currency(), null);
            }

            return exchangeRateEvidenceRepository
                    .findAllByOrganisationIdAndFinancialEventIdOrderByEffectiveAtDescCreatedAtDesc(
                            event.organisationId(),
                            event.id())
                    .stream()
                    .filter(evidence -> evidence.sourceCurrency().equals(event.currency()))
                    .filter(evidence -> evidence.targetCurrency().equals(booking.sellingCurrency()))
                    .findFirst()
                    .map(evidence -> {
                        exchangeRates.add(toExchangeRateDetail(evidence));
                        return new ConvertedAmount(
                                evidence.targetAmount(),
                                evidence.targetCurrency(),
                                evidence.id()
                        );
                    })
                    .orElseGet(() -> new ConvertedAmount(null, null, null));
        }

        private BookingEconomicsExplanationDetail.ExchangeRateDetail toExchangeRateDetail(
                ExchangeRateEvidence evidence) {
            return new BookingEconomicsExplanationDetail.ExchangeRateDetail(
                    evidence.id(),
                    evidence.financialEventId(),
                    evidence.sourceAmount(),
                    evidence.sourceCurrency(),
                    evidence.targetAmount(),
                    evidence.targetCurrency(),
                    evidence.rate(),
                    evidence.effectiveAt(),
                    evidence.rateSource(),
                    evidence.roundingPolicyVersion()
            );
        }

        private ComponentMapping componentMapping(FinancialEventType eventType) {
            return switch (eventType) {
                case APPROVED_DISCOUNT -> new ComponentMapping(
                        "APPROVED_DISCOUNT",
                        "expectedCustomerReceivable",
                        "BR-ECO-002"
                );
                case REFUND -> new ComponentMapping(
                        "EXPECTED_CUSTOMER_REFUND",
                        "expectedCustomerReceivable",
                        "BR-ECO-002"
                );
                case CHANNEL_COMMISSION -> new ComponentMapping(
                        "EXPECTED_CHANNEL_COMMISSION",
                        "expectedDeductions",
                        "BR-ECO-003"
                );
                case PAYMENT_FEE -> new ComponentMapping(
                        "EXPECTED_PAYMENT_FEE",
                        "expectedDeductions",
                        "BR-ECO-003"
                );
                default -> null;
            };
        }
    }

    private record ConvertedAmount(BigDecimal amount, String currency, UUID exchangeRateId) {
    }

    private record ComponentMapping(String componentType, String subtotal, String ruleReference) {
    }
}

package com.tripledger.timeline;

import com.tripledger.audit.AuditEvent;
import com.tripledger.audit.AuditEventRepository;
import com.tripledger.audit.AuditService;
import com.tripledger.authorization.AuthorizationService;
import com.tripledger.authorization.Permission;
import com.tripledger.booking.Booking;
import com.tripledger.booking.BookingRepository;
import com.tripledger.common.api.ApiException;
import com.tripledger.discrepancy.Discrepancy;
import com.tripledger.discrepancy.DiscrepancyRepository;
import com.tripledger.economics.CalculationSnapshot;
import com.tripledger.economics.CalculationSnapshotRepository;
import com.tripledger.finance.FinancialEvent;
import com.tripledger.finance.FinancialEventRepository;
import com.tripledger.identity.ActorContext;
import com.tripledger.ingestion.SourceRecord;
import com.tripledger.ingestion.SourceRecordRepository;
import com.tripledger.matching.BookingMatch;
import com.tripledger.matching.BookingMatchRepository;
import com.tripledger.reconciliation.ReconciliationResult;
import com.tripledger.reconciliation.ReconciliationResultRepository;
import com.tripledger.supplier.SupplierObligation;
import com.tripledger.supplier.SupplierObligationRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookingTimelineService {

    private final BookingRepository bookingRepository;
    private final SourceRecordRepository sourceRecordRepository;
    private final SupplierObligationRepository supplierObligationRepository;
    private final FinancialEventRepository financialEventRepository;
    private final CalculationSnapshotRepository calculationSnapshotRepository;
    private final BookingMatchRepository bookingMatchRepository;
    private final ReconciliationResultRepository reconciliationResultRepository;
    private final DiscrepancyRepository discrepancyRepository;
    private final AuditEventRepository auditEventRepository;
    private final AuthorizationService authorizationService;

    public BookingTimelineService(BookingRepository bookingRepository,
                                  SourceRecordRepository sourceRecordRepository,
                                  SupplierObligationRepository supplierObligationRepository,
                                  FinancialEventRepository financialEventRepository,
                                  CalculationSnapshotRepository calculationSnapshotRepository,
                                  BookingMatchRepository bookingMatchRepository,
                                  ReconciliationResultRepository reconciliationResultRepository,
                                  DiscrepancyRepository discrepancyRepository,
                                  AuditEventRepository auditEventRepository,
                                  AuthorizationService authorizationService) {
        this.bookingRepository = bookingRepository;
        this.sourceRecordRepository = sourceRecordRepository;
        this.supplierObligationRepository = supplierObligationRepository;
        this.financialEventRepository = financialEventRepository;
        this.calculationSnapshotRepository = calculationSnapshotRepository;
        this.bookingMatchRepository = bookingMatchRepository;
        this.reconciliationResultRepository = reconciliationResultRepository;
        this.discrepancyRepository = discrepancyRepository;
        this.auditEventRepository = auditEventRepository;
        this.authorizationService = authorizationService;
    }

    @Transactional(readOnly = true)
    public BookingTimeline get(ActorContext actor, UUID bookingId) {
        authorizationService.require(actor, Permission.PROTECTED_READ);
        Booking booking = bookingRepository.findByOrganisationIdAndId(actor.organisationId(), bookingId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "BOOKING_NOT_FOUND",
                        "Booking was not found."
                ));

        List<TimelineEvent> events = new ArrayList<>();
        events.add(bookingImported(booking));
        sourceRecordRepository.findBookingTimelineRecords(
                        actor.organisationId(),
                        booking.id(),
                        booking.currentSourceRecordId())
                .forEach(record -> events.add(sourceRecordAccepted(record)));
        supplierObligationRepository.findAllByOrganisationIdAndBookingIdOrderByCreatedAtAsc(
                        actor.organisationId(),
                        booking.id())
                .forEach(obligation -> events.add(supplierObligationCreated(obligation)));
        financialEventRepository.findAllByOrganisationIdAndBookingIdOrderByEffectiveAtAsc(
                        actor.organisationId(),
                        booking.id())
                .forEach(event -> events.add(financialEventCreated(event)));
        calculationSnapshotRepository.findAllByOrganisationIdAndBookingIdOrderByCreatedAtAsc(
                        actor.organisationId(),
                        booking.id())
                .forEach(snapshot -> events.add(calculationCreated(snapshot)));
        bookingMatchRepository.findAllByOrganisationIdAndBookingIdOrderByCreatedAtAsc(
                        actor.organisationId(),
                        booking.id())
                .forEach(match -> events.add(matchCreated(match)));
        reconciliationResultRepository.findAllByOrganisationIdAndBookingIdOrderByCreatedAtAsc(
                        actor.organisationId(),
                        booking.id())
                .forEach(result -> events.add(reconciliationCreated(result)));
        discrepancyRepository.findAllByOrganisationIdAndBookingIdOrderByCreatedAtAsc(
                        actor.organisationId(),
                        booking.id())
                .forEach(discrepancy -> events.add(discrepancyCreated(discrepancy)));
        auditEventRepository.findAllByOrganisationIdAndTargetTypeAndTargetIdOrderByCreatedAtAsc(
                        actor.organisationId(),
                        AuditService.TARGET_BOOKING,
                        booking.id())
                .forEach(auditEvent -> events.add(auditEvent(auditEvent)));

        return new BookingTimeline(
                booking.id(),
                booking.organisationId(),
                events.stream()
                        .sorted(Comparator.comparing(TimelineEvent::occurredAt).thenComparing(TimelineEvent::eventType))
                        .toList()
        );
    }

    private TimelineEvent bookingImported(Booking booking) {
        return new TimelineEvent(
                booking.id(),
                booking.createdAt(),
                TimelineEventCategory.SOURCE,
                "BOOKING_IMPORTED",
                "Booking imported",
                "Booking " + booking.externalBookingId() + " accepted as canonical booking.",
                "BOOKING",
                booking.id(),
                null,
                booking.contractedSellingAmount(),
                booking.sellingCurrency(),
                booking.lifecycleStatus().name(),
                reference("booking", booking.id())
        );
    }

    private TimelineEvent sourceRecordAccepted(SourceRecord record) {
        return new TimelineEvent(
                record.id(),
                record.acceptedAt(),
                TimelineEventCategory.SOURCE,
                "SOURCE_RECORD_ACCEPTED",
                "Source record accepted",
                record.recordType() + " " + record.externalRecordId() + " version " + record.sourceVersion()
                        + " accepted from row " + record.sourceRowNumber() + ".",
                "SOURCE_RECORD",
                record.id(),
                null,
                null,
                null,
                record.recordType(),
                reference("source_record", record.id())
        );
    }

    private TimelineEvent supplierObligationCreated(SupplierObligation obligation) {
        return new TimelineEvent(
                obligation.id(),
                obligation.createdAt(),
                TimelineEventCategory.SOURCE,
                "SUPPLIER_OBLIGATION_ACCEPTED",
                "Supplier obligation accepted",
                "Supplier obligation " + money(obligation.amount(), obligation.currency()) + " recorded.",
                "SUPPLIER_OBLIGATION",
                obligation.id(),
                null,
                obligation.amount(),
                obligation.currency(),
                obligation.status().name(),
                reference("supplier_obligation", obligation.id())
        );
    }

    private TimelineEvent financialEventCreated(FinancialEvent event) {
        TimelineEventCategory category = event.createdByUserId() == null
                ? TimelineEventCategory.SOURCE
                : TimelineEventCategory.USER;
        return new TimelineEvent(
                event.id(),
                event.createdAt(),
                category,
                "FINANCIAL_EVENT_ACCEPTED",
                "Financial event accepted",
                event.eventType().name() + " " + money(event.amount(), event.currency()) + " accepted.",
                "FINANCIAL_EVENT",
                event.id(),
                event.createdByUserId(),
                event.amount(),
                event.currency(),
                event.eventType().name(),
                reference("financial_event", event.id())
        );
    }

    private TimelineEvent calculationCreated(CalculationSnapshot snapshot) {
        return new TimelineEvent(
                snapshot.id(),
                snapshot.createdAt(),
                TimelineEventCategory.SYSTEM,
                "ECONOMICS_CALCULATED",
                "Economics calculated",
                "Expected receivable " + money(snapshot.expectedCustomerReceivable(), snapshot.currency())
                        + " using " + snapshot.ruleVersion() + ".",
                "CALCULATION_SNAPSHOT",
                snapshot.id(),
                null,
                snapshot.expectedCustomerReceivable(),
                snapshot.currency(),
                snapshot.status().name(),
                reference("calculation_snapshot", snapshot.id())
        );
    }

    private TimelineEvent matchCreated(BookingMatch match) {
        return new TimelineEvent(
                match.id(),
                match.createdAt(),
                TimelineEventCategory.SYSTEM,
                "MATCH_EVALUATED",
                "Match evaluated",
                "Match rule " + match.ruleCode() + " produced " + match.status().name() + ".",
                "BOOKING_MATCH",
                match.id(),
                match.createdByUserId(),
                null,
                null,
                match.status().name(),
                reference("booking_match", match.id())
        );
    }

    private TimelineEvent reconciliationCreated(ReconciliationResult result) {
        return new TimelineEvent(
                result.id(),
                result.createdAt(),
                TimelineEventCategory.SYSTEM,
                "RECONCILIATION_RECORDED",
                "Reconciliation recorded",
                "Reconciliation status " + result.status().name() + " with variance "
                        + money(result.varianceAmount(), result.varianceCurrency()) + ".",
                "RECONCILIATION_RESULT",
                result.id(),
                null,
                result.varianceAmount(),
                result.varianceCurrency(),
                result.status().name(),
                reference("reconciliation_result", result.id())
        );
    }

    private TimelineEvent discrepancyCreated(Discrepancy discrepancy) {
        return new TimelineEvent(
                discrepancy.id(),
                discrepancy.createdAt(),
                TimelineEventCategory.SYSTEM,
                "DISCREPANCY_RECORDED",
                "Discrepancy recorded",
                discrepancy.explanation(),
                "DISCREPANCY",
                discrepancy.id(),
                discrepancy.ownerUserId(),
                discrepancy.amount(),
                discrepancy.currency(),
                discrepancy.status().name(),
                reference("discrepancy", discrepancy.id())
        );
    }

    private TimelineEvent auditEvent(AuditEvent auditEvent) {
        return new TimelineEvent(
                auditEvent.id(),
                auditEvent.createdAt(),
                TimelineEventCategory.USER,
                "AUDIT_EVENT_RECORDED",
                "Audit event recorded",
                auditEvent.action() + " completed with " + auditEvent.outcome().name() + ".",
                "AUDIT_EVENT",
                auditEvent.id(),
                auditEvent.actorUserId(),
                null,
                null,
                auditEvent.outcome().name(),
                reference("audit_event", auditEvent.id())
        );
    }

    private String money(BigDecimal amount, String currency) {
        if (amount == null || currency == null) {
            return "UNKNOWN";
        }
        return currency + " " + amount;
    }

    private String reference(String table, UUID id) {
        return table + ":" + id;
    }
}

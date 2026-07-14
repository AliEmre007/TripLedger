package com.tripledger.finance;

import com.tripledger.authorization.AuthorizationService;
import com.tripledger.authorization.Permission;
import com.tripledger.booking.SourceRecordDetail;
import com.tripledger.identity.ActorContext;
import com.tripledger.ingestion.SourceRecord;
import com.tripledger.ingestion.SourceRecordRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FinancialEventQueryService {

    private final FinancialEventRepository financialEventRepository;
    private final SourceRecordRepository sourceRecordRepository;
    private final AuthorizationService authorizationService;

    public FinancialEventQueryService(FinancialEventRepository financialEventRepository,
                                      SourceRecordRepository sourceRecordRepository,
                                      AuthorizationService authorizationService) {
        this.financialEventRepository = financialEventRepository;
        this.sourceRecordRepository = sourceRecordRepository;
        this.authorizationService = authorizationService;
    }

    @Transactional(readOnly = true)
    public List<FinancialEventDetail> list(ActorContext actor, Boolean unmatched) {
        authorizationService.require(actor, Permission.PROTECTED_READ);
        List<FinancialEvent> events = Boolean.TRUE.equals(unmatched)
                ? financialEventRepository.findAllByOrganisationIdAndBookingIdIsNullOrderByEffectiveAtDesc(
                        actor.organisationId())
                : financialEventRepository.findAllByOrganisationIdOrderByEffectiveAtDesc(actor.organisationId());

        return events.stream()
                .map(this::toDetail)
                .toList();
    }

    private FinancialEventDetail toDetail(FinancialEvent event) {
        SourceRecord sourceRecord = event.sourceRecordId() == null
                ? null
                : sourceRecordRepository.findByOrganisationIdAndId(
                        event.organisationId(),
                        event.sourceRecordId()).orElse(null);
        return FinancialEventDetail.from(event, SourceRecordDetail.from(sourceRecord));
    }
}

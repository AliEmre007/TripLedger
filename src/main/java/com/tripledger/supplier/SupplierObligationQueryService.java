package com.tripledger.supplier;

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
public class SupplierObligationQueryService {

    private final SupplierObligationRepository supplierObligationRepository;
    private final SupplierRepository supplierRepository;
    private final SourceRecordRepository sourceRecordRepository;
    private final AuthorizationService authorizationService;

    public SupplierObligationQueryService(SupplierObligationRepository supplierObligationRepository,
                                          SupplierRepository supplierRepository,
                                          SourceRecordRepository sourceRecordRepository,
                                          AuthorizationService authorizationService) {
        this.supplierObligationRepository = supplierObligationRepository;
        this.supplierRepository = supplierRepository;
        this.sourceRecordRepository = sourceRecordRepository;
        this.authorizationService = authorizationService;
    }

    @Transactional(readOnly = true)
    public List<SupplierObligationDetail> list(ActorContext actor, Boolean unlinked) {
        authorizationService.require(actor, Permission.PROTECTED_READ);
        List<SupplierObligation> obligations = Boolean.TRUE.equals(unlinked)
                ? supplierObligationRepository
                        .findAllByOrganisationIdAndBookingIdIsNullAndBookingItemIdIsNullOrderByCreatedAtDesc(
                                actor.organisationId())
                : supplierObligationRepository.findAllByOrganisationIdOrderByCreatedAtDesc(actor.organisationId());

        return obligations.stream()
                .map(this::toDetail)
                .toList();
    }

    private SupplierObligationDetail toDetail(SupplierObligation obligation) {
        Supplier supplier = obligation.supplierId() == null
                ? null
                : supplierRepository.findByOrganisationIdAndId(
                        obligation.organisationId(),
                        obligation.supplierId()).orElse(null);
        SourceRecord sourceRecord = obligation.sourceRecordId() == null
                ? null
                : sourceRecordRepository.findByOrganisationIdAndId(
                        obligation.organisationId(),
                        obligation.sourceRecordId()).orElse(null);
        return SupplierObligationDetail.from(obligation, supplier, SourceRecordDetail.from(sourceRecord));
    }
}

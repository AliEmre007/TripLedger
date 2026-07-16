package com.tripledger.ingestion;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SourceRecordRepository extends JpaRepository<SourceRecord, UUID> {

    Optional<SourceRecord> findByOrganisationIdAndId(UUID organisationId, UUID id);

    Optional<SourceRecord> findByOrganisationIdAndSourceSystemIdAndRecordTypeAndExternalRecordIdAndSourceVersion(
            UUID organisationId,
            UUID sourceSystemId,
            String recordType,
            String externalRecordId,
            String sourceVersion
    );

    @Query("""
            select sr
            from SourceRecord sr
            where sr.organisationId = :organisationId
              and (
                    sr.id = :currentSourceRecordId
                    or sr.id in (
                        select fe.sourceRecordId
                        from FinancialEvent fe
                        where fe.organisationId = :organisationId
                          and fe.bookingId = :bookingId
                          and fe.sourceRecordId is not null
                    )
                    or sr.id in (
                        select so.sourceRecordId
                        from SupplierObligation so
                        where so.organisationId = :organisationId
                          and so.bookingId = :bookingId
                          and so.sourceRecordId is not null
                    )
                  )
            order by sr.acceptedAt asc, sr.id asc
            """)
    java.util.List<SourceRecord> findBookingTimelineRecords(
            UUID organisationId,
            UUID bookingId,
            UUID currentSourceRecordId
    );
}

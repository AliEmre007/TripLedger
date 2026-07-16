package com.tripledger.discrepancy;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface DiscrepancyRepository extends JpaRepository<Discrepancy, UUID> {

    @Query("""
            select d
            from Discrepancy d
            where d.organisationId = :organisationId
              and (:status is null or d.status = :status)
              and (:type is null or d.type = :type)
              and (:severity is null or d.severity = :severity)
              and (:ownerUserId is null or d.ownerUserId = :ownerUserId)
              and (:currency is null or d.currency = :currency)
            order by d.createdAt desc, d.id asc
            """)
    Page<Discrepancy> search(
            UUID organisationId,
            DiscrepancyStatus status,
            DiscrepancyType type,
            DiscrepancySeverity severity,
            UUID ownerUserId,
            String currency,
            Pageable pageable
    );

    @Query("""
            select d
            from Discrepancy d
            where d.organisationId = :organisationId
              and (:status is null or d.status = :status)
              and (:type is null or d.type = :type)
              and (:severity is null or d.severity = :severity)
              and (:ownerUserId is null or d.ownerUserId = :ownerUserId)
              and (:currency is null or d.currency = :currency)
            """)
    List<Discrepancy> searchForSummary(
            UUID organisationId,
            DiscrepancyStatus status,
            DiscrepancyType type,
            DiscrepancySeverity severity,
            UUID ownerUserId,
            String currency
    );

    Optional<Discrepancy> findByOrganisationIdAndId(UUID organisationId, UUID id);

    List<Discrepancy> findAllByOrganisationIdAndBookingIdOrderByCreatedAtAsc(UUID organisationId, UUID bookingId);

    Optional<Discrepancy>
            findByOrganisationIdAndBookingIdAndTypeAndComponentAndCauseIdentityAndStatus(
                    UUID organisationId,
                    UUID bookingId,
                    DiscrepancyType type,
                    String component,
                    String causeIdentity,
                    DiscrepancyStatus status
            );
}

package com.tripledger.economics;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CalculationSnapshotRepository extends JpaRepository<CalculationSnapshot, UUID> {
}

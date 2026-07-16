package com.tripledger.health;

import com.tripledger.operations.BackgroundJobRepository;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

@Service
public class ReadinessService {

    private final DataSource dataSource;
    private final BackgroundJobRepository backgroundJobRepository;
    private final Flyway flyway;

    public ReadinessService(DataSource dataSource,
                            BackgroundJobRepository backgroundJobRepository,
                            ObjectProvider<Flyway> flyway) {
        this.dataSource = dataSource;
        this.backgroundJobRepository = backgroundJobRepository;
        this.flyway = flyway.getIfAvailable();
    }

    public ReadinessStatus readiness() {
        List<ReadinessStatus.ReadinessCheck> checks = new ArrayList<>();
        checks.add(databaseCheck());
        checks.add(migrationCheck());
        checks.add(jobStoreCheck());

        String status = checks.stream().allMatch(check -> "UP".equals(check.status())) ? "UP" : "DOWN";
        return new ReadinessStatus(status, checks);
    }

    private ReadinessStatus.ReadinessCheck databaseCheck() {
        try (Connection connection = dataSource.getConnection()) {
            if (connection.isValid(1)) {
                return up("database", "Database connection is valid.");
            }
            return down("database", "Database connection validation failed.");
        } catch (Exception exception) {
            return down("database", "Database is not reachable.");
        }
    }

    private ReadinessStatus.ReadinessCheck migrationCheck() {
        if (flyway == null) {
            return up("migrations", "Flyway is not available in this context.");
        }

        try {
            MigrationInfo current = flyway.info().current();
            if (current == null) {
                return down("migrations", "No applied migration was found.");
            }
            return up("migrations", "Current migration is " + current.getVersion() + ".");
        } catch (Exception exception) {
            return down("migrations", "Migration state could not be read.");
        }
    }

    private ReadinessStatus.ReadinessCheck jobStoreCheck() {
        try {
            backgroundJobRepository.count();
            return up("background_jobs", "Background job store is reachable.");
        } catch (Exception exception) {
            return down("background_jobs", "Background job store is not reachable.");
        }
    }

    private ReadinessStatus.ReadinessCheck up(String name, String detail) {
        return new ReadinessStatus.ReadinessCheck(name, "UP", detail);
    }

    private ReadinessStatus.ReadinessCheck down(String name, String detail) {
        return new ReadinessStatus.ReadinessCheck(name, "DOWN", detail);
    }
}

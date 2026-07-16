package com.tripledger.health;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.tripledger.operations.BackgroundJobRepository;
import java.sql.Connection;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

@ExtendWith(MockitoExtension.class)
class ReadinessServiceTest {

    @Mock
    private DataSource dataSource;

    @Mock
    private Connection connection;

    @Mock
    private BackgroundJobRepository backgroundJobRepository;

    @Mock
    private ObjectProvider<Flyway> flywayProvider;

    @Test
    void reportsUpWhenDatabaseAndJobStoreAreReachable() throws Exception {
        when(flywayProvider.getIfAvailable()).thenReturn(null);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.isValid(1)).thenReturn(true);
        when(backgroundJobRepository.count()).thenReturn(0L);

        ReadinessStatus readiness = service().readiness();

        assertThat(readiness.status()).isEqualTo("UP");
        assertThat(readiness.checks())
                .extracting(ReadinessStatus.ReadinessCheck::name)
                .containsExactly("database", "migrations", "background_jobs");
        assertThat(readiness.checks())
                .extracting(ReadinessStatus.ReadinessCheck::status)
                .containsExactly("UP", "UP", "UP");
    }

    @Test
    void reportsDownWhenDatabaseIsNotReachable() throws Exception {
        when(flywayProvider.getIfAvailable()).thenReturn(null);
        when(dataSource.getConnection()).thenThrow(new IllegalStateException("database down"));
        when(backgroundJobRepository.count()).thenReturn(0L);

        ReadinessStatus readiness = service().readiness();

        assertThat(readiness.status()).isEqualTo("DOWN");
        assertThat(readiness.checks().getFirst().name()).isEqualTo("database");
        assertThat(readiness.checks().getFirst().status()).isEqualTo("DOWN");
    }

    @Test
    void reportsDownWhenBackgroundJobStoreIsNotReachable() throws Exception {
        when(flywayProvider.getIfAvailable()).thenReturn(null);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.isValid(1)).thenReturn(true);
        when(backgroundJobRepository.count()).thenThrow(new IllegalStateException("job store down"));

        ReadinessStatus readiness = service().readiness();

        assertThat(readiness.status()).isEqualTo("DOWN");
        assertThat(readiness.checks().getLast().name()).isEqualTo("background_jobs");
        assertThat(readiness.checks().getLast().status()).isEqualTo("DOWN");
    }

    private ReadinessService service() {
        return new ReadinessService(dataSource, backgroundJobRepository, flywayProvider);
    }
}

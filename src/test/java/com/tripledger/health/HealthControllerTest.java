package com.tripledger.health;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.Mockito.when;

@WebMvcTest(HealthController.class)
class HealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReadinessService readinessService;

    @Test
    void healthReturnsServiceStatusAndCorrelationId() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Correlation-Id", not(blankOrNullString())))
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("tripledger"));
    }

    @Test
    void livenessReturnsProcessHealthWithoutDependencyChecks() throws Exception {
        mockMvc.perform(get("/api/v1/health/live"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Correlation-Id", not(blankOrNullString())))
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("tripledger"));
    }

    @Test
    void readinessReturnsOkWhenDependenciesAreReady() throws Exception {
        when(readinessService.readiness()).thenReturn(new ReadinessStatus(
                "UP",
                List.of(new ReadinessStatus.ReadinessCheck("database", "UP", "Database connection is valid."))
        ));

        mockMvc.perform(get("/api/v1/health/ready"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Correlation-Id", not(blankOrNullString())))
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.checks[0].name").value("database"))
                .andExpect(jsonPath("$.checks[0].status").value("UP"));
    }

    @Test
    void readinessReturnsUnavailableWhenDependencyIsDown() throws Exception {
        when(readinessService.readiness()).thenReturn(new ReadinessStatus(
                "DOWN",
                List.of(new ReadinessStatus.ReadinessCheck("database", "DOWN", "Database is not reachable."))
        ));

        mockMvc.perform(get("/api/v1/health/ready"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(header().string("X-Correlation-Id", not(blankOrNullString())))
                .andExpect(jsonPath("$.status").value("DOWN"))
                .andExpect(jsonPath("$.checks[0].name").value("database"))
                .andExpect(jsonPath("$.checks[0].status").value("DOWN"));
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        BuildProperties buildProperties() {
            return new BuildProperties(new java.util.Properties());
        }
    }
}

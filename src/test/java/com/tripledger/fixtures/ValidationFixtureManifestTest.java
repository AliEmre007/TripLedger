package com.tripledger.fixtures;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class ValidationFixtureManifestTest {

    private static final String FIXTURE_ROOT = "fixtures/validation-release";
    private static final Set<String> REQUIRED_ACCEPTANCE_CRITERIA = Set.of(
            "AC-011",
            "AC-012",
            "AC-013",
            "AC-014",
            "AC-015",
            "AC-016",
            "AC-017",
            "AC-018",
            "AC-019",
            "AC-020",
            "AC-021",
            "AC-033",
            "AC-034",
            "AC-035",
            "AC-036",
            "AC-040",
            "AC-041",
            "AC-042");

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void manifestCoversRequiredAcceptanceCriteria() throws IOException {
        FixtureManifest manifest = readManifest();

        assertThat(manifest.slice()).isEqualTo("VR-001");
        assertThat(manifest.requiredAcceptanceCriteria())
                .containsExactlyInAnyOrderElementsOf(REQUIRED_ACCEPTANCE_CRITERIA);

        Set<String> coveredCriteria = manifest.fixtures().stream()
                .flatMap(fixture -> fixture.covers().stream())
                .collect(Collectors.toSet());

        assertThat(coveredCriteria).containsAll(REQUIRED_ACCEPTANCE_CRITERIA);
    }

    @Test
    void everyManifestFixtureExistsAndHasContent() throws IOException, URISyntaxException {
        FixtureManifest manifest = readManifest();

        for (Fixture fixture : manifest.fixtures()) {
            Path fixturePath = fixturePath(fixture.path());

            assertThat(Files.exists(fixturePath))
                    .as("Fixture file exists: %s", fixture.path())
                    .isTrue();
            assertThat(Files.size(fixturePath))
                    .as("Fixture file is not empty: %s", fixture.path())
                    .isGreaterThan(0L);
        }
    }

    @Test
    void csvFixtureRowCountsMatchExpectedImportOutcomes() throws IOException, URISyntaxException {
        FixtureManifest manifest = readManifest();

        for (Fixture fixture : manifest.fixtures()) {
            if (!fixture.type().endsWith("_CSV")) {
                continue;
            }

            ExpectedOutcome expected = Objects.requireNonNull(fixture.expected());
            List<String> lines = Files.readAllLines(fixturePath(fixture.path())).stream()
                    .filter(line -> !line.isBlank())
                    .toList();

            if ("FAILED".equals(expected.batchStatus())) {
                assertThat(lines)
                        .as("Failed file-level fixture has a header: %s", fixture.path())
                        .isNotEmpty();
                continue;
            }

            int expectedRows = expected.accepted()
                    + expected.duplicate()
                    + expected.rejected()
                    + expected.warning();

            assertThat(lines)
                    .as("CSV fixture has header and data rows: %s", fixture.path())
                    .hasSize(expectedRows + 1);
        }
    }

    private FixtureManifest readManifest() throws IOException {
        try (InputStream stream = classLoaderResource("fixture-manifest.json")) {
            return objectMapper.readValue(stream, FixtureManifest.class);
        }
    }

    private Path fixturePath(String relativePath) throws URISyntaxException {
        return Path.of(classLoaderResourceUrl(relativePath).toURI());
    }

    private InputStream classLoaderResource(String relativePath) {
        InputStream stream = getClass().getClassLoader()
                .getResourceAsStream(FIXTURE_ROOT + "/" + relativePath);
        assertThat(stream)
                .as("Classpath resource exists: %s", relativePath)
                .isNotNull();
        return stream;
    }

    private java.net.URL classLoaderResourceUrl(String relativePath) {
        java.net.URL url = getClass().getClassLoader()
                .getResource(FIXTURE_ROOT + "/" + relativePath);
        assertThat(url)
                .as("Classpath resource exists: %s", relativePath)
                .isNotNull();
        return url;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record FixtureManifest(
            String slice,
            List<String> requiredAcceptanceCriteria,
            List<Fixture> fixtures) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Fixture(
            String path,
            String type,
            List<String> covers,
            ExpectedOutcome expected) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ExpectedOutcome(
            int accepted,
            int duplicate,
            int rejected,
            int warning,
            String batchStatus) {
    }
}

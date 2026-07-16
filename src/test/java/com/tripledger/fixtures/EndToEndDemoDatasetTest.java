package com.tripledger.fixtures;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class EndToEndDemoDatasetTest {

    private static final String DEMO_ROOT = "fixtures/validation-release/demo";
    private static final Set<String> REQUIRED_SCENARIOS = Set.of(
            "exact_booking",
            "ota_settlement",
            "cancellation_refund",
            "ambiguous_payment",
            "duplicate_import",
            "fx_case",
            "short_settlement",
            "forbidden_adjustment",
            "restore");
    private static final Set<String> PROHIBITED_MARKERS = Set.of(
            "card_number",
            "passport",
            "medical",
            "bank_account",
            "cvv",
            "secret");

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void manifestDeclaresAllValidationDemoScenarios() throws Exception {
        JsonNode manifest = readJson("demo-manifest.json");

        assertThat(manifest.path("slice").asText()).isEqualTo("VR-026");

        Set<String> scenarioIds = scenarios(manifest).stream()
                .map(scenario -> scenario.path("id").asText())
                .collect(Collectors.toSet());

        assertThat(scenarioIds).containsExactlyInAnyOrderElementsOf(REQUIRED_SCENARIOS);
    }

    @Test
    void everyManifestSourceExistsHasContentAndMatchesExpectedRows() throws Exception {
        JsonNode manifest = readJson("demo-manifest.json");

        for (JsonNode sourceFile : manifest.path("sourceFiles")) {
            String path = sourceFile.path("path").asText();
            Path fixturePath = fixturePath(path);

            assertThat(Files.exists(fixturePath))
                    .as("Demo fixture exists: %s", path)
                    .isTrue();
            assertThat(Files.size(fixturePath))
                    .as("Demo fixture is not empty: %s", path)
                    .isGreaterThan(0L);

            int expectedRows = sourceFile.path("expectedRows").asInt();
            if (path.endsWith(".csv")) {
                assertThat(readCsv(path).rows())
                        .as("CSV row count: %s", path)
                        .hasSize(expectedRows);
            } else if (readJson(path).isArray()) {
                assertThat(readJson(path).size())
                        .as("JSON array count: %s", path)
                        .isEqualTo(expectedRows);
            } else {
                assertThat(expectedRows)
                        .as("JSON object evidence count: %s", path)
                        .isEqualTo(1);
            }
        }
    }

    @Test
    void scenarioReferencesPointToExistingEvidenceFiles() throws Exception {
        JsonNode manifest = readJson("demo-manifest.json");
        Set<String> declaredFiles = manifest.path("sourceFiles").findValues("path").stream()
                .map(JsonNode::asText)
                .collect(Collectors.toSet());

        for (JsonNode scenario : scenarios(manifest)) {
            assertThat(scenario.path("sourceFiles")).isNotEmpty();
            for (JsonNode sourceFile : scenario.path("sourceFiles")) {
                assertThat(declaredFiles)
                        .as("Scenario %s references declared file", scenario.path("id").asText())
                        .contains(sourceFile.asText());
            }
            assertThat(scenario.path("expected").isObject())
                    .as("Scenario %s declares expected evidence", scenario.path("id").asText())
                    .isTrue();
        }
    }

    @Test
    void sourceRowsCoverScenarioIdsWithoutProhibitedDataMarkers() throws Exception {
        JsonNode manifest = readJson("demo-manifest.json");
        Set<String> scenarioIds = scenarios(manifest).stream()
                .map(scenario -> scenario.path("id").asText())
                .collect(Collectors.toSet());

        Set<String> rowScenarioIds = Set.of(
                readCsv("bookings.csv"),
                readCsv("bookings_duplicate.csv"),
                readCsv("supplier_obligations.csv"),
                readCsv("financial_events.csv"))
                .stream()
                .flatMap(csv -> csv.rows().stream())
                .map(row -> row.get("scenario_id"))
                .filter(value -> value != null && !value.isBlank())
                .collect(Collectors.toSet());

        assertThat(rowScenarioIds)
                .contains("exact_booking",
                        "ota_settlement",
                        "cancellation_refund",
                        "ambiguous_payment",
                        "duplicate_import",
                        "fx_case",
                        "short_settlement");
        assertThat(scenarioIds).containsAll(rowScenarioIds);

        for (String path : sourceFilePaths(manifest)) {
            String content = Files.readString(fixturePath(path)).toLowerCase();
            for (String prohibitedMarker : PROHIBITED_MARKERS) {
                assertThat(content)
                        .as("Fixture %s must not contain prohibited marker %s", path, prohibitedMarker)
                        .doesNotContain(prohibitedMarker);
            }
        }
    }

    @Test
    void economicsExpectationsMatchDemoSourceAmounts() throws Exception {
        JsonNode manifest = readJson("demo-manifest.json");
        CsvFile bookings = readCsv("bookings.csv");
        CsvFile supplierObligations = readCsv("supplier_obligations.csv");
        CsvFile financialEvents = readCsv("financial_events.csv");

        Map<String, Map<String, String>> bookingsByReference = bookings.rows().stream()
                .collect(Collectors.toMap(row -> row.get("external_booking_id"), row -> row));

        for (JsonNode scenario : scenarios(manifest)) {
            JsonNode expected = scenario.path("expected");
            if (!expected.has("expectedCustomerReceivable")) {
                continue;
            }

            String bookingReference = scenario.path("bookingReference").asText();
            Map<String, String> booking = bookingsByReference.get(bookingReference);
            assertThat(booking)
                    .as("Booking exists for scenario %s", scenario.path("id").asText())
                    .isNotNull();

            BigDecimal contracted = money(booking.get("selling_amount"));
            BigDecimal refund = financialEvents.sum(bookingReference, "REFUND");
            BigDecimal discount = financialEvents.sum(bookingReference, "APPROVED_DISCOUNT");
            BigDecimal commission = financialEvents.sum(bookingReference, "CHANNEL_COMMISSION");
            BigDecimal paymentFee = financialEvents.sum(bookingReference, "PAYMENT_FEE");
            BigDecimal supplierCost = supplierObligations.sum(bookingReference);

            BigDecimal expectedReceivable = contracted.subtract(refund).subtract(discount);
            BigDecimal expectedDeductions = commission.add(paymentFee);
            BigDecimal expectedMargin = expectedReceivable
                    .subtract(expectedDeductions)
                    .subtract(supplierCost);

            assertThat(expected.path("expectedCustomerReceivable").asText())
                    .isEqualTo(amount(expectedReceivable));
            assertThat(expected.path("expectedDeductions").asText())
                    .isEqualTo(amount(expectedDeductions));
            assertThat(expected.path("activeSupplierCost").asText())
                    .isEqualTo(amount(supplierCost));
            assertThat(expected.path("estimatedGrossMargin").asText())
                    .isEqualTo(amount(expectedMargin));
        }
    }

    @Test
    void fxAndOperationalEvidenceMatchScenarioExpectations() throws Exception {
        JsonNode manifest = readJson("demo-manifest.json");
        JsonNode fxScenario = scenario(manifest, "fx_case");
        JsonNode fxEvidence = readJson("exchange_rate_evidence.json").get(0);

        assertThat(fxEvidence.path("bookingReference").asText())
                .isEqualTo(fxScenario.path("bookingReference").asText());
        assertThat(fxEvidence.path("targetAmount").asText())
                .isEqualTo(fxScenario.path("expected").path("exchangeRateTargetAmount").asText());
        assertThat(fxEvidence.path("targetCurrency").asText())
                .isEqualTo(fxScenario.path("expected").path("currency").asText());

        JsonNode forbidden = readJson("forbidden_financial_import.json");
        JsonNode forbiddenExpected = scenario(manifest, "forbidden_adjustment").path("expected");
        assertThat(forbidden.path("expectedError").path("code").asText())
                .isEqualTo(forbiddenExpected.path("errorCode").asText());
        assertThat(forbidden.path("expectedError").path("status").asInt())
                .isEqualTo(forbiddenExpected.path("httpStatus").asInt());

        JsonNode restore = readJson("restore_rehearsal_evidence.json");
        JsonNode restoreExpected = scenario(manifest, "restore").path("expected");
        assertThat(restore.path("expectedEvidence").path("status").asText())
                .isEqualTo(restoreExpected.path("restoreStatus").asText());
        assertThat(restore.path("expectedEvidence").path("criticalTableCountsMustMatch").size())
                .isEqualTo(5);
    }

    private JsonNode readJson(String relativePath) throws IOException {
        try (InputStream stream = classLoaderResource(relativePath)) {
            return objectMapper.readTree(stream);
        }
    }

    private List<JsonNode> scenarios(JsonNode manifest) {
        return manifest.path("scenarios").findValues("id").stream()
                .map(id -> scenario(manifest, id.asText()))
                .toList();
    }

    private JsonNode scenario(JsonNode manifest, String id) {
        for (JsonNode scenario : manifest.path("scenarios")) {
            if (scenario.path("id").asText().equals(id)) {
                return scenario;
            }
        }
        throw new IllegalArgumentException("Scenario not found: " + id);
    }

    private Set<String> sourceFilePaths(JsonNode manifest) {
        return manifest.path("sourceFiles").findValues("path").stream()
                .map(JsonNode::asText)
                .collect(Collectors.toSet());
    }

    private CsvFile readCsv(String relativePath) throws IOException {
        List<String> lines = Files.readAllLines(fixturePath(relativePath)).stream()
                .filter(line -> !line.isBlank())
                .toList();
        assertThat(lines).as("CSV has header: %s", relativePath).isNotEmpty();

        List<String> headers = splitCsvLine(lines.getFirst());
        List<Map<String, String>> rows = lines.stream()
                .skip(1)
                .map(line -> row(headers, splitCsvLine(line)))
                .toList();
        return new CsvFile(rows);
    }

    private Map<String, String> row(List<String> headers, List<String> values) {
        assertThat(values).hasSize(headers.size());
        Map<String, String> row = new HashMap<>();
        for (int index = 0; index < headers.size(); index++) {
            row.put(headers.get(index), values.get(index));
        }
        return row;
    }

    private List<String> splitCsvLine(String line) {
        return List.of(line.split(",", -1));
    }

    private Path fixturePath(String relativePath) {
        try {
            return Path.of(classLoaderResourceUrl(relativePath).toURI());
        } catch (URISyntaxException exception) {
            throw new IllegalArgumentException("Invalid fixture path: " + relativePath, exception);
        }
    }

    private InputStream classLoaderResource(String relativePath) {
        InputStream stream = getClass().getClassLoader().getResourceAsStream(DEMO_ROOT + "/" + relativePath);
        assertThat(stream)
                .as("Classpath resource exists: %s", relativePath)
                .isNotNull();
        return stream;
    }

    private java.net.URL classLoaderResourceUrl(String relativePath) {
        java.net.URL url = getClass().getClassLoader().getResource(DEMO_ROOT + "/" + relativePath);
        assertThat(url)
                .as("Classpath resource exists: %s", relativePath)
                .isNotNull();
        return url;
    }

    private BigDecimal money(String value) {
        return new BigDecimal(value).setScale(2);
    }

    private String amount(BigDecimal amount) {
        return amount.setScale(2).toPlainString();
    }

    private final class CsvFile {
        private final List<Map<String, String>> rows;

        private CsvFile(List<Map<String, String>> rows) {
            this.rows = rows;
        }

        private List<Map<String, String>> rows() {
            return rows;
        }

        private BigDecimal sum(String bookingReference, String eventType) {
            return rows.stream()
                    .filter(row -> bookingReference.equals(row.get("booking_reference")))
                    .filter(row -> eventType.equals(row.get("event_type")))
                    .map(row -> money(row.get("amount")))
                    .reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add);
        }

        private BigDecimal sum(String bookingReference) {
            return rows.stream()
                    .filter(row -> bookingReference.equals(row.get("booking_reference")))
                    .filter(row -> !"CANCELLED".equals(row.get("status")))
                    .map(row -> money(row.get("amount")))
                    .reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add);
        }
    }
}

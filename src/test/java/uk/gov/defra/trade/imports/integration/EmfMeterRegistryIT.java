package uk.gov.defra.trade.imports.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static uk.gov.defra.trade.imports.testsupport.CapturedConsole.captureSystemOut;
import static uk.gov.defra.trade.imports.testsupport.EmfDocuments.namespaceOf;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.TestPropertySource;
import uk.gov.defra.trade.imports.configuration.CacheConfig;
import uk.gov.defra.trade.imports.metrics.EmfMeterRegistry;
import uk.gov.defra.trade.imports.testsupport.CapturedConsole;

@TestPropertySource(
    properties = {
      "aws.emf.enabled=true",
      "aws.emf.environment=Local",
      "aws.emf.namespace=trade-imports-reference-data",
      "aws.emf.step=1s"
    })
class EmfMeterRegistryIT extends IntegrationBase {

  private static final ObjectMapper JSON = new ObjectMapper();

  @Autowired private MeterRegistry meterRegistry;
  @Autowired private TestRestTemplate restTemplate;
  @Autowired private CacheManager cacheManager;

  @BeforeEach
  void stubMdmAndClearItsCache() {
    stubTradeAuthToken();
    stubMdmCountriesResponse();
    cacheManager.getCache(CacheConfig.MDM_COUNTRIES_CACHE).clear();
  }

  @Test
  void theMeterRegistryInUseIsTheEmfMeterRegistry() {
    assertThat(meterRegistry).isInstanceOf(EmfMeterRegistry.class);
  }

  @Test
  void everyCustomMeterIsLetThroughTheAllowlist() {
    restTemplate.getForEntity("/countries", String.class);
    restTemplate.getForEntity("/commodities?q=d", String.class);

    for (String name :
        List.of(
            "external.call.duration",
            "external.call.failure",
            "external.dependency.available",
            "cache.hit",
            "cache.miss",
            "lookup",
            "controller.getCountries.time",
            "service.searchCommodities.time")) {
      assertThat(meterRegistry.find(name).meter()).as(name).isNotNull();
    }
  }

  @Test
  void theRegistryPublishesARecordedCallOnItsOwnSchedule() throws Exception {
    CapturedConsole console = captureSystemOut();
    try (console) {
      restTemplate.getForEntity("/countries", String.class);

      await()
          .atMost(Duration.ofSeconds(10))
          .pollInterval(Duration.ofSeconds(1))
          .until(() -> emfDocumentFor("external.call.duration", console).isPresent());
    }

    JsonNode document = emfDocumentFor("external.call.duration", console).orElseThrow();
    assertThat(namespaceOf(document)).isEqualTo("trade-imports-reference-data");
    assertThat(document.path("dependency").asText()).isNotBlank();
  }

  private static Optional<JsonNode> emfDocumentFor(String metricName, CapturedConsole console)
      throws IOException {
    for (String line : console.text().split("\n")) {
      if (line.contains("\"_aws\"") && line.contains("\"" + metricName + "\"")) {
        return Optional.of(JSON.readTree(line));
      }
    }
    return Optional.empty();
  }

  @ParameterizedTest
  @ValueSource(strings = {"cache.gets", "http.server.requests.active", "mongodb.driver.commands"})
  void aMeterOffTheAllowlistIsNeverRegistered(String name) {
    meterRegistry.counter(name).increment();

    assertThat(meterRegistry.find(name).meter()).isNull();
  }
}

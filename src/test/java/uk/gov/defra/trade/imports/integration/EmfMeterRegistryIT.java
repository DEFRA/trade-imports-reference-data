package uk.gov.defra.trade.imports.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static uk.gov.defra.trade.imports.testsupport.CapturedConsole.captureSystemOut;
import static uk.gov.defra.trade.imports.testsupport.EmfDocuments.documentsFor;
import static uk.gov.defra.trade.imports.testsupport.EmfDocuments.namespaceOf;
import static uk.gov.defra.trade.imports.testsupport.EmfDocuments.unitOf;

import com.fasterxml.jackson.databind.JsonNode;
import io.micrometer.core.instrument.MeterRegistry;
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
import uk.gov.defra.trade.imports.testsupport.EmfDocuments;

@TestPropertySource(
    properties = {
      "aws.emf.enabled=true",
      "aws.emf.environment=Local",
      "aws.emf.namespace=trade-imports-reference-data",
      "aws.emf.step=1s"
    })
class EmfMeterRegistryIT extends IntegrationBase {

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
  void meterRegistry_shouldBeTheEmfMeterRegistry_whenEmfIsEnabled() {
    assertThat(meterRegistry).isInstanceOf(EmfMeterRegistry.class);
  }

  @Test
  void allowlist_shouldLetThroughAMeterUnderEachCustomEntry() {
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
  void emfMeterRegistry_shouldPublishTheMdmCall_whenItsStepElapses() throws Exception {
    CapturedConsole console = captureSystemOut();
    try (console) {
      restTemplate.getForEntity("/countries", String.class);

      await()
          .atMost(Duration.ofSeconds(10))
          .pollInterval(Duration.ofSeconds(1))
          .until(() -> mdmCallDocument(console).isPresent());
    }

    JsonNode document = mdmCallDocument(console).orElseThrow();
    assertThat(namespaceOf(document)).isEqualTo("trade-imports-reference-data");
    assertThat(document.path("operation").asText()).isEqualTo("get-countries");
    assertThat(unitOf(document)).isEqualTo("Milliseconds");
  }

  private static Optional<JsonNode> mdmCallDocument(CapturedConsole console) {
    List<JsonNode> emfDocuments =
        console
            .text()
            .lines()
            .filter(line -> line.startsWith("{"))
            .map(EmfDocuments::parse)
            .filter(EmfDocuments::isEmfDocument)
            .toList();
    return documentsFor(emfDocuments, "external.call.duration").stream()
        .filter(document -> "mdm".equals(document.path("dependency").asText()))
        .findFirst();
  }

  @ParameterizedTest
  @ValueSource(strings = {"cache.gets", "http.server.requests.active", "mongodb.driver.commands"})
  void allowlist_shouldNotRegisterAMeter_whenItsNameIsNotListed(String name) {
    meterRegistry.counter(name).increment();

    assertThat(meterRegistry.find(name).meter()).isNull();
  }
}

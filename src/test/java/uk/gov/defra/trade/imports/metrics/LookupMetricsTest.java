package uk.gov.defra.trade.imports.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

class LookupMetricsTest {

  private final CapturingEmfEnvironment environment = new CapturingEmfEnvironment();
  private final LookupMetrics metrics = new LookupMetrics(environment.writer());

  @Test
  void recordLookup_shouldCountOneLookupOfTheGivenType() {
    metrics.recordLookup(LookupMetrics.PORTS_OF_ENTRY);

    assertThat(environment.documents()).hasSize(1);
    JsonNode document = environment.documents().getFirst();
    assertThat(document.get("LookupType").asText()).isEqualTo("ports-of-entry");
    assertThat(document.get("Lookup").asInt()).isEqualTo(1);
  }
}

package uk.gov.defra.trade.imports.metrics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.mongodb.MongoTimeoutException;
import com.mongodb.ReadPreference;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;
import java.time.Duration;
import java.util.Map;
import java.util.stream.Collectors;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.cloudwatchlogs.emf.logger.MetricsLogger;

@ExtendWith(MockitoExtension.class)
class DependencyAvailabilityMetricsTest {

  private static final String NAMESPACE = "trade-imports-reference-data";

  private final CapturingEmfEnvironment environment = new CapturingEmfEnvironment();
  private final EmfMetricWriter writer =
      new EmfMetricWriter(true, NAMESPACE, () -> new MetricsLogger(environment));
  private final ExternalCallMetrics externalCallMetrics =
      new ExternalCallMetrics(false, NAMESPACE, () -> new MetricsLogger(environment));

  @Mock
  private MongoClient mongoClient;

  @Mock
  private MongoDatabase adminDatabase;

  private DependencyAvailabilityMetrics metrics;

  @BeforeEach
  void aMongoThatAnswersPing() {
    lenient().when(mongoClient.getDatabase("admin")).thenReturn(adminDatabase);
    lenient()
        .when(adminDatabase.runCommand(any(Bson.class), any(ReadPreference.class)))
        .thenReturn(new Document("ok", 1));
    metrics = new DependencyAvailabilityMetrics(writer, externalCallMetrics, mongoClient);
  }

  @Test
  void publish_shouldReportMongoAvailable_whenItAnswersPing() {
    metrics.publish();

    assertThat(availabilityByDependency()).containsExactly(Map.entry("mongodb", 1));
  }

  @Test
  void publish_shouldReportMongoUnavailable_whenPingFails() {
    when(adminDatabase.runCommand(any(Bson.class), any(ReadPreference.class)))
        .thenThrow(new MongoTimeoutException("no server"));

    metrics.publish();

    assertThat(availabilityByDependency()).containsExactly(Map.entry("mongodb", 0));
  }

  @Test
  void publish_shouldReportEachCalledDependency_byItsMostRecentCall() {
    externalCallMetrics.recordCall(ExternalCall.TRADE_TOKEN, Duration.ofMillis(5), false);
    externalCallMetrics.recordCall(ExternalCall.MDM_GET_COUNTRIES, Duration.ofMillis(5), false);
    externalCallMetrics.recordCall(
        ExternalCall.MDM_GET_PORTS_OF_ENTRY, Duration.ofMillis(5), true);

    metrics.publish();

    assertThat(availabilityByDependency())
        .containsOnly(
            Map.entry("mongodb", 1), Map.entry("trade-token", 1), Map.entry("mdm", 0));
  }

  @Test
  void publish_shouldEmitOnlyTheDependencyDimension() {
    metrics.publish();

    JsonNode directive =
        environment.documents().getFirst().get("_aws").get("CloudWatchMetrics").get(0);
    assertThat(directive.get("Dimensions")).hasSize(1);
    assertThat(directive.get("Dimensions").get(0)).hasSize(1);
    assertThat(directive.get("Dimensions").get(0).get(0).asText()).isEqualTo("Dependency");
  }

  @Test
  void publish_shouldNotPingMongo_whenEmfIsDisabled() {
    var disabledWriter =
        new EmfMetricWriter(false, NAMESPACE, () -> new MetricsLogger(environment));
    var disabled =
        new DependencyAvailabilityMetrics(disabledWriter, externalCallMetrics, mongoClient);

    disabled.publish();

    assertThat(environment.documents()).isEmpty();
    verifyNoInteractions(mongoClient);
  }

  private Map<String, Integer> availabilityByDependency() {
    return environment.documents().stream()
        .collect(
            Collectors.toMap(
                document -> document.get("Dependency").asText(),
                document -> document.get("DependencyAvailable").asInt()));
  }
}

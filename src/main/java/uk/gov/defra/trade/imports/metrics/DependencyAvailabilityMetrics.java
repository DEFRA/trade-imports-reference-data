package uk.gov.defra.trade.imports.metrics;

import com.mongodb.MongoException;
import com.mongodb.ReadPreference;
import com.mongodb.client.MongoClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import software.amazon.cloudwatchlogs.emf.model.Unit;

/**
 * Publishes {@code DependencyAvailable} once a minute for each dependency: 1 when it is available
 * and 0 when it is not, with the single dimension {@code Dependency}.
 *
 * <p>This is deliberately not a Spring {@code HealthIndicator}. CDP uses {@code /health} to decide
 * whether a task is alive, so a dependency check there would restart every task whenever a
 * dependency went down.
 *
 * <p>MongoDB is pinged. MDM and the Trade token endpoint are not probed, because the MDM caches
 * exist to limit how often we call MDM: their value is the outcome of the most recent real call,
 * and nothing is published for them until there has been one.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DependencyAvailabilityMetrics {

  static final String AVAILABLE_METRIC = "DependencyAvailable";
  static final String DEPENDENCY_DIMENSION = "Dependency";
  static final String MONGODB = "mongodb";
  private static final long PUBLISH_INTERVAL_MILLISECONDS = 60_000;

  private final EmfMetricWriter emfMetricWriter;
  private final ExternalCallMetrics externalCallMetrics;
  private final MongoClient mongoClient;

  @Scheduled(fixedRate = PUBLISH_INTERVAL_MILLISECONDS)
  public void publish() {
    if (!emfMetricWriter.isEnabled()) {
      return;
    }
    write(MONGODB, mongoAnswersPing());
    externalCallMetrics.lastCallSucceededByDependency().forEach(this::write);
  }

  private void write(String dependency, boolean available) {
    emfMetricWriter.write(
        AVAILABLE_METRIC, available ? 1 : 0, Unit.COUNT, DEPENDENCY_DIMENSION, dependency);
  }

  private boolean mongoAnswersPing() {
    try {
      mongoClient
          .getDatabase("admin")
          .runCommand(new Document("ping", 1), ReadPreference.nearest());
      return true;
    } catch (MongoException e) {
      log.warn("MongoDB did not answer a ping: {}", e.getMessage());
      return false;
    }
  }
}

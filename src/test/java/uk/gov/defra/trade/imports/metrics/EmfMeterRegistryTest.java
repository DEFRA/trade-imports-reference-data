package uk.gov.defra.trade.imports.metrics;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.defra.trade.imports.testsupport.CapturedLogs.loggedBy;
import static uk.gov.defra.trade.imports.testsupport.EmfDocuments.dimensionSetsOf;
import static uk.gov.defra.trade.imports.testsupport.EmfDocuments.namespaceOf;
import static uk.gov.defra.trade.imports.testsupport.EmfDocuments.unitOf;
import static uk.gov.defra.trade.imports.testsupport.EmfDocuments.valuesOf;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import com.fasterxml.jackson.databind.JsonNode;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.FunctionCounter;
import io.micrometer.core.instrument.FunctionTimer;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.LongTaskTimer;
import io.micrometer.core.instrument.MockClock;
import io.micrometer.core.instrument.TimeGauge;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.binder.BaseUnits;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class EmfMeterRegistryTest {

  private static final String NAMESPACE = "trade-imports-reference-data";
  private static final Duration STEP = Duration.ofMinutes(1);

  private final MockClock clock = new MockClock();
  private final CapturingEmfEnvironment emf = new CapturingEmfEnvironment();
  private final EmfMeterRegistry registry =
      new EmfMeterRegistry(NAMESPACE, STEP, clock, emf::metricsLogger);

  @Test
  void publish_shouldWriteEveryTimerValueInMillisecondsWithItsTagsAsTheOnlyDimensions() {
    Timer timer =
        Timer.builder("external.call.duration")
            .tags("dependency", "mongodb", "operation", "find")
            .register(registry);
    timer.record(Duration.ofMillis(2));
    timer.record(Duration.ofMillis(5));

    registry.publish();

    JsonNode document = onlyDocumentFor("external.call.duration");
    assertThat(namespaceOf(document)).isEqualTo(NAMESPACE);
    assertThat(unitOf(document)).isEqualTo("Milliseconds");
    assertThat(dimensionSetsOf(document)).hasSize(1);
    assertThat(dimensionSetsOf(document).getFirst())
        .containsExactlyInAnyOrder("dependency", "operation");
    assertThat(document.path("dependency").asText()).isEqualTo("mongodb");
    assertThat(document.path("operation").asText()).isEqualTo("find");
    assertThat(valuesOf(document, "external.call.duration")).containsExactly(2.0, 5.0);
  }

  @Test
  void publish_shouldSkipATimer_whenNoValueWasRecordedSinceTheLastPublish() {
    Timer.builder("service.get.time").register(registry).record(Duration.ofMillis(1));
    registry.publish();

    registry.publish();

    assertThat(emf.documentsFor("service.get.time")).hasSize(1);
  }

  @Test
  void publish_shouldWriteEveryDistributionSummaryValueAsACount_whenItHasNoBaseUnit() {
    DistributionSummary summary =
        DistributionSummary.builder("external.call.failure")
            .tags("dependency", "mongodb", "operation", "insert")
            .register(registry);
    summary.record(0);
    summary.record(1);

    registry.publish();

    JsonNode document = onlyDocumentFor("external.call.failure");
    assertThat(unitOf(document)).isEqualTo("Count");
    assertThat(valuesOf(document, "external.call.failure")).containsExactly(0.0, 1.0);
  }

  @Test
  void publish_shouldWriteACountersCountForTheLastStep() {
    Counter counter = Counter.builder("address.change").tag("change", "create").register(registry);
    counter.increment();
    counter.increment();
    clock.add(STEP);

    registry.publish();

    JsonNode document = onlyDocumentFor("address.change");
    assertThat(unitOf(document)).isEqualTo("Count");
    assertThat(valuesOf(document, "address.change")).containsExactly(2.0);
  }

  @Test
  void publish_shouldWriteAFiniteGaugeAndSkipANaNOne() {
    AtomicInteger available = new AtomicInteger(1);
    Gauge.builder("external.dependency.available", available, AtomicInteger::get)
        .tag("dependency", "mongodb")
        .register(registry);
    Gauge.builder("unknown.gauge", () -> Double.NaN).register(registry);

    registry.publish();

    assertThat(valuesOf(onlyDocumentFor("external.dependency.available"), "external.dependency.available"))
        .containsExactly(1.0);
    assertThat(emf.documentsFor("unknown.gauge")).isEmpty();
  }

  @Test
  void publish_shouldWriteAGaugeInBytes_whenItsBaseUnitIsBytes() {
    Gauge.builder("jvm.memory.used", () -> 2048)
        .baseUnit(BaseUnits.BYTES)
        .tag("area", "heap")
        .register(registry);

    registry.publish();

    JsonNode document = onlyDocumentFor("jvm.memory.used");
    assertThat(unitOf(document)).isEqualTo("Bytes");
    assertThat(valuesOf(document, "jvm.memory.used")).containsExactly(2048.0);
  }

  @Test
  void publish_shouldWriteAGaugeWithNoUnit_whenItHasNoBaseUnit() {
    Gauge.builder("external.dependency.available", () -> 1).register(registry);

    registry.publish();

    assertThat(unitOf(onlyDocumentFor("external.dependency.available"))).isEqualTo("None");
  }

  @Test
  void publish_shouldWriteAMeterUnderAnEmptyDimensionSet_whenItHasNoTags() {
    Timer.builder("service.list.time").register(registry).record(Duration.ofMillis(3));

    registry.publish();

    assertThat(dimensionSetsOf(onlyDocumentFor("service.list.time"))).containsExactly(List.of());
  }

  @Test
  void publish_shouldWriteADistributionSummaryWithNoUnit_whenItsBaseUnitIsNotBytes() {
    DistributionSummary.builder("pool.connections")
        .baseUnit("connections")
        .register(registry)
        .record(3);

    registry.publish();

    assertThat(unitOf(onlyDocumentFor("pool.connections"))).isEqualTo("None");
  }

  @Test
  void publish_shouldWriteEveryRecordedValue_whenMoreThan100AreRecorded() {
    Timer timer = Timer.builder("external.call.duration").register(registry);
    IntStream.rangeClosed(1, 150).forEach(each -> timer.record(Duration.ofMillis(each)));

    registry.publish();

    List<Double> published = new ArrayList<>();
    emf.documentsFor("external.call.duration")
        .forEach(document -> published.addAll(valuesOf(document, "external.call.duration")));
    assertThat(published)
        .containsExactlyElementsOf(
            IntStream.rangeClosed(1, 150).mapToObj(each -> (double) each).toList());
  }

  @Test
  void publish_shouldStillWriteTheOtherMetersAndLogAWarning_whenOneCannotBePublished() {
    Counter.builder("bad.dimension").tag("action", " ").register(registry).increment();
    Timer.builder("service.create.time").register(registry).record(Duration.ofMillis(4));
    clock.add(STEP);

    List<ILoggingEvent> lines = loggedBy(EmfMeterRegistry.class, registry::publish);

    assertThat(emf.documentsFor("bad.dimension")).isEmpty();
    assertThat(emf.documentsFor("service.create.time")).hasSize(1);
    assertThat(lines).hasSize(1);
    assertThat(lines.getFirst().getLevel()).isEqualTo(Level.WARN);
    assertThat(lines.getFirst().getFormattedMessage())
        .isEqualTo("Could not publish meter bad.dimension");
    assertThat(lines.getFirst().getThrowableProxy()).isNotNull();
  }

  @Test
  void publish_shouldSkipLongTaskTimersFunctionCountersFunctionTimersAndTimeGauges() {
    LongTaskTimer.builder("long.task.timer").register(registry).start();
    FunctionCounter.builder("function.counter", new AtomicInteger(5), AtomicInteger::get)
        .register(registry);
    FunctionTimer.builder(
            "function.timer",
            new AtomicInteger(5),
            AtomicInteger::get,
            AtomicInteger::get,
            TimeUnit.MILLISECONDS)
        .register(registry);
    TimeGauge.builder("process.uptime", () -> 5, TimeUnit.SECONDS).register(registry);
    clock.add(STEP);

    registry.publish();

    assertThat(emf.documents()).isEmpty();
  }

  private JsonNode onlyDocumentFor(String metricName) {
    List<JsonNode> documents = emf.documentsFor(metricName);
    assertThat(documents).hasSize(1);
    return documents.getFirst();
  }
}

package uk.gov.defra.trade.imports.logging;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import co.elastic.logging.logback.EcsEncoder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.function.Consumer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import uk.gov.defra.trade.imports.metrics.CapturingLogAppender;

class EcsEventEncoderTest {

  private static final Logger LOG = LoggerFactory.getLogger(EcsEventEncoderTest.class);
  private static final ObjectMapper JSON = new ObjectMapper();

  private final EcsEventEncoder encoder = new EcsEventEncoder();
  private final EcsEncoder parentEncoder = new EcsEncoder();

  @BeforeEach
  void startEncoders() {
    var context = (LoggerContext) LoggerFactory.getILoggerFactory();
    for (EcsEncoder each : new EcsEncoder[] {encoder, parentEncoder}) {
      each.setContext(context);
      each.setServiceName("trade-imports-reference-data");
      each.start();
    }
  }

  @AfterEach
  void clearMdc() {
    MDC.clear();
  }

  @Test
  void encode_shouldNestEventKeyValuePairs_withANumericDuration() throws IOException {
    ILoggingEvent event =
        logged(
            log ->
                log.atInfo()
                    .addKeyValue("event.category", "external-call")
                    .addKeyValue("event.action", "mdm/get-countries")
                    .addKeyValue("event.outcome", "success")
                    .addKeyValue("event.duration", 203_000_000L)
                    .log("External call mdm/get-countries success in 203 ms"));

    JsonNode line = encode(event);

    JsonNode eventObject = line.path("event");
    assertThat(eventObject.isObject()).as("a nested event object").isTrue();
    assertThat(eventObject.get("category").asText()).isEqualTo("external-call");
    assertThat(eventObject.get("action").asText()).isEqualTo("mdm/get-countries");
    assertThat(eventObject.get("outcome").asText()).isEqualTo("success");
    assertThat(eventObject.get("duration").isIntegralNumber()).isTrue();
    assertThat(eventObject.get("duration").asLong()).isEqualTo(203_000_000L);
  }

  @Test
  void encode_shouldWriteNoFlatEventKeys() throws IOException {
    ILoggingEvent event =
        logged(
            log ->
                log.atInfo()
                    .addKeyValue("event.category", "external-call")
                    .addKeyValue("event.duration", 5L)
                    .log("External call"));

    JsonNode line = encode(event);

    assertThat(line.has("event.category")).isFalse();
    assertThat(line.has("event.duration")).isFalse();
  }

  @Test
  void encode_shouldKeepTheParentsFields_includingTraceIdAndTheException() throws IOException {
    MDC.put("trace.id", "trace-from-the-caller");
    ILoggingEvent event =
        logged(
            log ->
                log.atWarn()
                    .addKeyValue("event.outcome", "failure")
                    .setCause(new IllegalStateException("mdm unavailable"))
                    .log("External call failed"));

    JsonNode line = encode(event);

    assertThat(line.get("message").asText()).isEqualTo("External call failed");
    assertThat(line.get("log.level").asText()).isEqualTo("WARN");
    assertThat(line.get("service.name").asText()).isEqualTo("trade-imports-reference-data");
    assertThat(line.get("trace.id").asText()).isEqualTo("trace-from-the-caller");
    assertThat(line.get("error.type").asText()).isEqualTo("java.lang.IllegalStateException");
    assertThat(line.get("error.message").asText()).isEqualTo("mdm unavailable");
    assertThat(line.has("error.stack_trace")).isTrue();
    assertThat(line.get("event").get("outcome").asText()).isEqualTo("failure");
  }

  @Test
  void encode_shouldWriteALineWithNoEventKeyValuePairs_exactlyAsTheEcsEncoderDoes() {
    MDC.put("trace.id", "trace-from-the-caller");
    ILoggingEvent event = logged(log -> log.info("Started"));

    assertThat(new String(encoder.encode(event)))
        .isEqualTo(new String(parentEncoder.encode(event)));
  }

  @Test
  void encode_shouldIgnoreKeyValuePairsOutsideEvent() throws IOException {
    ILoggingEvent event =
        logged(log -> log.atInfo().addKeyValue("other.key", "value").log("Started"));

    JsonNode line = encode(event);

    assertThat(line.has("event")).isFalse();
    assertThat(line.has("other.key")).isFalse();
  }

  @Test
  void encode_shouldEscapeStringValues() throws IOException {
    ILoggingEvent event =
        logged(
            log -> log.atInfo().addKeyValue("event.action", "say \"hi\" \\ there").log("Escaped"));

    JsonNode line = encode(event);

    assertThat(line.get("event").get("action").asText()).isEqualTo("say \"hi\" \\ there");
  }

  private static ILoggingEvent logged(Consumer<Logger> logging) {
    try (var logs = CapturingLogAppender.attachedTo(EcsEventEncoderTest.class)) {
      logging.accept(LOG);
      return logs.events().getFirst();
    }
  }

  private JsonNode encode(ILoggingEvent event) throws IOException {
    return JSON.readTree(encoder.encode(event));
  }
}

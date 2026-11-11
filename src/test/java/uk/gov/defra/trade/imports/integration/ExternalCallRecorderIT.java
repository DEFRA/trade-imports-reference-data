package uk.gov.defra.trade.imports.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static uk.gov.defra.trade.imports.metrics.CapturingLogAppender.keyValues;
import static uk.gov.defra.trade.imports.testsupport.ExternalCallMeters.durationCount;
import static uk.gov.defra.trade.imports.testsupport.ExternalCallMeters.failureTotal;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.OutputStreamAppender;
import ch.qos.logback.core.encoder.Encoder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.matchers.TimeToLive;
import org.mockserver.matchers.Times;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.defra.trade.imports.configuration.CacheConfig;
import uk.gov.defra.trade.imports.metrics.CapturingLogAppender;
import uk.gov.defra.trade.imports.metrics.ExternalCallRecorder;
import uk.gov.defra.trade.imports.metrics.LastCallOutcomes;

class ExternalCallRecorderIT extends IntegrationBase {

  private static final int EXPIRED_TOKEN_PRIORITY = 10;
  private static final int FAILING_STUB_PRIORITY = 20;
  private static final String TOKEN = "trade-token";
  private static final String TOKEN_OPERATION = "client-credentials-token";

  @Autowired
  private TestRestTemplate restTemplate;

  @Autowired
  private CacheManager cacheManager;

  @Autowired
  private LastCallOutcomes lastCallOutcomes;

  @Autowired
  private MeterRegistry meterRegistry;

  @BeforeEach
  void stubAnAlwaysExpiredTokenAndClearCaches() {
    cacheManager.getCache(CacheConfig.MDM_COUNTRIES_CACHE).clear();
    cacheManager.getCache(CacheConfig.MDM_POE_CACHE).clear();
    stubMdmCountriesResponse();
    stubMdmPortsResponse();
    usingStub().when(
        request().withMethod("POST").withPath("/trade-auth/token"),
        Times.unlimited(),
        TimeToLive.unlimited(),
        EXPIRED_TOKEN_PRIORITY
    ).respond(
        response()
            .withStatusCode(200)
            .withContentType(org.mockserver.model.MediaType.APPLICATION_JSON)
            .withBody("{\"access_token\":\"test-token\",\"expires_on\":0}")
    );
  }

  @Test
  void getCountries_shouldRecordTheTradeTokenAndMdmCallsAsSuccessful() {
    CallTally token = talliedCalls(TOKEN, TOKEN_OPERATION);
    CallTally mdm = talliedCalls("mdm", "get-countries");

    ResponseEntity<String> response = restTemplate.getForEntity("/countries", String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(talliedCallsSince(token, TOKEN, TOKEN_OPERATION)).isEqualTo(new CallTally(1, 0));
    assertThat(talliedCallsSince(mdm, "mdm", "get-countries")).isEqualTo(new CallTally(1, 0));
  }

  @Test
  void getCountries_shouldRecordTheMdmCallAsFailed_whenMdmAnswers500() {
    failMdmCountries();
    CallTally mdm = talliedCalls("mdm", "get-countries");

    ResponseEntity<String> response = restTemplate.getForEntity("/countries", String.class);

    assertThat(response.getStatusCode()).isNotEqualTo(HttpStatus.OK);
    assertThat(talliedCallsSince(mdm, "mdm", "get-countries")).isEqualTo(new CallTally(1, 1));
  }

  @Test
  void getCountries_shouldRecordTheTokenCallAsFailed_andCallNoMdm_whenTheTokenEndpointAnswers500() {
    usingStub().when(
        request().withMethod("POST").withPath("/trade-auth/token"),
        Times.unlimited(),
        TimeToLive.unlimited(),
        FAILING_STUB_PRIORITY
    ).respond(response().withStatusCode(500));
    CallTally token = talliedCalls(TOKEN, TOKEN_OPERATION);
    CallTally mdm = talliedCalls("mdm", "get-countries");

    restTemplate.getForEntity("/countries", String.class);

    assertThat(talliedCallsSince(token, TOKEN, TOKEN_OPERATION)).isEqualTo(new CallTally(1, 1));
    assertThat(talliedCallsSince(mdm, "mdm", "get-countries")).isEqualTo(new CallTally(0, 0));
  }

  @Test
  void getCountries_shouldWriteTheMdmCallLineWithTheCallersTraceIdAndANestedEventObject()
      throws IOException {
    HttpHeaders headers = new HttpHeaders();
    headers.set("x-cdp-request-id", "trace-from-the-caller");

    try (var logs = CapturingLogAppender.attachedTo(ExternalCallRecorder.class)) {
      restTemplate.exchange(
          "/countries", HttpMethod.GET, new HttpEntity<>(headers), String.class);

      var mdmCallLines =
          logs.events().stream()
              .filter(e -> "mdm/get-countries".equals(keyValues(e).get("event.action")))
              .toList();
      assertThat(mdmCallLines).hasSize(1);
      JsonNode line =
          new ObjectMapper().readTree(consoleEncoder().encode(mdmCallLines.getFirst()));

      assertThat(line.path("trace.id").asText()).isEqualTo("trace-from-the-caller");
      assertThat(line.path("service.name").asText()).isEqualTo("trade-imports-reference-data");
      JsonNode eventObject = line.path("event");
      assertThat(eventObject.isObject()).as("a nested event object").isTrue();
      assertThat(eventObject.path("category").asText()).isEqualTo("external-call");
      assertThat(eventObject.path("action").asText()).isEqualTo("mdm/get-countries");
      assertThat(eventObject.path("outcome").asText()).isEqualTo("success");
      assertThat(eventObject.path("duration").isIntegralNumber()).isTrue();
      assertThat(line.has("event.action")).isFalse();
      assertThat(line.has("event.duration")).isFalse();
    }
  }

  @Test
  void getCountries_shouldMarkMdmUnavailableAndTheTokenEndpointAvailable_whenMdmAnswers500() {
    restTemplate.getForEntity("/countries", String.class);
    assertThat(lastCallOutcomes.lastCallSucceeded("mdm")).contains(true);
    failMdmCountries();
    cacheManager.getCache(CacheConfig.MDM_COUNTRIES_CACHE).clear();

    restTemplate.getForEntity("/countries", String.class);

    assertThat(lastCallOutcomes.lastCallSucceeded("mdm")).contains(false);
    assertThat(lastCallOutcomes.lastCallSucceeded(TOKEN)).contains(true);
  }

  @Test
  void getPortsOfEntry_shouldRecordTheMdmCall() {
    CallTally mdm = talliedCalls("mdm", "get-ports-of-entry");

    ResponseEntity<String> response = restTemplate.getForEntity("/ports-of-entry", String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(talliedCallsSince(mdm, "mdm", "get-ports-of-entry"))
        .isEqualTo(new CallTally(1, 0));
  }

  private void failMdmCountries() {
    usingStub().when(
        request().withMethod("GET").withPath("/mdm-service/mdm/geo/countries"),
        Times.unlimited(),
        TimeToLive.unlimited(),
        FAILING_STUB_PRIORITY
    ).respond(response().withStatusCode(500));
  }

  private record CallTally(long calls, double failures) {}

  private CallTally talliedCalls(String dependency, String operation) {
    return new CallTally(
        durationCount(meterRegistry, dependency, operation),
        failureTotal(meterRegistry, dependency, operation));
  }

  private CallTally talliedCallsSince(CallTally before, String dependency, String operation) {
    CallTally now = talliedCalls(dependency, operation);
    return new CallTally(now.calls() - before.calls(), now.failures() - before.failures());
  }

  @SuppressWarnings("unchecked")
  private static Encoder<ILoggingEvent> consoleEncoder() {
    var loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
    var console =
        (OutputStreamAppender<ILoggingEvent>)
            loggerContext.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME).getAppender("CONSOLE");
    return console.getEncoder();
  }
}

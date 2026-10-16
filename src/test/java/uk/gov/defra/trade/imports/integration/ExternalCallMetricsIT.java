package uk.gov.defra.trade.imports.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockserver.matchers.TimeToLive;
import org.mockserver.matchers.Times;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import uk.gov.defra.trade.imports.configuration.CacheConfig;
import uk.gov.defra.trade.imports.metrics.CapturingLogAppender;
import uk.gov.defra.trade.imports.metrics.ExternalCall;
import uk.gov.defra.trade.imports.metrics.ExternalCallMetrics;

class ExternalCallMetricsIT extends IntegrationBase {

  private static final int EXPIRED_TOKEN_PRIORITY = 10;
  private static final int FAILING_STUB_PRIORITY = 20;

  @Autowired
  private TestRestTemplate restTemplate;

  @Autowired
  private CacheManager cacheManager;

  @MockitoSpyBean
  private ExternalCallMetrics externalCallMetrics;

  @BeforeEach
  void stubAnAlwaysExpiredTokenAndClearCaches() {
    cacheManager.getCache("MDM_COUNTRIES_CACHE").clear();
    cacheManager.getCache("MDM_POE_CACHE").clear();
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
    Mockito.reset(externalCallMetrics);
  }

  @AfterEach
  void resetSpy() {
    Mockito.reset(externalCallMetrics);
  }

  @Test
  void getCountries_shouldRecordTheTradeTokenAndMdmCallsAsSuccessful() {
    ResponseEntity<String> response = restTemplate.getForEntity("/countries", String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    verify(externalCallMetrics)
        .recordCall(eq(ExternalCall.TRADE_TOKEN), any(Duration.class), eq(false));
    verify(externalCallMetrics)
        .recordCall(eq(ExternalCall.MDM_GET_COUNTRIES), any(Duration.class), eq(false));
  }

  @Test
  void getCountries_shouldRecordTheMdmCallAsFailed_whenMdmAnswers500() {
    usingStub().when(
        request().withMethod("GET").withPath("/mdm-service/mdm/geo/countries"),
        Times.unlimited(),
        TimeToLive.unlimited(),
        FAILING_STUB_PRIORITY
    ).respond(response().withStatusCode(500));

    ResponseEntity<String> response = restTemplate.getForEntity("/countries", String.class);

    assertThat(response.getStatusCode()).isNotEqualTo(HttpStatus.OK);
    verify(externalCallMetrics)
        .recordCall(eq(ExternalCall.MDM_GET_COUNTRIES), any(Duration.class), eq(true));
  }

  @Test
  void getCountries_shouldRecordTheTokenCallAsFailed_andCallNoMdm_whenTheTokenEndpointAnswers500() {
    usingStub().when(
        request().withMethod("POST").withPath("/trade-auth/token"),
        Times.unlimited(),
        TimeToLive.unlimited(),
        FAILING_STUB_PRIORITY
    ).respond(response().withStatusCode(500));

    restTemplate.getForEntity("/countries", String.class);

    verify(externalCallMetrics)
        .recordCall(eq(ExternalCall.TRADE_TOKEN), any(Duration.class), eq(true));
    verify(externalCallMetrics, never())
        .recordCall(eq(ExternalCall.MDM_GET_COUNTRIES), any(), anyBoolean());
  }

  @Test
  void getCountries_shouldLogTheMdmCallWithTheCallersTraceId() {
    HttpHeaders headers = new HttpHeaders();
    headers.set("x-cdp-request-id", "trace-from-the-caller");

    try (var logs = CapturingLogAppender.attachedTo(ExternalCallMetrics.class)) {
      restTemplate.exchange(
          "/countries", HttpMethod.GET, new HttpEntity<>(headers), String.class);

      assertThat(logs.events())
          .filteredOn(
              event -> "mdm/get-countries".equals(event.getMDCPropertyMap().get("event.action")))
          .singleElement()
          .satisfies(
              event ->
                  assertThat(event.getMDCPropertyMap())
                      .containsEntry("trace.id", "trace-from-the-caller")
                      .containsEntry("event.outcome", "success")
                      .containsKey("event.duration"));
    }
  }

  @Test
  void getCountries_shouldMarkMdmUnavailableAndTheTokenEndpointAvailable_whenMdmAnswers500() {
    // Given: a healthy call first, so the failure below is seen to change the reading
    restTemplate.getForEntity("/countries", String.class);
    assertThat(externalCallMetrics.lastCallSucceededByDependency()).containsEntry("mdm", true);
    usingStub().when(
        request().withMethod("GET").withPath("/mdm-service/mdm/geo/countries"),
        Times.unlimited(),
        TimeToLive.unlimited(),
        FAILING_STUB_PRIORITY
    ).respond(response().withStatusCode(500));
    cacheManager.getCache(CacheConfig.MDM_COUNTRIES_CACHE).clear();

    // When
    restTemplate.getForEntity("/countries", String.class);

    // Then
    assertThat(externalCallMetrics.lastCallSucceededByDependency())
        .containsEntry("mdm", false)
        .containsEntry("trade-token", true);
  }

  @Test
  void getPortsOfEntry_shouldRecordTheMdmCall() {
    ResponseEntity<String> response = restTemplate.getForEntity("/ports-of-entry", String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    verify(externalCallMetrics)
        .recordCall(eq(ExternalCall.MDM_GET_PORTS_OF_ENTRY), any(Duration.class), eq(false));
  }
}

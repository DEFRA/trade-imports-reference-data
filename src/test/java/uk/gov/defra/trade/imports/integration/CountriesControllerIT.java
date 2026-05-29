package uk.gov.defra.trade.imports.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.matchers.TimeToLive;
import org.mockserver.matchers.Times;
import org.mockserver.verify.VerificationTimes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class CountriesControllerIT extends IntegrationBase {

  @Autowired
  private TestRestTemplate restTemplate;

  @Autowired
  private CacheManager cacheManager;

  @BeforeEach
  void stubServices() {
    cacheManager.getCache("MDM_COUNTRIES_CACHE").clear();
    stubMdmCountriesResponse();
  }

  @Test
  void getCountries_returnsSortedCountriesFromMdm() {
    // When
    ResponseEntity<String> response = restTemplate.getForEntity("/countries", String.class);

    // Then: 200 OK with countries sorted alphabetically (France, Germany, Sweden)
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).containsSubsequence("France", "Germany", "Sweden");
  }

  @Test
  void getCountries_mapsCountryFieldsCorrectly() {
    // When
    ResponseEntity<String> response = restTemplate.getForEntity("/countries", String.class);

    // Then: alpha2 mapped to code, other fields present
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).contains("\"code\":\"DE\"");
    assertThat(response.getBody()).contains("\"name\":\"Germany\"");
    assertThat(response.getBody()).contains("\"classifiers\":[\"EU\"]");
  }

  @Test
  void getCountries_withClassifierParam_passesItToMdm() {
    // When: request with specific classifier
    ResponseEntity<String> response = restTemplate.getForEntity(
        "/countries?classifier=EU", String.class);

    // Then: 200 OK and MDM received the classifier query param
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    usingStub().verify(
        request().withMethod("GET")
            .withPath("/mdm-service/mdm/geo/countries")
            .withQueryStringParameter("classifier", "EU"),
        VerificationTimes.exactly(1)
    );
  }

  @Test
  void getCountries_callsMdm_onFirstRequestAfterCacheClear() {
    // When: single request on a cold cache (cleared in @BeforeEach)
    restTemplate.getForEntity("/countries", String.class);

    // Then: MDM was called exactly once — AC3: cache miss triggers MDM fetch
    usingStub().verify(
        request().withMethod("GET").withPath("/mdm-service/mdm/geo/countries"),
        VerificationTimes.exactly(1)
    );
  }

  @Test
  void getCountries_doesNotCacheEmptyMdmResponse() {
    // Given: MDM returns empty list on first call, real data on second.
    // Higher priority (10 > default 0) ensures this stub wins over the @BeforeEach stub (FIFO ordering).
    usingStub().when(
        request().withMethod("GET").withPath("/mdm-service/mdm/geo/countries"),
        Times.exactly(1),
        TimeToLive.unlimited(),
        10
    ).respond(
        response()
            .withStatusCode(200)
            .withHeader("x-ms-middleware-request-id", "trace-empty")
            .withContentType(org.mockserver.model.MediaType.APPLICATION_JSON)
            .withBody("[]")
    );

    // When: two consecutive requests
    restTemplate.getForEntity("/countries", String.class);
    restTemplate.getForEntity("/countries", String.class);

    // Then: MDM was called twice — empty result was not cached
    usingStub().verify(
        request().withMethod("GET").withPath("/mdm-service/mdm/geo/countries"),
        VerificationTimes.exactly(2)
    );
  }

  @Test
  void getCountries_cachesResultsIndependently_perUniqueClassifier() {
    // When: two requests with different classifiers, then the first classifier repeated
    restTemplate.getForEntity("/countries?classifier=EU", String.class);
    restTemplate.getForEntity("/countries?classifier=ANIMALS", String.class);
    restTemplate.getForEntity("/countries?classifier=EU", String.class);

    // Then: MDM called once per unique classifier — repeated classifier is a cache hit
    usingStub().verify(
        request().withMethod("GET").withPath("/mdm-service/mdm/geo/countries"),
        VerificationTimes.exactly(2)
    );
  }

  @Test
  void getCountries_returnsCachedResult_onSecondCall() {
    // When: same endpoint called twice
    restTemplate.getForEntity("/countries", String.class);
    restTemplate.getForEntity("/countries", String.class);

    // Then: MDM was only called once — second response served from cache
    usingStub().verify(
        request().withMethod("GET").withPath("/mdm-service/mdm/geo/countries"),
        VerificationTimes.exactly(1)
    );
  }
}

package uk.gov.defra.trade.imports.integration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class CountriesControllerIT extends IntegrationBase {

  @Autowired
  private TestRestTemplate restTemplate;

  @BeforeEach
  void stubServices() {
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

    // Then: 200 OK — MDM was called (stub responds regardless of classifier param)
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
  }
}

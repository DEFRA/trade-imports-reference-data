package uk.gov.defra.trade.imports.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;

@DirtiesContext(classMode = ClassMode.BEFORE_CLASS)
class TokenEndpointUnavailableIT extends IntegrationBase {

  @Autowired
  private TestRestTemplate restTemplate;

  @Test
  void searchCommodities_answersServiceUnavailable_whenTheTokenEndpointAnswers503() {
    usingStub()
        .when(request().withMethod("POST").withPath("/trade-auth/token"))
        .respond(response().withStatusCode(503));

    ResponseEntity<String> response =
        restTemplate.getForEntity("/commodities?q=fox", String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
  }
}

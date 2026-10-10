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
import uk.gov.defra.trade.imports.portsofentry.PortOfEntry;
import uk.gov.defra.trade.imports.portsofentry.PortType;

class PortsOfEntryControllerIT extends IntegrationBase {

  @Autowired
  private TestRestTemplate restTemplate;

  @Autowired
  private CacheManager cacheManager;

  @BeforeEach
  void stubServices() {
    cacheManager.getCache("MDM_POE_CACHE").clear();
    stubMdmPortsResponse();
  }

  @Test
  void getPortsOfEntry_returnsAirportsThenSeaportsThenRail_eachAToZIgnoringCase() {
    ResponseEntity<PortOfEntry[]> response = restTemplate.getForEntity("/ports-of-entry", PortOfEntry[].class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).extracting(PortOfEntry::getName)
        .containsExactly("Aberdeen Airport", "East Midlands Airport", "Aberdeen Harbour", "Teestort", "TILBURY",
            "Folkestone");
    assertThat(response.getBody()).extracting(PortOfEntry::getType)
        .containsExactly(PortType.AIRPORT, PortType.AIRPORT, PortType.SEAPORT, PortType.SEAPORT, PortType.SEAPORT,
            PortType.RAIL);
  }

  @Test
  void getPortsOfEntry_writesEachPortAsCodeNameAndType() {
    ResponseEntity<String> response = restTemplate.getForEntity("/ports-of-entry", String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).contains("{\"code\":\"GB ABD\",\"name\":\"Aberdeen Harbour\",\"type\":\"seaport\"}");
    assertThat(response.getBody()).contains("{\"code\":\"GB FOL\",\"name\":\"Folkestone\",\"type\":\"rail\"}");
  }

  @Test
  void getPortsOfEntry_callsMdm_onFirstRequestAfterCacheClear() {
    restTemplate.getForEntity("/ports-of-entry", String.class);

    usingStub().verify(
        request().withMethod("GET").withPath("/mdm-service/mdm/trade/bcp/poes"),
        VerificationTimes.exactly(1)
    );
  }

  @Test
  void getPortsOfEntry_returnsCachedResult_onSecondCall() {
    restTemplate.getForEntity("/ports-of-entry", String.class);
    restTemplate.getForEntity("/ports-of-entry", String.class);

    usingStub().verify(
        request().withMethod("GET").withPath("/mdm-service/mdm/trade/bcp/poes"),
        VerificationTimes.exactly(1)
    );
  }

  @Test
  void getPortsOfEntry_doesNotCacheEmptyMdmResponse() {
    usingStub().when(
        request().withMethod("GET").withPath("/mdm-service/mdm/trade/bcp/poes"),
        Times.exactly(1),
        TimeToLive.unlimited(),
        10
    ).respond(
        response()
            .withStatusCode(200)
            .withHeader("x-ms-middleware-request-id", "trace-empty")
            .withContentType(org.mockserver.model.MediaType.APPLICATION_JSON)
            .withBody("{\"result\":[]}")
    );

    restTemplate.getForEntity("/ports-of-entry", String.class);
    restTemplate.getForEntity("/ports-of-entry", String.class);

    usingStub().verify(
        request().withMethod("GET").withPath("/mdm-service/mdm/trade/bcp/poes"),
        VerificationTimes.exactly(2)
    );
  }
}

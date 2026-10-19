package uk.gov.defra.trade.imports.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.verify.VerificationTimes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.defra.trade.imports.commodities.CommodityGroup;
import uk.gov.defra.trade.imports.commodities.CommodityLine;
import uk.gov.defra.trade.imports.commodities.CommoditySearchResponse;
import uk.gov.defra.trade.imports.commodities.CommoditySpecies;
import uk.gov.defra.trade.imports.commodities.SpeciesCheckRequest;
import uk.gov.defra.trade.imports.commodities.SpeciesCheckResponse;

/** The MDM responses are TST's own, saved on 16 Oct 2026. */
class CommoditiesControllerIT extends IntegrationBase {

  private static final String SEARCH_PATH = "/mdm-service/mdm/trade/product/search";
  private static final String SPECIES_PATH =
      "/mdm-service/mdm/trade/product/commodities/0102/species";

  @Autowired
  private TestRestTemplate restTemplate;

  @BeforeEach
  void stubServices() {
    usingStub()
        .when(request().withMethod("POST").withPath("/trade-auth/token"))
        .respond(response()
            .withStatusCode(200)
            .withContentType(org.mockserver.model.MediaType.APPLICATION_JSON)
            .withBody("{\"access_token\":\"test-token\",\"expires_on\":9999999999999}"));
    stubSearch("dog", "integration/mdm-product-search-dog-response.json");
    stubSearch("0102", "integration/mdm-product-search-0102-response.json");
    usingStub()
        .when(request().withMethod("GET").withPath(SPECIES_PATH))
        .respond(response()
            .withStatusCode(200)
            .withHeader("x-ms-middleware-request-id", "test-trace-id")
            .withContentType(org.mockserver.model.MediaType.APPLICATION_JSON)
            .withBody(getJsonFromFile("integration/mdm-product-species-0102-response.json")));
  }

  @Test
  void searchCommodities_groupsMdmsSpeciesRowsUnderTheirCodes() {
    // When
    ResponseEntity<CommoditySearchResponse> response =
        restTemplate.getForEntity("/commodities?search=dog", CommoditySearchResponse.class);

    // Then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody().getTotal()).isEqualTo(2);
    assertThat(response.getBody().getGroups()).containsExactly(CommodityGroup.builder()
        .code("01061900")
        .species(List.of(
            CommoditySpecies.builder().latinName("Nyctereutes procyonoides").commonName("Raccoon Dog")
                .build(),
            CommoditySpecies.builder().latinName("Canis familiaris").commonName("Dog").build()))
        .build());
  }

  @Test
  void searchCommodities_listsTheSpeciesOfACommodityFoundByCode() {
    // When
    ResponseEntity<CommoditySearchResponse> response =
        restTemplate.getForEntity("/commodities?search=0102", CommoditySearchResponse.class);

    // Then
    assertThat(response.getBody().getGroups()).hasSize(1);
    CommodityGroup group = response.getBody().getGroups().get(0);
    assertThat(group.getCode()).isEqualTo("0102");
    assertThat(group.getSpecies()).hasSize(15);
    assertThat(group.getSpecies().get(4))
        .isEqualTo(CommoditySpecies.builder().latinName("Bos taurus").commonName("Cattle or Cow")
            .build());
  }

  @Test
  void searchCommodities_asksMdmForOneHundredResults() {
    // When
    restTemplate.getForEntity("/commodities?search=dog", String.class);

    // Then
    usingStub().verify(
        request().withMethod("GET").withPath(SEARCH_PATH)
            .withQueryStringParameter("q", "dog")
            .withQueryStringParameter("dataset", "gbnag")
            .withQueryStringParameter("page", "0")
            .withQueryStringParameter("size", "100"),
        VerificationTimes.exactly(1));
  }

  @Test
  void searchCommodities_doesNotCallMdm_forAOneCharacterTerm() {
    // When
    ResponseEntity<String> response = restTemplate.getForEntity("/commodities?search=d", String.class);

    // Then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isEqualTo("{\"total\":0,\"groups\":[]}");
    usingStub().verify(request().withPath(SEARCH_PATH), VerificationTimes.never());
  }

  @Test
  void checkSpecies_returnsTheLinesMdmNoLongerListsUnderTheirCode() {
    // Given: MDM lists "Bos sp." under 0102, not "Bos spp."
    SpeciesCheckRequest request = SpeciesCheckRequest.builder().lines(List.of(
        CommodityLine.builder().code("0102").latinName("Bos taurus").build(),
        CommodityLine.builder().code("0102").latinName("Bos spp.").build())).build();

    // When
    ResponseEntity<SpeciesCheckResponse> response = restTemplate.postForEntity(
        "/commodities/species-check", request, SpeciesCheckResponse.class);

    // Then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody().getMissing())
        .containsExactly(CommodityLine.builder().code("0102").latinName("Bos spp.").build());
    usingStub().verify(
        request().withMethod("GET").withPath(SPECIES_PATH)
            .withQueryStringParameter("dataset", "gbnag")
            .withQueryStringParameter("page", "0")
            .withQueryStringParameter("size", "100"),
        VerificationTimes.exactly(1));
  }

  private void stubSearch(String term, String fixture) {
    usingStub()
        .when(request().withMethod("GET").withPath(SEARCH_PATH).withQueryStringParameter("q", term))
        .respond(response()
            .withStatusCode(200)
            .withHeader("x-ms-middleware-request-id", "test-trace-id")
            .withContentType(org.mockserver.model.MediaType.APPLICATION_JSON)
            .withBody(getJsonFromFile(fixture)));
  }
}

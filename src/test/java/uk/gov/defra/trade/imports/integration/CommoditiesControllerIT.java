package uk.gov.defra.trade.imports.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockserver.verify.VerificationTimes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import uk.gov.defra.trade.imports.commodities.CommodityGroup;
import uk.gov.defra.trade.imports.commodities.CommodityLine;
import uk.gov.defra.trade.imports.commodities.CommoditySearchResponse;
import uk.gov.defra.trade.imports.commodities.CommoditySpecies;
import uk.gov.defra.trade.imports.commodities.SpeciesCheckRequest;
import uk.gov.defra.trade.imports.commodities.SpeciesCheckResponse;

class CommoditiesControllerIT extends IntegrationBase {

  private static final String SEARCH_PATH = "/mdm-service/mdm/trade/product/search";
  private static final String SPECIES_PATH =
      "/mdm-service/mdm/trade/product/commodities/0102/species";
  private static final String COMMODITIES_PATH = "/mdm-service/mdm/trade/product/commodities";

  @Autowired
  private TestRestTemplate restTemplate;

  @BeforeEach
  void stubServices() {
    stubTradeAuthToken();
    stubSearch("dog", "integration/mdm-product-search-dog-response.json");
    stubSearch("0102", "integration/mdm-product-search-0102-response.json");
    stubSearch("cattle", "integration/mdm-product-search-cattle-response.json");
    stubMdmGet(SPECIES_PATH, "integration/mdm-product-species-0102-response.json");
    stubMdmGet(COMMODITIES_PATH, "integration/mdm-commodities-response.json");
  }

  @Test
  void searchCommodities_leavesOutTheCodesMdmFlagsAsGerminal() {
    ResponseEntity<CommoditySearchResponse> response =
        restTemplate.getForEntity("/commodities?q=cattle", CommoditySearchResponse.class);

    assertThat(response.getBody().groups()).containsExactly(CommodityGroup.builder()
        .code("0102")
        .species(List.of(
            CommoditySpecies.builder().latinName("Bos taurus").commonName("Cattle or Cow").build()))
        .build());
    usingStub().verify(
        request().withMethod("GET").withPath(COMMODITIES_PATH)
            .withQueryStringParameter("codes", "0102,05111000")
            .withQueryStringParameter("dataset", "gbnag"),
        VerificationTimes.exactly(1));
  }

  @Test
  void searchCommodities_groupsMdmsSpeciesRowsUnderTheirCodes() {
    ResponseEntity<CommoditySearchResponse> response =
        restTemplate.getForEntity("/commodities?q=dog", CommoditySearchResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody().total()).isEqualTo(2);
    assertThat(response.getBody().groups()).containsExactly(CommodityGroup.builder()
        .code("01061900")
        .species(List.of(
            CommoditySpecies.builder().latinName("Nyctereutes procyonoides").commonName("Raccoon Dog")
                .build(),
            CommoditySpecies.builder().latinName("Canis familiaris").commonName("Dog").build()))
        .build());
  }

  @Test
  void searchCommodities_listsTheSpeciesOfACommodityFoundByCode() {
    ResponseEntity<CommoditySearchResponse> response =
        restTemplate.getForEntity("/commodities?q=0102", CommoditySearchResponse.class);

    assertThat(response.getBody().groups()).containsExactly(CommodityGroup.builder()
        .code("0102")
        .species(List.of(
            species("Bos", "Cattle"),
            species("Bubalus", "water buffalos"),
            species("Poephagus spp.", "Animals"),
            species("Bison bonasus", "European Bison"),
            species("Bos taurus", "Cattle or Cow"),
            species("Bibos spp.", "Cattle"),
            species("Novibos spp.", "Cattle"),
            species("Bos sp.", "Cattle"),
            species("Bubalus bubalis", "Water Buffalo"),
            species("Elephas maximus", "Asian Elephant"),
            species("Loxodonta africana", "African elephant"),
            species("Bison", "Bison"),
            species("Bison bison", "Bisons Spp"),
            species("Ovibos", "Muskoxen"),
            species("Syncerus", "African Buffaloes")))
        .build());
  }

  private static CommoditySpecies species(String latinName, String commonName) {
    return CommoditySpecies.builder().latinName(latinName).commonName(commonName).build();
  }

  @Test
  void searchCommodities_asksMdmForThePageAndSizeGiven() {
    ResponseEntity<CommoditySearchResponse> response = restTemplate.getForEntity(
        "/commodities?q=dog&page=1&size=10", CommoditySearchResponse.class);

    assertThat(response.getBody().page()).isEqualTo(1);
    assertThat(response.getBody().size()).isEqualTo(10);
    usingStub().verify(
        request().withMethod("GET").withPath(SEARCH_PATH)
            .withQueryStringParameter("q", "dog")
            .withQueryStringParameter("page", "1")
            .withQueryStringParameter("size", "10"),
        VerificationTimes.exactly(1));
  }

  @Test
  void searchCommodities_asksMdmForOneHundredResults_whenNoSizeIsGiven() {
    restTemplate.getForEntity("/commodities?q=dog", String.class);

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
    ResponseEntity<String> response = restTemplate.getForEntity("/commodities?q=d", String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isEqualTo("{\"total\":0,\"page\":0,\"size\":100,\"groups\":[]}");
    usingStub().verify(request().withPath(SEARCH_PATH), VerificationTimes.never());
  }

  @Test
  void checkSpecies_returnsTheLinesMdmDoesNotListUnderTheirCode() {
    SpeciesCheckRequest request = SpeciesCheckRequest.builder().lines(List.of(
        CommodityLine.builder().code("0102").latinName("Bos taurus").build(),
        CommodityLine.builder().code("0102").latinName("Bos spp.").build())).build();

    ResponseEntity<SpeciesCheckResponse> response = restTemplate.postForEntity(
        "/commodities/species-check", request, SpeciesCheckResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody().missing())
        .containsExactly(CommodityLine.builder().code("0102").latinName("Bos spp.").build());
    usingStub().verify(
        request().withMethod("GET").withPath(SPECIES_PATH)
            .withQueryStringParameter("dataset", "gbnag")
            .withQueryStringParameter("page", "0")
            .withQueryStringParameter("size", "100"),
        VerificationTimes.exactly(1));
  }

  @Test
  void checkSpecies_answersServiceUnavailable_whenMdmSendsNoBody() {
    usingStub()
        .when(request().withMethod("GET")
            .withPath("/mdm-service/mdm/trade/product/commodities/0103/species"))
        .respond(response().withStatusCode(200));
    SpeciesCheckRequest request = SpeciesCheckRequest.builder().lines(List.of(
        CommodityLine.builder().code("0103").latinName("Sus scrofa").build())).build();

    ResponseEntity<String> response = restTemplate.postForEntity(
        "/commodities/species-check", request, String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
  }

  @ParameterizedTest
  @ValueSource(strings = {"page=-1", "size=0", "size=101", "page=first", "size=many"})
  void searchCommodities_answersBadRequestWithoutCallingMdm_forPagingOutsideItsLimits(
      String paging) {
    ResponseEntity<String> response =
        restTemplate.getForEntity("/commodities?q=dog&" + paging, String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    usingStub().verify(request().withPath(SEARCH_PATH), VerificationTimes.never());
  }

  @ParameterizedTest
  @ValueSource(strings = {
      "",
      "{",
      "{}",
      "{\"lines\":null}",
      "{\"lines\":[null]}",
      "{\"lines\":[{\"latinName\":\"Bos taurus\"}]}",
      "{\"lines\":[{\"code\":\"0102\",\"latinName\":\" \"}]}"})
  void checkSpecies_answersBadRequestWithoutCallingMdm_forABodyThatIsNotAListOfLines(
      String body) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    ResponseEntity<String> response = restTemplate.postForEntity(
        "/commodities/species-check", new HttpEntity<>(body, headers), String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    usingStub().verify(request().withPath(SPECIES_PATH), VerificationTimes.never());
  }

  private void stubSearch(String term, String fixture) {
    stubMdmGet(request().withPath(SEARCH_PATH).withQueryStringParameter("q", term), fixture);
  }
}

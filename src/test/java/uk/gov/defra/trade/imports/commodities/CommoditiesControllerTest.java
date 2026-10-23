package uk.gov.defra.trade.imports.commodities;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.defra.trade.imports.client.MdmClient;
import uk.gov.defra.trade.imports.client.MdmProductPage;
import uk.gov.defra.trade.imports.client.MdmService;
import uk.gov.defra.trade.imports.configuration.MdmConfiguration;
import uk.gov.defra.trade.imports.metrics.CapturingEmfEnvironment;
import uk.gov.defra.trade.imports.metrics.LookupMetrics;

@ExtendWith(MockitoExtension.class)
class CommoditiesControllerTest {

  @Mock
  private MdmClient mdmClient;

  private final CapturingEmfEnvironment metricsEnvironment = new CapturingEmfEnvironment();

  private CommoditiesController controller;

  @BeforeEach
  void setUp() {
    MdmService mdmService = new MdmService(mdmClient, new MdmConfiguration());
    LookupMetrics lookupMetrics = new LookupMetrics(metricsEnvironment.writer());
    controller = new CommoditiesController(
        new CommoditySearchService(mdmService), new SpeciesCheckService(mdmService), lookupMetrics);
  }

  @Test
  void searchCommodities_shouldCountOneCommoditySearchLookup() {
    when(mdmClient.searchProducts(any(), any(), any(), anyInt(), anyInt()))
        .thenReturn(ResponseEntity.ok(new MdmProductPage<>(List.of(), 0L)));

    ResponseEntity<CommoditySearchResponse> response = controller.searchCommodities("dog", 0, 10);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(lookupTypesCounted()).containsExactly("commodity-search");
  }

  @Test
  void checkSpecies_shouldCountOneSpeciesCheckLookup() {
    SpeciesCheckRequest request = SpeciesCheckRequest.builder().lines(List.of()).build();

    ResponseEntity<SpeciesCheckResponse> response = controller.checkSpecies(request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(lookupTypesCounted()).containsExactly("species-check");
  }

  private List<String> lookupTypesCounted() {
    return metricsEnvironment.documents().stream()
        .filter(document -> document.get("Lookup").asInt() == 1)
        .map(document -> document.get("LookupType"))
        .map(JsonNode::asText)
        .toList();
  }
}

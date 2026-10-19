package uk.gov.defra.trade.imports.commodities;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.defra.trade.imports.metrics.LookupMetrics;

@ExtendWith(MockitoExtension.class)
class CommoditiesControllerTest {

  @Mock
  private CommoditySearchService commoditySearchService;

  @Mock
  private SpeciesCheckService speciesCheckService;

  @Mock
  private LookupMetrics lookupMetrics;

  private CommoditiesController controller;

  @BeforeEach
  void setUp() {
    controller =
        new CommoditiesController(commoditySearchService, speciesCheckService, lookupMetrics);
  }

  @Test
  void searchCommodities_shouldCountOneCommoditySearchLookup() {
    // Given
    when(commoditySearchService.search("dog"))
        .thenReturn(CommoditySearchResponse.builder().total(0).groups(List.of()).build());

    // When
    ResponseEntity<CommoditySearchResponse> response = controller.searchCommodities("dog");

    // Then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    verify(lookupMetrics).recordLookup("commodity-search");
  }

  @Test
  void checkSpecies_shouldCountOneSpeciesCheckLookup() {
    // Given
    SpeciesCheckRequest request = SpeciesCheckRequest.builder().lines(List.of()).build();
    when(speciesCheckService.check(request))
        .thenReturn(SpeciesCheckResponse.builder().missing(List.of()).build());

    // When
    ResponseEntity<SpeciesCheckResponse> response = controller.checkSpecies(request);

    // Then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    verify(lookupMetrics).recordLookup("species-check");
  }
}

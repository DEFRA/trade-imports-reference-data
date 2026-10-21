package uk.gov.defra.trade.imports.commodities;

import io.micrometer.core.annotation.Timed;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.defra.trade.imports.metrics.LookupMetrics;

@RestController
@AllArgsConstructor
@RequestMapping("/commodities")
public class CommoditiesController {

  private final CommoditySearchService commoditySearchService;
  private final SpeciesCheckService speciesCheckService;
  private final LookupMetrics lookupMetrics;

  @GetMapping()
  @Timed("controller.searchCommodities.time")
  public ResponseEntity<CommoditySearchResponse> searchCommodities(
      @RequestParam(required = false, value = "search") String searchTerm,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "100") int size) {
    lookupMetrics.recordLookup(LookupMetrics.COMMODITY_SEARCH);
    return ResponseEntity.ok(commoditySearchService.search(searchTerm, page, size));
  }

  @PostMapping("/species-check")
  @Timed("controller.checkSpecies.time")
  public ResponseEntity<SpeciesCheckResponse> checkSpecies(
      @RequestBody(required = false) SpeciesCheckRequest request) {
    lookupMetrics.recordLookup(LookupMetrics.SPECIES_CHECK);
    return ResponseEntity.ok(speciesCheckService.check(request));
  }
}

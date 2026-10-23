package uk.gov.defra.trade.imports.commodities;

import io.micrometer.core.annotation.Timed;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.defra.trade.imports.metrics.LookupMetrics;

@RestController
@Validated
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
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "100") @Min(1) @Max(CommoditySearchService.MAXIMUM_PAGE_SIZE)
      int size) {
    lookupMetrics.recordLookup(LookupMetrics.COMMODITY_SEARCH);
    return ResponseEntity.ok(commoditySearchService.search(searchTerm, page, size));
  }

  @PostMapping("/species-check")
  @Timed("controller.checkSpecies.time")
  public ResponseEntity<SpeciesCheckResponse> checkSpecies(
      @Valid @RequestBody SpeciesCheckRequest request) {
    lookupMetrics.recordLookup(LookupMetrics.SPECIES_CHECK);
    return ResponseEntity.ok(speciesCheckService.check(request));
  }
}

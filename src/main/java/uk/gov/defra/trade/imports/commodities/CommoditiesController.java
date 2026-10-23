package uk.gov.defra.trade.imports.commodities;

import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "Commodities", description = "Live-animal commodity codes and species from MDM")
@RestController
@Validated
@AllArgsConstructor
@RequestMapping("/commodities")
public class CommoditiesController {

  private final CommoditySearchService commoditySearchService;
  private final SpeciesCheckService speciesCheckService;
  private final LookupMetrics lookupMetrics;

  @Operation(summary = "Search species by name or commodity code",
      description = "Lists the non-germinal species MDM matches to the term, grouped by code. "
          + "A term shorter than two characters matches nothing.")
  @ApiResponse(responseCode = "200", description = "The matching species")
  @ApiResponse(responseCode = "400", description = "The page or size is not valid")
  @ApiResponse(responseCode = "503", description = "MDM sent no usable answer")
  @GetMapping()
  @Timed("controller.searchCommodities.time")
  public ResponseEntity<CommoditySearchResponse> searchCommodities(
      @Parameter(description = "Search term: a species name or a commodity code", example = "dog")
      @RequestParam(value = "q", required = false) String q,
      @Parameter(description = "Zero-based page number")
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @Parameter(description = "Most species to list, from 1 to 100")
      @RequestParam(defaultValue = "100") @Min(1) @Max(CommoditySearchService.MAXIMUM_PAGE_SIZE)
      int size) {
    lookupMetrics.recordLookup(LookupMetrics.COMMODITY_SEARCH);
    return ResponseEntity.ok(commoditySearchService.search(q, page, size));
  }

  @Operation(summary = "Check chosen species are still listed under their codes",
      description = "Returns the lines whose Latin name MDM does not list under their code.")
  @ApiResponse(responseCode = "200", description = "The lines MDM does not list")
  @ApiResponse(responseCode = "400", description = "The body is not a list of complete lines")
  @ApiResponse(responseCode = "503", description = "MDM sent no usable answer")
  @PostMapping("/species-check")
  @Timed("controller.checkSpecies.time")
  public ResponseEntity<SpeciesCheckResponse> checkSpecies(
      @Valid @RequestBody SpeciesCheckRequest request) {
    lookupMetrics.recordLookup(LookupMetrics.SPECIES_CHECK);
    return ResponseEntity.ok(speciesCheckService.check(request));
  }
}

package uk.gov.defra.trade.imports.commodities;

import io.micrometer.core.annotation.Timed;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@AllArgsConstructor
@RequestMapping("/commodities")
public class CommoditiesController {

  private final CommoditySearchService commoditySearchService;

  @GetMapping()
  @Timed("controller.searchCommodities.time")
  public ResponseEntity<CommoditySearchResponse> searchCommodities(
      @RequestParam(required = false, value = "search") String search) {
    return ResponseEntity.ok(commoditySearchService.search(search));
  }
}

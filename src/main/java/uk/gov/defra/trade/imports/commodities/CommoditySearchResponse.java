package uk.gov.defra.trade.imports.commodities;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;

@Schema(description = "One page of the species matching a search term, grouped by commodity code")
@Builder
public record CommoditySearchResponse(
    @Schema(description = "Number of species matching the search term", requiredMode = REQUIRED,
        example = "15")
    long total,
    @Schema(description = "Zero-based page number", requiredMode = REQUIRED, example = "0")
    int page,
    @Schema(description = "Most species the page lists", requiredMode = REQUIRED, example = "100")
    int size,
    @Schema(description = "Species on this page, grouped by commodity code in MDM's order",
        requiredMode = REQUIRED)
    List<CommodityGroup> groups) {

  public CommoditySearchResponse {
    groups = List.copyOf(groups);
  }
}

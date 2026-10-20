package uk.gov.defra.trade.imports.commodities;

import java.util.List;
import lombok.Builder;

@Builder
public record CommoditySearchResponse(long total, int page, int size, List<CommodityGroup> groups) {

  public CommoditySearchResponse {
    groups = List.copyOf(groups);
  }
}

package uk.gov.defra.trade.imports.commodities;

import java.util.List;
import lombok.Builder;

@Builder
public record SpeciesCheckResponse(List<CommodityLine> missing) {

  public SpeciesCheckResponse {
    missing = List.copyOf(missing);
  }
}

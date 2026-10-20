package uk.gov.defra.trade.imports.commodities;

import java.util.List;
import lombok.Builder;

@Builder
public record SpeciesCheckRequest(List<CommodityLine> lines) {

  public SpeciesCheckRequest {
    lines = lines == null ? null : List.copyOf(lines);
  }
}

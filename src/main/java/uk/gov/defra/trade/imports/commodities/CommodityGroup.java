package uk.gov.defra.trade.imports.commodities;

import java.util.List;
import java.util.Objects;
import lombok.Builder;

@Builder
public record CommodityGroup(String code, List<CommoditySpecies> species) {

  public CommodityGroup {
    Objects.requireNonNull(code, "code");
    species = List.copyOf(species);
  }
}

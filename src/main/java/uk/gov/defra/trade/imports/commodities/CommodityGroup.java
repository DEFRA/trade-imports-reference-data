package uk.gov.defra.trade.imports.commodities;

import java.util.List;
import lombok.Builder;

@Builder
public record CommodityGroup(String code, List<CommoditySpecies> species) {
}

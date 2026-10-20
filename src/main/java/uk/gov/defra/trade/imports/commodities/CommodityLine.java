package uk.gov.defra.trade.imports.commodities;

import lombok.Builder;

@Builder
public record CommodityLine(String code, String latinName) {
}

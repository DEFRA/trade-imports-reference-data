package uk.gov.defra.trade.imports.commodities;

import lombok.Builder;

@Builder
public record CommoditySpecies(String latinName, String commonName) {
}

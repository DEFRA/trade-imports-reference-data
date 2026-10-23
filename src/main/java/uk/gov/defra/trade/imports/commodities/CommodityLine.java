package uk.gov.defra.trade.imports.commodities;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record CommodityLine(@NotBlank String code, @NotBlank String latinName) {
}

package uk.gov.defra.trade.imports.commodities;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Objects;
import lombok.Builder;

@Schema(description = "The matching species listed under one commodity code")
@Builder
public record CommodityGroup(
    @Schema(description = "Commodity (CN) code", requiredMode = REQUIRED, example = "0102")
    String code,
    @Schema(description = "Matching species listed under the code", requiredMode = REQUIRED)
    List<CommoditySpecies> species) {

  public CommodityGroup {
    Objects.requireNonNull(code, "code");
    species = List.copyOf(species);
  }
}

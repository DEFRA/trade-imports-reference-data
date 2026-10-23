package uk.gov.defra.trade.imports.commodities;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.NOT_REQUIRED;
import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Schema(description = "A species MDM lists under a commodity code")
@Builder
public record CommoditySpecies(
    @Schema(description = "Latin (scientific) name", requiredMode = REQUIRED,
        example = "Bos taurus")
    String latinName,
    @Schema(description = "Common name, when MDM holds one", requiredMode = NOT_REQUIRED,
        types = {"string", "null"}, example = "Cattle or Cow")
    String commonName) {
}

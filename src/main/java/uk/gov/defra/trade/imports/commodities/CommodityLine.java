package uk.gov.defra.trade.imports.commodities;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Schema(description = "A species chosen under a commodity code")
@Builder
public record CommodityLine(
    @Schema(description = "Commodity (CN) code", requiredMode = REQUIRED, example = "0102")
    @NotBlank String code,
    @Schema(description = "Latin (scientific) name of the species", requiredMode = REQUIRED,
        example = "Bos taurus")
    @NotBlank String latinName) {
}

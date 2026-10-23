package uk.gov.defra.trade.imports.commodities;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;

@Schema(description = "The outcome of checking commodity lines against MDM")
@Builder
public record SpeciesCheckResponse(
    @Schema(description = "Lines whose species MDM does not list under their code",
        requiredMode = REQUIRED)
    List<CommodityLine> missing) {

  public SpeciesCheckResponse {
    missing = List.copyOf(missing);
  }
}

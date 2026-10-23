package uk.gov.defra.trade.imports.commodities;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Objects;
import lombok.Builder;

@Schema(description = "Commodity lines whose species to check against MDM")
@Builder
public record SpeciesCheckRequest(
    @Schema(description = "Commodity lines whose species to check against MDM",
        requiredMode = REQUIRED)
    @NotNull @Valid List<CommodityLine> lines) {

  public SpeciesCheckRequest {
    lines = List.copyOf(Objects.requireNonNull(lines, "lines"));
  }
}

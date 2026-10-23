package uk.gov.defra.trade.imports.commodities;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Objects;
import lombok.Builder;

@Builder
public record SpeciesCheckRequest(@NotNull @Valid List<CommodityLine> lines) {

  public SpeciesCheckRequest {
    lines = List.copyOf(Objects.requireNonNull(lines, "lines"));
  }
}

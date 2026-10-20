package uk.gov.defra.trade.imports.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Builder;

@Builder
public record MdmCommodity(
    String code,
    String description,
    Boolean isGerminal,
    Boolean isSelectable,
    Integer speciesCount,
    @JsonProperty("scientificNames") List<MdmSpecies> species) {
}

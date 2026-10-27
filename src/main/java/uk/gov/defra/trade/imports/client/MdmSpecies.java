package uk.gov.defra.trade.imports.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder
public record MdmSpecies(@JsonProperty("speciesName") String latinName, String commonName) {
}

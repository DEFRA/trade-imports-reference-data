package uk.gov.defra.trade.imports.client;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Builder;

@Builder
public record MdmProductSearchResult(
    @JsonFormat(with = JsonFormat.Feature.READ_UNKNOWN_ENUM_VALUES_AS_NULL) ResultType resultType,
    @JsonProperty("speciesName") String latinName,
    String commonName,
    String commodityCnCode,
    String code,
    @JsonProperty("scientificNames") List<MdmSpecies> species) {

  public enum ResultType {
    @JsonProperty("species")
    SPECIES,
    @JsonProperty("commodity")
    COMMODITY
  }
}

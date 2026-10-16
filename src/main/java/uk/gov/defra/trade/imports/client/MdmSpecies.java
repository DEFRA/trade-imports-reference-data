package uk.gov.defra.trade.imports.client;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MdmSpecies {

  private String speciesName;
  private String commonName;
  private String commodityCnCode;
}

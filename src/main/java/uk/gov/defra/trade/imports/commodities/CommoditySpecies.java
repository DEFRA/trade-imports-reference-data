package uk.gov.defra.trade.imports.commodities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommoditySpecies {

  private String latinName;
  private String commonName;
}

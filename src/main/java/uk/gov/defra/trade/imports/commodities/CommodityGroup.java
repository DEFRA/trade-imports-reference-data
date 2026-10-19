package uk.gov.defra.trade.imports.commodities;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommodityGroup {

  private String code;
  private List<CommoditySpecies> species;
}

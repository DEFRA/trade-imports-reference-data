package uk.gov.defra.trade.imports.client;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** {@code scientificNames} stops at 100 even when {@code speciesCount} is higher. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MdmCommodity {

  private String code;
  private String description;
  private Boolean isGerminal;
  private Boolean isSelectable;
  private Integer speciesCount;
  private List<MdmSpecies> scientificNames;
}

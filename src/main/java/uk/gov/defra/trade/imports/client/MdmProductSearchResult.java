package uk.gov.defra.trade.imports.client;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A search row is either a species ({@code resultType} "species") or a whole commodity
 * ("commodity", e.g. for a code search), each with its own fields; the other kind's are null.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MdmProductSearchResult {

  public static final String SPECIES = "species";
  public static final String COMMODITY = "commodity";

  private String resultType;

  private String speciesName;
  private String commonName;
  private String commodityCnCode;

  private String code;
  private String description;
  private Boolean isGerminal;
  private Boolean isSelectable;
  private Integer speciesCount;
  private List<MdmSpecies> scientificNames;
}

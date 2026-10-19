package uk.gov.defra.trade.imports.commodities;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * {@code total} is MDM's count of matching rows, germinals included, not the rows returned.
 * {@code page} and {@code size} are the page asked of MDM, after limits are applied.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommoditySearchResponse {

  private long total;
  private int page;
  private int size;
  private List<CommodityGroup> groups;
}

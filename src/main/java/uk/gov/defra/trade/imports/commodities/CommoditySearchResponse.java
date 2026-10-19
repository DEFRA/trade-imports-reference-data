package uk.gov.defra.trade.imports.commodities;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** {@code total} is MDM's count of matching rows, germinals included, not the rows returned. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommoditySearchResponse {

  private long total;
  private List<CommodityGroup> groups;
}

package uk.gov.defra.trade.imports.portsofentry;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.defra.trade.imports.client.MdmPortOfEntry;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortOfEntry {

  private String code;
  private String name;

  public PortOfEntry(MdmPortOfEntry mdm) {
    this.code = mdm.getCode();
    this.name = mdm.getName();
  }
}

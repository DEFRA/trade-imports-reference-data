package uk.gov.defra.trade.imports.portsofentry;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.gov.defra.trade.imports.client.MdmPortOfEntry;

@Slf4j
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortOfEntry {

  private static final Set<String> REPORTED_UNKNOWN_TRAFFIC_BY_PORT = ConcurrentHashMap.newKeySet();

  private String code;
  private String name;
  private PortType type;

  public PortOfEntry(MdmPortOfEntry mdm) {
    this.code = mdm.getCode();
    this.name = mdm.getName();
    this.type = PortType.fromTraffic(mdm.getTraffic()).orElse(null);
    if (type == null && REPORTED_UNKNOWN_TRAFFIC_BY_PORT.add(code + "|" + mdm.getTraffic())) {
      log.warn("MDM port {} has traffic '{}', which is not a known port type; it is listed last with no type",
          code, mdm.getTraffic());
    }
  }
}

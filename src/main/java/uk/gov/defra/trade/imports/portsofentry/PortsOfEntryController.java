package uk.gov.defra.trade.imports.portsofentry;

import io.micrometer.core.annotation.Timed;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.defra.trade.imports.client.MdmService;

@Slf4j
@RestController
@AllArgsConstructor
@RequestMapping("/ports-of-entry")
public class PortsOfEntryController {

  private final MdmService mdmService;

  @GetMapping()
  @Timed("controller.getPortsOfEntry.time")
  public ResponseEntity<List<PortOfEntry>> getPortsOfEntry() {
    List<PortOfEntry> ports = mdmService.getPortsOfEntry()
        .stream()
        .map(PortOfEntry::new)
        .sorted(PortListOrder.AIRPORTS_THEN_SEAPORTS_THEN_RAIL)
        .toList();
    return ResponseEntity.ok(ports);
  }
}

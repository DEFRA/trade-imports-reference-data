package uk.gov.defra.trade.imports.portsofentry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.defra.trade.imports.client.MdmService;
import uk.gov.defra.trade.imports.metrics.LookupMetrics;

@ExtendWith(MockitoExtension.class)
class PortsOfEntryControllerTest {

  @Mock
  private MdmService mdmService;

  @Mock
  private LookupMetrics lookupMetrics;

  @InjectMocks
  private PortsOfEntryController controller;

  @Test
  void getPortsOfEntry_countsOnePortsOfEntryLookup() {
    when(mdmService.getPortsOfEntry()).thenReturn(List.of());

    ResponseEntity<List<PortOfEntry>> response = controller.getPortsOfEntry();

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    verify(lookupMetrics).recordLookup("ports-of-entry");
  }
}

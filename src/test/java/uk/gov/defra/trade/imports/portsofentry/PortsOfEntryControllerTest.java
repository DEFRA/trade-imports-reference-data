package uk.gov.defra.trade.imports.portsofentry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

  private PortsOfEntryController controller;

  @BeforeEach
  void setUp() {
    controller = new PortsOfEntryController(mdmService, lookupMetrics);
  }

  @Test
  void getPortsOfEntry_shouldCountOnePortsOfEntryLookup() {
    // Given
    when(mdmService.getPortsOfEntry()).thenReturn(List.of());

    // When
    ResponseEntity<List<PortOfEntry>> response = controller.getPortsOfEntry();

    // Then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    verify(lookupMetrics).recordLookup("ports-of-entry");
  }
}

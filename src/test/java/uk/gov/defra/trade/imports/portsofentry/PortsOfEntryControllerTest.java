package uk.gov.defra.trade.imports.portsofentry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.defra.trade.imports.client.MdmPortOfEntry;
import uk.gov.defra.trade.imports.client.MdmService;

@ExtendWith(MockitoExtension.class)
class PortsOfEntryControllerTest {

  @Mock
  private MdmService mdmService;

  @InjectMocks
  private PortsOfEntryController controller;

  @Test
  void getPortsOfEntry_listsAirportsThenSeaportsThenRailPorts() {
    // Given
    when(mdmService.getPortsOfEntry()).thenReturn(List.of(
        port("GB FOL", "Folkestone", "Rail"),
        port("GB ABD", "Aberdeen Harbour", "Port"),
        port("GB EDI", "Edinburgh Airport", "Airport"),
        port("GB AVO", "Avonmouth Docks", "Port"),
        port("GB DYC", "Aberdeen Airport", "Airport")));

    // When
    ResponseEntity<List<PortOfEntry>> response = controller.getPortsOfEntry();

    // Then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).extracting(PortOfEntry::getName)
        .containsExactly("Aberdeen Airport", "Edinburgh Airport", "Aberdeen Harbour", "Avonmouth Docks",
            "Folkestone");
  }

  @Test
  void getPortsOfEntry_sortsNamesWithinAGroupIgnoringLetterCase() {
    // Given
    when(mdmService.getPortsOfEntry()).thenReturn(List.of(
        port("GB TLP", "Tilbury Port", "Port"),
        port("GB TIL", "TILBURY", "Port"),
        port("GB TEE", "Teestort", "Port"),
        port("GB ABD", "Aberdeen Harbour", "Port")));

    // When
    ResponseEntity<List<PortOfEntry>> response = controller.getPortsOfEntry();

    // Then
    assertThat(response.getBody()).extracting(PortOfEntry::getName)
        .containsExactly("Aberdeen Harbour", "Teestort", "TILBURY", "Tilbury Port");
  }

  @Test
  void getPortsOfEntry_comparesDoubledSpacesAsSingleSpaces() {
    // Given
    when(mdmService.getPortsOfEntry()).thenReturn(List.of(
        port("GB TYN", "Port of Tyne", "Port"),
        port("GB DOV", "Port of Dover", "Port"),
        port("GB PSQ", "Portsmouth  Quay", "Port"),
        port("GB PSP", "Portsmouth Port", "Port")));

    // When
    ResponseEntity<List<PortOfEntry>> response = controller.getPortsOfEntry();

    // Then
    assertThat(response.getBody()).extracting(PortOfEntry::getName)
        .containsExactly("Port of Dover", "Port of Tyne", "Portsmouth Port", "Portsmouth  Quay");
  }

  @Test
  void getPortsOfEntry_comparesNonBreakingSpaceAsSingleSpace() {
    // Given
    when(mdmService.getPortsOfEntry()).thenReturn(List.of(
        port("AAA", "Port of Tyne", "Port"),
        port("ZZZ", "Port of\u00A0Dover", "Port")));

    // When
    ResponseEntity<List<PortOfEntry>> response = controller.getPortsOfEntry();

    // Then
    assertThat(response.getBody()).extracting(PortOfEntry::getName)
        .containsExactly("Port of\u00A0Dover", "Port of Tyne");
  }

  @Test
  void getPortsOfEntry_ordersPortsWithTheSameNameByCode() {
    // Given
    when(mdmService.getPortsOfEntry()).thenReturn(List.of(
        port("GBSHS", "Port of Sheerness", "Port"),
        port("GB SHS", "Port of Sheerness", "Port")));

    // When
    ResponseEntity<List<PortOfEntry>> response = controller.getPortsOfEntry();

    // Then
    assertThat(response.getBody()).extracting(PortOfEntry::getCode)
        .containsExactly("GB SHS", "GBSHS");
  }

  @Test
  void getPortsOfEntry_mapsMdmTrafficToPortType() {
    // Given
    when(mdmService.getPortsOfEntry()).thenReturn(List.of(
        port("GB AAA", "Alpha", "Airport"),
        port("GB BBB", "Beta", "PORT"),
        port("GB CCC", "Gamma", "rail")));

    // When
    ResponseEntity<List<PortOfEntry>> response = controller.getPortsOfEntry();

    // Then
    assertThat(response.getBody()).extracting(PortOfEntry::getType)
        .containsExactly(PortType.AIRPORT, PortType.SEAPORT, PortType.RAIL);
    assertThat(response.getBody()).extracting(PortOfEntry::getCode)
        .containsExactly("GB AAA", "GB BBB", "GB CCC");
    assertThat(response.getBody()).extracting(PortOfEntry::getName)
        .containsExactly("Alpha", "Beta", "Gamma");
  }

  @Test
  void getPortsOfEntry_keepsAPortOfUnknownTypeLast() {
    // Given
    when(mdmService.getPortsOfEntry()).thenReturn(List.of(
        port("GB ZZB", "Beta Quay", null),
        port("GB ZZA", "Alpha Ferry Terminal", "Ferry"),
        port("GB FOL", "Folkestone", "Rail")));

    // When
    ResponseEntity<List<PortOfEntry>> response = controller.getPortsOfEntry();

    // Then
    assertThat(response.getBody()).extracting(PortOfEntry::getName)
        .containsExactly("Folkestone", "Alpha Ferry Terminal", "Beta Quay");
    assertThat(response.getBody()).extracting(PortOfEntry::getType)
        .containsExactly(PortType.RAIL, null, null);
  }

  @Test
  void getPortsOfEntry_returnsEmptyList_whenMdmReturnsNoPorts() {
    // Given
    when(mdmService.getPortsOfEntry()).thenReturn(List.of());

    // When
    ResponseEntity<List<PortOfEntry>> response = controller.getPortsOfEntry();

    // Then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isEmpty();
  }

  private static MdmPortOfEntry port(String code, String name, String traffic) {
    return MdmPortOfEntry.builder().code(code).name(name).traffic(traffic).build();
  }
}

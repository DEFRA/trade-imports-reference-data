package uk.gov.defra.trade.imports.countries;

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
import uk.gov.defra.trade.imports.client.MdmCodedValue;
import uk.gov.defra.trade.imports.client.MdmCountry;
import uk.gov.defra.trade.imports.client.MdmService;
import uk.gov.defra.trade.imports.client.MdmSubDivision;
import uk.gov.defra.trade.imports.countries.SubDivision;

@ExtendWith(MockitoExtension.class)
class CountriesControllerTest {

  @Mock
  private MdmService mdmService;

  @InjectMocks
  private CountriesController controller;

  @Test
  void getCountries_sortsMdmResponseAlphabeticallyByName() {
    // Given: MDM returns countries in non-alphabetical order
    List<MdmCountry> unsortedCountries = List.of(
        MdmCountry.builder().effectiveAlpha2("SE").effectiveAlias("Sweden").build(),
        MdmCountry.builder().effectiveAlpha2("AT").effectiveAlias("Austria").build(),
        MdmCountry.builder().effectiveAlpha2("FR").effectiveAlias("France").build()
    );
    when(mdmService.getCountries("GBNAG_SPS_EX")).thenReturn(unsortedCountries);

    // When
    ResponseEntity<List<Country>> response = controller.getCountries("GBNAG_SPS_EX");

    // Then: countries are returned in alphabetical order by name
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).extracting(Country::getName)
        .containsExactly("Austria", "France", "Sweden");
  }

  @Test
  void getCountries_mapsMdmCountryFieldsToCountry() {
    // Given
    MdmCountry mdmCountry = MdmCountry.builder()
        .effectiveAlpha2("DE")
        .effectiveAlias("Germany")
        .build();
    when(mdmService.getCountries("GBNAG_SPS_EX")).thenReturn(List.of(mdmCountry));

    // When
    ResponseEntity<List<Country>> response = controller.getCountries("GBNAG_SPS_EX");

    // Then
    Country country = response.getBody().get(0);
    assertThat(country.getCode()).isEqualTo("DE");
    assertThat(country.getName()).isEqualTo("Germany");
    assertThat(country.getSubDivisions()).isEmpty();
  }

  @Test
  void getCountries_mapsSubDivisionsFromMdmCountry() {
    MdmCountry spain = MdmCountry.builder()
        .effectiveAlpha2("ES")
        .effectiveAlias("Spain")
        .subDivisions(List.of(
            MdmSubDivision.builder()
                .code(MdmCodedValue.builder().value("ES-CN").build())
                .name("Canary Islands")
                .build()))
        .build();
    MdmCountry austria = MdmCountry.builder()
        .effectiveAlpha2("AT")
        .effectiveAlias("Austria")
        .subDivisions(List.of())
        .build();
    when(mdmService.getCountries("GBNAG_SPS_EX")).thenReturn(List.of(spain, austria));

    ResponseEntity<List<Country>> response = controller.getCountries("GBNAG_SPS_EX");

    Country mappedSpain = response.getBody().stream()
        .filter(country -> "ES".equals(country.getCode()))
        .findFirst()
        .orElseThrow();
    assertThat(mappedSpain.getSubDivisions()).containsExactly(
        SubDivision.builder().code("ES-CN").name("Canary Islands").build());
    Country mappedAustria = response.getBody().stream()
        .filter(country -> "AT".equals(country.getCode()))
        .findFirst()
        .orElseThrow();
    assertThat(mappedAustria.getSubDivisions()).isEmpty();
  }

  @Test
  void getCountries_returnsEmptyList_whenMdmReturnsNoCountries() {
    // Given
    when(mdmService.getCountries(null)).thenReturn(List.of());

    // When
    ResponseEntity<List<Country>> response = controller.getCountries(null);

    // Then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isEmpty();
  }
}

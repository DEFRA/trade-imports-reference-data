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
import uk.gov.defra.trade.imports.client.MdmCountry;
import uk.gov.defra.trade.imports.client.MdmService;

@ExtendWith(MockitoExtension.class)
class CountriesControllerTest {

  @Mock
  private MdmService mdmService;

  @InjectMocks
  private CountriesController controller;

  @Test
  void getCountries_sortsMdmResponseAlphabeticallyByName() {
    // Given: MDM returns countries in non-alphabetical order
    List<String> classifiers = List.of("EU", "EFTA", "CTC");
    List<MdmCountry> unsortedCountries = List.of(
        MdmCountry.builder().alpha2("SE").name("Sweden").build(),
        MdmCountry.builder().alpha2("AT").name("Austria").build(),
        MdmCountry.builder().alpha2("FR").name("France").build()
    );
    when(mdmService.getCountries(classifiers)).thenReturn(unsortedCountries);

    // When
    ResponseEntity<List<Country>> response = controller.getCountries(classifiers);

    // Then: countries are returned in alphabetical order by name
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).extracting(Country::getName)
        .containsExactly("Austria", "France", "Sweden");
  }

  @Test
  void getCountries_mapsMdmCountryFieldsToCountry() {
    // Given
    List<String> classifiers = List.of("EU");
    MdmCountry mdmCountry = MdmCountry.builder()
        .alpha2("DE")
        .name("Germany")
        .classifiers(List.of("EU"))
        .internalClassifiers(List.of("INTERNAL_1"))
        .build();
    when(mdmService.getCountries(classifiers)).thenReturn(List.of(mdmCountry));

    // When
    ResponseEntity<List<Country>> response = controller.getCountries(classifiers);

    // Then
    Country country = response.getBody().get(0);
    assertThat(country.getCode()).isEqualTo("DE");
    assertThat(country.getName()).isEqualTo("Germany");
    assertThat(country.getClassifiers()).containsExactly("EU");
    assertThat(country.getInternalClassifiers()).containsExactly("INTERNAL_1");
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

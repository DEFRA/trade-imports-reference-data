package uk.gov.defra.trade.imports.countries;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import uk.gov.defra.trade.imports.client.MdmCountry;

class CountryTest {

  @Test
  void constructor_mapsAllFieldsFromMdmCountry() {
    // Given
    MdmCountry mdmCountry = MdmCountry.builder()
        .alpha2("DE")
        .name("Germany")
        .classifiers(List.of("EU", "EFTA"))
        .internalClassifiers(List.of("INTERNAL_1"))
        .build();

    // When
    Country country = new Country(mdmCountry);

    // Then
    assertThat(country.getCode()).isEqualTo("DE");
    assertThat(country.getName()).isEqualTo("Germany");
    assertThat(country.getClassifiers()).containsExactly("EU", "EFTA");
    assertThat(country.getInternalClassifiers()).containsExactly("INTERNAL_1");
  }
}

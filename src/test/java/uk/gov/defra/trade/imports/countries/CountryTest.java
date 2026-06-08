package uk.gov.defra.trade.imports.countries;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import uk.gov.defra.trade.imports.client.MdmCountry;

class CountryTest {

  @Test
  void constructor_mapsAllFieldsFromMdmCountry() {
    // Given
    MdmCountry mdmCountry = MdmCountry.builder()
        .effectiveAlpha2("DE")
        .effectiveAlias("Germany")
        .build();

    // When
    Country country = new Country(mdmCountry);

    // Then
    assertThat(country.getCode()).isEqualTo("DE");
    assertThat(country.getName()).isEqualTo("Germany");
  }
}

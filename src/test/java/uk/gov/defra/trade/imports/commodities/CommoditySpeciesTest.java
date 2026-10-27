package uk.gov.defra.trade.imports.commodities;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class CommoditySpeciesTest {

  @Test
  void rejectsANullLatinName() {
    CommoditySpecies.CommoditySpeciesBuilder species =
        CommoditySpecies.builder().commonName("Cattle");

    assertThatThrownBy(species::build)
        .isInstanceOf(NullPointerException.class).hasMessage("latinName");
  }
}

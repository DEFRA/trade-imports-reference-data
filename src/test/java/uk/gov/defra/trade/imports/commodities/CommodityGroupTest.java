package uk.gov.defra.trade.imports.commodities;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class CommodityGroupTest {

  @Test
  void holdsItsOwnCopyOfTheSpecies() {
    List<CommoditySpecies> species = new ArrayList<>();
    species.add(CommoditySpecies.builder().latinName("Bos taurus").build());

    CommodityGroup group = CommodityGroup.builder().code("0102").species(species).build();
    species.add(CommoditySpecies.builder().latinName("Bison bison").build());

    assertThat(group.species())
        .containsExactly(CommoditySpecies.builder().latinName("Bos taurus").build());
  }

  @Test
  void rejectsANullCode() {
    CommodityGroup.CommodityGroupBuilder group = CommodityGroup.builder().species(List.of());

    assertThatThrownBy(group::build).isInstanceOf(NullPointerException.class).hasMessage("code");
  }

  @Test
  void rejectsANullSpeciesList() {
    CommodityGroup.CommodityGroupBuilder group = CommodityGroup.builder().code("0102");

    assertThatThrownBy(group::build).isInstanceOf(NullPointerException.class);
  }
}

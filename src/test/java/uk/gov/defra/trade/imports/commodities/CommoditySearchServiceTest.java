package uk.gov.defra.trade.imports.commodities;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.defra.trade.imports.client.MdmProductPage;
import uk.gov.defra.trade.imports.client.MdmProductSearchResult;
import uk.gov.defra.trade.imports.client.MdmService;
import uk.gov.defra.trade.imports.client.MdmSpecies;

@ExtendWith(MockitoExtension.class)
class CommoditySearchServiceTest {

  @Mock
  private MdmService mdmService;

  @InjectMocks
  private CommoditySearchService service;

  @Test
  void search_groupsSpeciesByCodeInMdmOrder() {
    // Given: MDM's rows for "dog"
    when(mdmService.searchProducts("dog", 0, 10)).thenReturn(page(2,
        species("01061900", "Nyctereutes procyonoides", "Raccoon Dog"),
        species("01061900", "Canis familiaris", "Dog")));

    // When
    CommoditySearchResponse response = service.search("dog", 0, 10);

    // Then
    assertThat(response.getTotal()).isEqualTo(2);
    assertThat(response.getGroups()).containsExactly(CommodityGroup.builder()
        .code("01061900")
        .species(List.of(
            CommoditySpecies.builder().latinName("Nyctereutes procyonoides").commonName("Raccoon Dog")
                .build(),
            CommoditySpecies.builder().latinName("Canis familiaris").commonName("Dog").build()))
        .build());
  }

  @Test
  void search_listsACommodityRowsSpeciesOncePerCode() {
    // Given: MDM's rows for "horse", where 0101 comes back as species rows and as a commodity row
    when(mdmService.searchProducts("horse", 0, 10)).thenReturn(page(7,
        species("0101", "Equus cabalus", "Horse"),
        species("05119985", "Equus cabalus", "Horse"),
        species("05119985", "Equus caballus", "Horse"),
        species("01061900", "Equidae", "Horses"),
        species("01061900", "Equus", "Horses"),
        species("05119985", "Equidae", "Horses"),
        MdmProductSearchResult.builder()
            .resultType(MdmProductSearchResult.COMMODITY)
            .code("0101")
            .description("Live horses, asses, mules and hinnies")
            .isGerminal(false)
            .scientificNames(List.of(
                MdmSpecies.builder().speciesName("Equus cabalus*asinus").build(),
                MdmSpecies.builder().speciesName("Equus cabalus").commonName("Horse").build(),
                MdmSpecies.builder().speciesName("Equus asinus").commonName("Donkey").build()))
            .build()));

    // When
    CommoditySearchResponse response = service.search("horse", 0, 10);

    // Then
    assertThat(response.getTotal()).isEqualTo(7);
    assertThat(response.getGroups()).containsExactly(
        CommodityGroup.builder()
            .code("0101")
            .species(List.of(
                CommoditySpecies.builder().latinName("Equus cabalus").commonName("Horse").build(),
                CommoditySpecies.builder().latinName("Equus cabalus*asinus").build(),
                CommoditySpecies.builder().latinName("Equus asinus").commonName("Donkey").build()))
            .build(),
        CommodityGroup.builder()
            .code("05119985")
            .species(List.of(
                CommoditySpecies.builder().latinName("Equus cabalus").commonName("Horse").build(),
                CommoditySpecies.builder().latinName("Equus caballus").commonName("Horse").build(),
                CommoditySpecies.builder().latinName("Equidae").commonName("Horses").build()))
            .build(),
        CommodityGroup.builder()
            .code("01061900")
            .species(List.of(
                CommoditySpecies.builder().latinName("Equidae").commonName("Horses").build(),
                CommoditySpecies.builder().latinName("Equus").commonName("Horses").build()))
            .build());
  }

  @Test
  void search_listsNoMoreSpeciesThanThePageSize_whenACommodityRowHoldsMore() {
    // Given: MDM's commodity row for "0102" carries its species inside it
    when(mdmService.searchProducts("0102", 0, 2)).thenReturn(page(1,
        MdmProductSearchResult.builder()
            .resultType(MdmProductSearchResult.COMMODITY)
            .code("0102")
            .description("Live bovine animals")
            .scientificNames(List.of(
                MdmSpecies.builder().speciesName("Bos").commonName("Cattle").build(),
                MdmSpecies.builder().speciesName("Bubalus").commonName("water buffalos").build(),
                MdmSpecies.builder().speciesName("Bos taurus").commonName("Cattle or Cow").build()))
            .build(),
        species("01061900", "Bos taurus", "Cattle or Cow")));

    // When
    CommoditySearchResponse response = service.search("0102", 0, 2);

    // Then
    assertThat(response.getGroups()).containsExactly(CommodityGroup.builder()
        .code("0102")
        .species(List.of(
            CommoditySpecies.builder().latinName("Bos").commonName("Cattle").build(),
            CommoditySpecies.builder().latinName("Bubalus").commonName("water buffalos").build()))
        .build());
  }

  @Test
  void search_asksMdmForAPageWithinItsLimits() {
    // Given
    when(mdmService.searchProducts("dog", 0, 100)).thenReturn(page(0));

    // When
    CommoditySearchResponse response = service.search("dog", -1, 500);

    // Then
    assertThat(response.getPage()).isZero();
    assertThat(response.getSize()).isEqualTo(100);
  }

  @Test
  void search_trimsTheTermBeforeSearching() {
    // Given
    when(mdmService.searchProducts("dog", 0, 10)).thenReturn(page(0));

    // When
    CommoditySearchResponse response = service.search("  dog  ", 0, 10);

    // Then
    assertThat(response.getTotal()).isZero();
    assertThat(response.getGroups()).isEmpty();
  }

  @Test
  void search_returnsNothingWithoutCallingMdm_whenTheTermIsShorterThanTwoCharacters() {
    // When
    CommoditySearchResponse blank = service.search("   ", 0, 10);
    CommoditySearchResponse missing = service.search(null, 0, 10);
    CommoditySearchResponse oneCharacter = service.search(" c ", 0, 10);

    // Then
    assertThat(List.of(blank, missing, oneCharacter)).allSatisfy(response -> {
      assertThat(response.getTotal()).isZero();
      assertThat(response.getGroups()).isEmpty();
    });
    verify(mdmService, never()).searchProducts(anyString(), anyInt(), anyInt());
  }

  private static MdmProductPage<MdmProductSearchResult> page(long total,
      MdmProductSearchResult... rows) {
    return MdmProductPage.<MdmProductSearchResult>builder()
        .content(List.of(rows))
        .page(0)
        .size(100)
        .totalElements(total)
        .build();
  }

  private static MdmProductSearchResult species(String code, String latinName, String commonName) {
    return MdmProductSearchResult.builder()
        .resultType(MdmProductSearchResult.SPECIES)
        .commodityCnCode(code)
        .speciesName(latinName)
        .commonName(commonName)
        .build();
  }
}

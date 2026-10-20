package uk.gov.defra.trade.imports.commodities;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import uk.gov.defra.trade.imports.client.MdmClient;
import uk.gov.defra.trade.imports.client.MdmProductPage;
import uk.gov.defra.trade.imports.client.MdmProductSearchResult;
import uk.gov.defra.trade.imports.client.MdmProductSearchResult.ResultType;
import uk.gov.defra.trade.imports.client.MdmService;
import uk.gov.defra.trade.imports.client.MdmSpecies;
import uk.gov.defra.trade.imports.configuration.MdmConfiguration;

@ExtendWith(MockitoExtension.class)
class CommoditySearchServiceTest {

  @Mock
  private MdmClient mdmClient;

  private CommoditySearchService service;

  @BeforeEach
  void setUp() {
    service = new CommoditySearchService(new MdmService(mdmClient, new MdmConfiguration()));
  }

  @Test
  void search_groupsSpeciesByCodeInMdmOrder() {
    // Given
    givenMdmSearchAnswers("dog", 0, 10, page(2,
        species("01061900", "Nyctereutes procyonoides", "Raccoon Dog"),
        species("01061900", "Canis familiaris", "Dog")));

    // When
    CommoditySearchResponse response = service.search("dog", 0, 10);

    // Then
    assertThat(response.total()).isEqualTo(2);
    assertThat(response.groups()).containsExactly(CommodityGroup.builder()
        .code("01061900")
        .species(List.of(
            CommoditySpecies.builder().latinName("Nyctereutes procyonoides").commonName("Raccoon Dog")
                .build(),
            CommoditySpecies.builder().latinName("Canis familiaris").commonName("Dog").build()))
        .build());
  }

  @Test
  void search_listsACommodityRowsSpeciesOncePerCode() {
    // Given
    givenMdmSearchAnswers("horse", 0, 10, page(7,
        species("0101", "Equus cabalus", "Horse"),
        species("05119985", "Equus cabalus", "Horse"),
        species("05119985", "Equus caballus", "Horse"),
        species("01061900", "Equidae", "Horses"),
        species("01061900", "Equus", "Horses"),
        species("05119985", "Equidae", "Horses"),
        commodityRow("0101", "Live horses, asses, mules and hinnies",
            MdmSpecies.builder().speciesName("Equus cabalus*asinus").build(),
            MdmSpecies.builder().speciesName("Equus cabalus").commonName("Horse").build(),
            MdmSpecies.builder().speciesName("Equus asinus").commonName("Donkey").build())));

    // When
    CommoditySearchResponse response = service.search("horse", 0, 10);

    // Then
    assertThat(response.total()).isEqualTo(7);
    assertThat(response.groups()).containsExactly(
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
    // Given
    givenMdmSearchAnswers("0102", 0, 2, page(1,
        commodityRow("0102", "Live bovine animals",
            MdmSpecies.builder().speciesName("Bos").commonName("Cattle").build(),
            MdmSpecies.builder().speciesName("Bubalus").commonName("water buffalos").build(),
            MdmSpecies.builder().speciesName("Bos taurus").commonName("Cattle or Cow").build()),
        species("01061900", "Bos taurus", "Cattle or Cow")));

    // When
    CommoditySearchResponse response = service.search("0102", 0, 2);

    // Then
    assertThat(response.groups()).containsExactly(CommodityGroup.builder()
        .code("0102")
        .species(List.of(
            CommoditySpecies.builder().latinName("Bos").commonName("Cattle").build(),
            CommoditySpecies.builder().latinName("Bubalus").commonName("water buffalos").build()))
        .build());
  }

  @Test
  void search_asksMdmForAPageWithinItsLimits() {
    // Given
    givenMdmSearchAnswers("dog", 0, 100, page(0));

    // When
    CommoditySearchResponse response = service.search("dog", -1, 500);

    // Then
    assertThat(response.page()).isZero();
    assertThat(response.size()).isEqualTo(100);
  }

  @Test
  void search_trimsTheTermBeforeSearching() {
    // Given
    givenMdmSearchAnswers("dog", 0, 10, page(0));

    // When
    CommoditySearchResponse response = service.search("  dog  ", 0, 10);

    // Then
    assertThat(response.total()).isZero();
    assertThat(response.groups()).isEmpty();
  }

  @Test
  void search_returnsNothingWithoutCallingMdm_whenTheTermIsShorterThanTwoCharacters() {
    // When
    CommoditySearchResponse blank = service.search("   ", 0, 10);
    CommoditySearchResponse missing = service.search(null, 0, 10);
    CommoditySearchResponse oneCharacter = service.search(" c ", 0, 10);

    // Then
    assertThat(List.of(blank, missing, oneCharacter)).allSatisfy(response -> {
      assertThat(response.total()).isZero();
      assertThat(response.groups()).isEmpty();
    });
    verifyNoInteractions(mdmClient);
  }

  private void givenMdmSearchAnswers(String searchTerm, int page, int size,
      MdmProductPage<MdmProductSearchResult> productPage) {
    when(mdmClient.searchProducts(any(), eq(searchTerm), any(), eq(page), eq(size)))
        .thenReturn(ResponseEntity.ok(productPage));
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
        .resultType(ResultType.SPECIES)
        .commodityCnCode(code)
        .speciesName(latinName)
        .commonName(commonName)
        .build();
  }

  private static MdmProductSearchResult commodityRow(String code, String description,
      MdmSpecies... species) {
    return MdmProductSearchResult.builder()
        .resultType(ResultType.COMMODITY)
        .code(code)
        .description(description)
        .species(List.of(species))
        .build();
  }
}

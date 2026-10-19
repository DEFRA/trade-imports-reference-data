package uk.gov.defra.trade.imports.commodities;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.defra.trade.imports.client.MdmProductPage;
import uk.gov.defra.trade.imports.client.MdmService;
import uk.gov.defra.trade.imports.client.MdmSpecies;

@ExtendWith(MockitoExtension.class)
class SpeciesCheckServiceTest {

  @Mock
  private MdmService mdmService;

  @InjectMocks
  private SpeciesCheckService service;

  @Test
  void check_returnsTheLinesWhoseLatinNameMdmNoLongerListsUnderTheirCode() {
    // Given
    when(mdmService.getCommoditySpecies("0102", 0, 100))
        .thenReturn(page(2, "Bos taurus", "Bubalus bubalis"));

    // When
    SpeciesCheckResponse response = service.check(request(
        line("0102", "Bos taurus"),
        line("0102", "Bison bison"),
        line("0102", "bos taurus")));

    // Then
    assertThat(response.getMissing())
        .containsExactly(line("0102", "Bison bison"), line("0102", "bos taurus"));
  }

  @Test
  void check_asksMdmOncePerCode_andReadsEveryPage() {
    // Given: MDM lists 120 species under 01061900, over two pages
    when(mdmService.getCommoditySpecies("01061900", 0, 100))
        .thenReturn(page(120, "Acinonyx sp.", "Addax"));
    when(mdmService.getCommoditySpecies("01061900", 1, 100))
        .thenReturn(page(120, "Pseudoryx", "Pudu"));

    // When
    SpeciesCheckResponse response = service.check(request(
        line("01061900", "Addax"),
        line("01061900", "Pudu")));

    // Then
    assertThat(response.getMissing()).isEmpty();
    verify(mdmService, times(1)).getCommoditySpecies("01061900", 0, 100);
    verify(mdmService, times(1)).getCommoditySpecies("01061900", 1, 100);
  }

  @Test
  void check_returnsEveryLine_forACodeMdmHasNoSpeciesFor() {
    // Given
    when(mdmService.getCommoditySpecies("030111", 0, 100)).thenReturn(page(0));

    // When
    SpeciesCheckResponse response = service.check(request(line("030111", "Cyprinus carpio")));

    // Then
    assertThat(response.getMissing()).containsExactly(line("030111", "Cyprinus carpio"));
  }

  @Test
  void check_returnsALineWithoutCallingMdm_whenItHasNoCodeOrLatinName() {
    // When
    SpeciesCheckResponse response = service.check(request(
        line(null, "Bos taurus"),
        line("0102", null)));

    // Then
    assertThat(response.getMissing())
        .containsExactly(line(null, "Bos taurus"), line("0102", null));
    verify(mdmService, never()).getCommoditySpecies(anyString(), anyInt(), anyInt());
  }

  @Test
  void check_returnsNothing_whenThereAreNoLines() {
    // When
    SpeciesCheckResponse noBody = service.check(null);
    SpeciesCheckResponse noLines = service.check(new SpeciesCheckRequest());

    // Then
    assertThat(noBody.getMissing()).isEmpty();
    assertThat(noLines.getMissing()).isEmpty();
  }

  private static SpeciesCheckRequest request(CommodityLine... lines) {
    return SpeciesCheckRequest.builder().lines(List.of(lines)).build();
  }

  private static CommodityLine line(String code, String latinName) {
    return CommodityLine.builder().code(code).latinName(latinName).build();
  }

  private static MdmProductPage<MdmSpecies> page(long total, String... latinNames) {
    return MdmProductPage.<MdmSpecies>builder()
        .content(Arrays.stream(latinNames)
            .map(name -> MdmSpecies.builder().speciesName(name).build())
            .toList())
        .totalElements(total)
        .build();
  }
}

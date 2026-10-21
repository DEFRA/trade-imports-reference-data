package uk.gov.defra.trade.imports.commodities;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import uk.gov.defra.trade.imports.client.MdmClient;
import uk.gov.defra.trade.imports.client.MdmProductPage;
import uk.gov.defra.trade.imports.client.MdmService;
import uk.gov.defra.trade.imports.client.MdmSpecies;
import uk.gov.defra.trade.imports.configuration.MdmConfiguration;

@ExtendWith(MockitoExtension.class)
class SpeciesCheckServiceTest {

  private static final long TWO_PAGES_OF_SPECIES = 120;

  @Mock
  private MdmClient mdmClient;

  private SpeciesCheckService service;

  @BeforeEach
  void setUp() {
    service = new SpeciesCheckService(new MdmService(mdmClient, new MdmConfiguration()));
  }

  @Test
  void check_returnsTheLinesWhoseLatinNameIsNotListedUnderTheirCode_matchingCaseExactly() {
    givenMdmSpeciesPage("0102", 0, page(2, "Bos taurus", "Bubalus bubalis"));

    SpeciesCheckResponse response = service.check(request(
        line("0102", "Bos taurus"),
        line("0102", "Bison bison"),
        line("0102", "bos taurus")));

    assertThat(response.missing())
        .containsExactly(line("0102", "Bison bison"), line("0102", "bos taurus"));
  }

  @Test
  void check_asksMdmOncePerCode_andReadsEveryPage() {
    givenMdmSpeciesPage("01061900", 0, page(TWO_PAGES_OF_SPECIES, "Acinonyx sp.", "Addax"));
    givenMdmSpeciesPage("01061900", 1, page(TWO_PAGES_OF_SPECIES, "Pseudoryx", "Pudu"));

    SpeciesCheckResponse response = service.check(request(
        line("01061900", "Addax"),
        line("01061900", "Pudu")));

    assertThat(response.missing()).isEmpty();
    verify(mdmClient, times(1)).getCommoditySpecies(any(), eq("01061900"), any(), eq(0), eq(100));
    verify(mdmClient, times(1)).getCommoditySpecies(any(), eq("01061900"), any(), eq(1), eq(100));
  }

  @Test
  void check_returnsEveryLine_forACodeMdmHasNoSpeciesFor() {
    givenMdmSpeciesPage("030111", 0, page(0));

    SpeciesCheckResponse response = service.check(request(line("030111", "Cyprinus carpio")));

    assertThat(response.missing()).containsExactly(line("030111", "Cyprinus carpio"));
  }

  @Test
  void check_returnsALineWithoutCallingMdm_whenItHasNoCodeOrLatinName() {
    SpeciesCheckResponse response = service.check(request(
        line(null, "Bos taurus"),
        line("0102", null)));

    assertThat(response.missing())
        .containsExactly(line(null, "Bos taurus"), line("0102", null));
    verifyNoInteractions(mdmClient);
  }

  @Test
  void check_returnsNothing_whenThereAreNoLines() {
    SpeciesCheckResponse noBody = service.check(null);
    SpeciesCheckResponse noLines = service.check(new SpeciesCheckRequest(null));

    assertThat(noBody.missing()).isEmpty();
    assertThat(noLines.missing()).isEmpty();
  }

  private void givenMdmSpeciesPage(String code, int page, MdmProductPage<MdmSpecies> speciesPage) {
    when(mdmClient.getCommoditySpecies(any(), eq(code), any(), eq(page), eq(100)))
        .thenReturn(ResponseEntity.ok(speciesPage));
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

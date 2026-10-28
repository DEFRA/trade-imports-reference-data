package uk.gov.defra.trade.imports.commodities;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.defra.trade.imports.commodities.SpeciesCheckService.PAGE_SIZE;

import ch.qos.logback.classic.spi.ILoggingEvent;
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
import uk.gov.defra.trade.imports.exceptions.MdmUnusableResponseException;
import uk.gov.defra.trade.imports.metrics.CapturingLogAppender;

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

    assertThat(response.unlisted())
        .containsExactly(line("0102", "Bison bison"), line("0102", "bos taurus"));
  }

  @Test
  void check_logsTheCodeAndLatinNameOfEveryUnlistedLine() {
    givenMdmSpeciesPage("0102", 0, page(1, "Bos taurus"));

    try (var logs = CapturingLogAppender.attachedTo(SpeciesCheckService.class)) {
      service.check(request(line("0102", "Bos taurus"), line("0102", "Bison bison")));

      assertThat(logs.events()).extracting(ILoggingEvent::getFormattedMessage).containsExactly(
          "Species check found 1 lines MDM does not list under their code: "
              + "[CommodityLine[code=0102, latinName=Bison bison]]");
    }
  }

  @Test
  void check_asksMdmOncePerCode_andReadsEveryPage() {
    givenMdmSpeciesPage("01061900", 0, page(TWO_PAGES_OF_SPECIES, "Acinonyx sp.", "Addax"));
    givenMdmSpeciesPage("01061900", 1, page(TWO_PAGES_OF_SPECIES, "Pseudoryx", "Pudu"));

    SpeciesCheckResponse response = service.check(request(
        line("01061900", "Addax"),
        line("01061900", "Pudu")));

    assertThat(response.unlisted()).isEmpty();
    verify(mdmClient, times(1))
        .getCommoditySpecies(any(), eq("01061900"), any(), eq(0), eq(PAGE_SIZE));
    verify(mdmClient, times(1))
        .getCommoditySpecies(any(), eq("01061900"), any(), eq(1), eq(PAGE_SIZE));
  }

  @Test
  void check_stopsReadingPages_atTheFirstEmptyPage() {
    givenMdmSpeciesPage("01061900", 0, page(300, "Addax"));
    givenMdmSpeciesPage("01061900", 1, page(300));

    SpeciesCheckResponse response = service.check(request(line("01061900", "Addax")));

    assertThat(response.unlisted()).isEmpty();
    verify(mdmClient, never())
        .getCommoditySpecies(any(), eq("01061900"), any(), eq(2), eq(PAGE_SIZE));
  }

  @Test
  void check_throwsMdmUnusableResponse_whenMdmSendsASpeciesPageWithNoTotal() {
    givenMdmSpeciesPage("0102", 0, MdmProductPage.<MdmSpecies>builder()
        .content(List.of(MdmSpecies.builder().latinName("Bos taurus").build()))
        .build());
    SpeciesCheckRequest request = request(line("0102", "Bos taurus"));

    assertThatThrownBy(() -> service.check(request))
        .isInstanceOf(MdmUnusableResponseException.class);
  }

  @Test
  void check_returnsEveryLine_forACodeMdmHasNoSpeciesFor() {
    givenMdmSpeciesPage("030111", 0, page(0));

    SpeciesCheckResponse response = service.check(request(
        line("030111", "Cyprinus carpio"),
        line("030111", "Carassius auratus")));

    assertThat(response.unlisted()).containsExactly(
        line("030111", "Cyprinus carpio"),
        line("030111", "Carassius auratus"));
  }

  @Test
  void check_throwsMdmUnusableResponse_whenMdmSendsNoSpeciesPageForACode() {
    when(mdmClient.getCommoditySpecies(any(), eq("0102"), any(), eq(0), eq(PAGE_SIZE)))
        .thenReturn(ResponseEntity.ok(null));
    SpeciesCheckRequest request = request(line("0102", "Bos taurus"));

    assertThatThrownBy(() -> service.check(request))
        .isInstanceOf(MdmUnusableResponseException.class);
  }

  @Test
  void check_returnsNothingWithoutCallingMdm_whenThereAreNoLines() {
    SpeciesCheckResponse response = service.check(request());

    assertThat(response.unlisted()).isEmpty();
    verifyNoInteractions(mdmClient);
  }

  private void givenMdmSpeciesPage(String code, int page, MdmProductPage<MdmSpecies> speciesPage) {
    when(mdmClient.getCommoditySpecies(any(), eq(code), any(), eq(page), eq(PAGE_SIZE)))
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
            .map(name -> MdmSpecies.builder().latinName(name).build())
            .toList())
        .totalElements(total)
        .build();
  }
}

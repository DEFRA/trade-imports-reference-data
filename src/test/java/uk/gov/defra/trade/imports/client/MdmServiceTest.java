package uk.gov.defra.trade.imports.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.spi.ILoggingEvent;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import uk.gov.defra.trade.imports.client.MdmProductSearchResult.ResultType;
import uk.gov.defra.trade.imports.configuration.MdmConfiguration;
import uk.gov.defra.trade.imports.exceptions.MdmUnusableResponseException;
import uk.gov.defra.trade.imports.metrics.CapturingLogAppender;

@ExtendWith(MockitoExtension.class)
class MdmServiceTest {

  private static final String SUBSCRIPTION_KEY = "test-subscription-key";
  private static final String MDM_TRACE_HEADER = "x-ms-middleware-request-id";

  @Mock
  private MdmClient mdmClient;

  private MdmService mdmService;

  @BeforeEach
  void setUp() {
    MdmConfiguration mdmConfiguration = new MdmConfiguration();
    mdmConfiguration.ocpApimSubscriptionKey = SUBSCRIPTION_KEY;
    mdmService = new MdmService(mdmClient, mdmConfiguration);
  }

  private static <T> ResponseEntity<T> okWithTraceHeader(T body) {
    HttpHeaders headers = new HttpHeaders();
    headers.add(MDM_TRACE_HEADER, "trace-abc-123");
    return ResponseEntity.ok().headers(headers).body(body);
  }

  @Test
  void getPortsOfEntry_returnsBodyFromMdmResponse() {
    List<MdmPortOfEntry> ports = List.of(
        MdmPortOfEntry.builder().id("1").code("GBABE").name("Aberdeen").build()
    );
    when(mdmClient.getPorts(any(), any()))
        .thenReturn(okWithTraceHeader(new MdmPortsResponse(ports)));

    List<MdmPortOfEntry> result = mdmService.getPortsOfEntry();

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getCode()).isEqualTo("GBABE");
    assertThat(result.get(0).getName()).isEqualTo("Aberdeen");
  }

  @Test
  void getPortsOfEntry_throwsMdmUnusableResponse_whenMdmBodyIsNull() {
    when(mdmClient.getPorts(any(), any())).thenReturn(okWithTraceHeader(null));

    assertThatThrownBy(() -> mdmService.getPortsOfEntry())
        .isInstanceOf(MdmUnusableResponseException.class)
        .hasMessage("MDM returned no body for ports-of-entry request for GBNAG");
  }

  @Test
  void getPortsOfEntry_throwsMdmUnusableResponse_whenResultIsNull() {
    when(mdmClient.getPorts(any(), any()))
        .thenReturn(okWithTraceHeader(new MdmPortsResponse(null)));

    assertThatThrownBy(() -> mdmService.getPortsOfEntry())
        .isInstanceOf(MdmUnusableResponseException.class)
        .hasMessage("MDM returned a ports-of-entry body with no result for GBNAG");
  }

  @Test
  void getPortsOfEntry_stillReturnsData_whenMdmTraceHeaderIsAbsent() {
    List<MdmPortOfEntry> ports = List.of(
        MdmPortOfEntry.builder().code("GBABE").name("Aberdeen").build()
    );
    when(mdmClient.getPorts(any(), any())).thenReturn(ResponseEntity.ok(new MdmPortsResponse(ports)));

    List<MdmPortOfEntry> result = mdmService.getPortsOfEntry();

    assertThat(result).hasSize(1);
  }

  @Test
  void getPortsOfEntry_callsMdmWithGbnagSystem() {
    when(mdmClient.getPorts(any(), any()))
        .thenReturn(okWithTraceHeader(new MdmPortsResponse(List.of())));

    mdmService.getPortsOfEntry();

    verify(mdmClient).getPorts(SUBSCRIPTION_KEY, "GBNAG");
  }

  @Test
  void getCountries_returnsBodyFromMdmResponse() {
    List<MdmCountry> mdmCountries = List.of(
        MdmCountry.builder()
            .effectiveAlpha2("FR")
            .effectiveAlias("France")
            .blocks(List.of(MdmBlock.builder().name("GBNAG_SPS_EX").includeCountry(true).build()))
            .build()
    );
    when(mdmClient.getCountries(any(), any(), any())).thenReturn(okWithTraceHeader(mdmCountries));

    List<MdmCountry> result = mdmService.getCountries("GBNAG_SPS_EX");

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getEffectiveAlpha2()).isEqualTo("FR");
  }

  @Test
  void getCountries_stillReturnsCountries_whenMdmTraceHeaderIsAbsent() {
    List<MdmCountry> mdmCountries = List.of(
        MdmCountry.builder()
            .effectiveAlpha2("FR")
            .effectiveAlias("France")
            .blocks(List.of(MdmBlock.builder().name("GBNAG_SPS_EX").includeCountry(true).build()))
            .build()
    );
    when(mdmClient.getCountries(any(), any(), any())).thenReturn(ResponseEntity.ok(mdmCountries));

    List<MdmCountry> result = mdmService.getCountries("GBNAG_SPS_EX");

    assertThat(result).hasSize(1);
  }

  @Test
  void getCountries_callsMdmWithGbnagSystemAndPassedBlocksParam() {
    when(mdmClient.getCountries(any(), any(), any())).thenReturn(okWithTraceHeader(List.of()));

    mdmService.getCountries("GBNAG_SPS_EX");

    verify(mdmClient).getCountries(SUBSCRIPTION_KEY, "GBNAG", "GBNAG_SPS_EX");
  }

  @Test
  void getCountries_filtersOutUk() {
    List<MdmCountry> mdmCountries = List.of(
        MdmCountry.builder()
            .effectiveAlpha2("GB")
            .effectiveAlias("United Kingdom")
            .blocks(List.of(MdmBlock.builder().name("GBNAG_SPS_EX").includeCountry(true).build()))
            .build()
    );
    when(mdmClient.getCountries(any(), any(), any())).thenReturn(okWithTraceHeader(mdmCountries));

    List<MdmCountry> result = mdmService.getCountries("GBNAG_SPS_EX");

    assertThat(result).isEmpty();
  }

  @Test
  void getCountries_filtersOutCountriesWhereGbnagSpsExBlockHasIncludeCountryFalse() {
    List<MdmCountry> mdmCountries = List.of(
        MdmCountry.builder()
            .effectiveAlpha2("MQ")
            .effectiveAlias("Martinique")
            .blocks(List.of(
                MdmBlock.builder().name("OMR").includeCountry(true).build(),
                MdmBlock.builder().name("GBNAG_SPS_EX").includeCountry(false).build()
            ))
            .build()
    );
    when(mdmClient.getCountries(any(), any(), any())).thenReturn(okWithTraceHeader(mdmCountries));

    List<MdmCountry> result = mdmService.getCountries("GBNAG_SPS_EX");

    assertThat(result).isEmpty();
  }

  @Test
  void getCountries_throwsMdmUnusableResponse_whenMdmBodyIsNull() {
    when(mdmClient.getCountries(any(), any(), any())).thenReturn(okWithTraceHeader(null));

    assertThatThrownBy(() -> mdmService.getCountries("GBNAG_SPS_EX"))
        .isInstanceOf(MdmUnusableResponseException.class)
        .hasMessage("MDM returned no body for countries request for GBNAG_SPS_EX");
  }

  @Test
  void searchProducts_callsMdmWithGbnagDatasetAndPaging() {
    when(mdmClient.searchProducts(any(), any(), any(), anyInt(), anyInt()))
        .thenReturn(ResponseEntity.ok(new MdmProductPage<>(List.of(), 0L)));

    mdmService.searchProducts("cattle", 0, 100);

    verify(mdmClient).searchProducts(SUBSCRIPTION_KEY, "cattle", "gbnag", 0, 100);
  }

  @Test
  void searchProducts_logsTheTraceIdWithTheRequestAndTerm() {
    when(mdmClient.searchProducts(any(), any(), any(), anyInt(), anyInt()))
        .thenReturn(okWithTraceHeader(new MdmProductPage<>(List.of(), 0L)));

    try (var logs = CapturingLogAppender.attachedTo(MdmService.class)) {
      mdmService.searchProducts("cattle", 0, 100);

      assertThat(logs.events()).extracting(ILoggingEvent::getFormattedMessage)
          .containsExactly("MDM trace id for product search request for cattle is trace-abc-123");
    }
  }

  @Test
  void getCommoditySpecies_logsAMissingTraceIdWithTheRequestAndCode() {
    when(mdmClient.getCommoditySpecies(any(), any(), any(), anyInt(), anyInt()))
        .thenReturn(ResponseEntity.ok(new MdmProductPage<>(List.of(), 0L)));

    try (var logs = CapturingLogAppender.attachedTo(MdmService.class)) {
      mdmService.getCommoditySpecies("0102", 0, 100);

      assertThat(logs.events()).extracting(ILoggingEvent::getFormattedMessage)
          .containsExactly("No MDM trace id returned for commodity species request for 0102");
    }
  }

  @Test
  void searchProducts_returnsBodyFromMdmResponse() {
    MdmProductSearchResult bosTaurus = MdmProductSearchResult.builder()
        .resultType(ResultType.SPECIES)
        .latinName("Bos taurus")
        .commonName("Cattle or Cow")
        .commodityCnCode("0102")
        .build();
    when(mdmClient.searchProducts(any(), any(), any(), anyInt(), anyInt()))
        .thenReturn(ResponseEntity.ok(new MdmProductPage<>(List.of(bosTaurus), 8L)));

    MdmProductPage<MdmProductSearchResult> productPage =
        mdmService.searchProducts("cattle", 0, 100);

    assertThat(productPage.content()).containsExactly(bosTaurus);
    assertThat(productPage.totalElements()).isEqualTo(8L);
  }

  @Test
  void searchProducts_throwsMdmUnusableResponse_whenMdmBodyIsNull() {
    when(mdmClient.searchProducts(any(), any(), any(), anyInt(), anyInt()))
        .thenReturn(ResponseEntity.ok(null));

    assertThatThrownBy(() -> mdmService.searchProducts("cattle", 0, 100))
        .isInstanceOf(MdmUnusableResponseException.class)
        .hasMessage("MDM returned no body for product search request for cattle");
  }

  @Test
  void searchProducts_throwsMdmUnusableResponse_whenMdmContentIsNull() {
    when(mdmClient.searchProducts(any(), any(), any(), anyInt(), anyInt()))
        .thenReturn(ResponseEntity.ok(new MdmProductPage<>(null, 5L)));

    assertThatThrownBy(() -> mdmService.searchProducts("cattle", 0, 100))
        .isInstanceOf(MdmUnusableResponseException.class)
        .hasMessage("MDM returned a product search page with no content for cattle");
  }

  @Test
  void searchProducts_throwsMdmUnusableResponse_whenMdmTotalIsNull() {
    when(mdmClient.searchProducts(any(), any(), any(), anyInt(), anyInt()))
        .thenReturn(ResponseEntity.ok(new MdmProductPage<>(List.of(), null)));

    assertThatThrownBy(() -> mdmService.searchProducts("cattle", 0, 100))
        .isInstanceOf(MdmUnusableResponseException.class)
        .hasMessage("MDM returned a product search page with no total for cattle");
  }

  @Test
  void getCommodities_callsMdmWithCodesCommaSeparated() {
    when(mdmClient.getCommodities(any(), any(), any())).thenReturn(ResponseEntity.ok(List.of()));

    mdmService.getCommodities(List.of("0101", "0102", "01061900"));

    verify(mdmClient).getCommodities(SUBSCRIPTION_KEY, "0101,0102,01061900", "gbnag");
  }

  @Test
  void getCommodities_returnsBodyFromMdmResponse() {
    MdmCommodity liveBovineAnimals = MdmCommodity.builder()
        .code("0102")
        .isGerminal(false)
        .build();
    when(mdmClient.getCommodities(any(), any(), any()))
        .thenReturn(ResponseEntity.ok(List.of(liveBovineAnimals)));

    List<MdmCommodity> commodities = mdmService.getCommodities(List.of("0102"));

    assertThat(commodities).containsExactly(liveBovineAnimals);
  }

  @Test
  void getCommodities_doesNotCallMdm_whenThereAreNoCodes() {
    List<MdmCommodity> commodities = mdmService.getCommodities(List.of());

    assertThat(commodities).isEmpty();
    verifyNoInteractions(mdmClient);
  }

  @Test
  void getCommodities_throwsMdmUnusableResponse_whenMdmBodyIsNull() {
    when(mdmClient.getCommodities(any(), any(), any())).thenReturn(ResponseEntity.ok(null));
    List<String> codes = List.of("0102");

    assertThatThrownBy(() -> mdmService.getCommodities(codes))
        .isInstanceOf(MdmUnusableResponseException.class)
        .hasMessage("MDM returned no body for commodities request for [0102]");
  }

  @Test
  void getCommoditySpecies_callsMdmWithCodeGbnagDatasetAndPaging() {
    when(mdmClient.getCommoditySpecies(any(), any(), any(), anyInt(), anyInt()))
        .thenReturn(ResponseEntity.ok(new MdmProductPage<>(List.of(), 0L)));

    mdmService.getCommoditySpecies("01061900", 1, 500);

    verify(mdmClient).getCommoditySpecies(SUBSCRIPTION_KEY, "01061900", "gbnag", 1, 500);
  }

  @Test
  void getCommoditySpecies_returnsBodyFromMdmResponse() {
    MdmSpecies felisCatus = MdmSpecies.builder()
        .latinName("Felis catus")
        .commonName("Domestic Cat")
        .build();
    when(mdmClient.getCommoditySpecies(any(), any(), any(), anyInt(), anyInt()))
        .thenReturn(ResponseEntity.ok(new MdmProductPage<>(List.of(felisCatus), 120L)));

    MdmProductPage<MdmSpecies> speciesPage = mdmService.getCommoditySpecies("01061900", 0, 500);

    assertThat(speciesPage.content()).containsExactly(felisCatus);
    assertThat(speciesPage.totalElements()).isEqualTo(120L);
  }

  @Test
  void getCommoditySpecies_throwsMdmUnusableResponse_whenMdmBodyIsNull() {
    when(mdmClient.getCommoditySpecies(any(), any(), any(), anyInt(), anyInt()))
        .thenReturn(ResponseEntity.ok(null));

    assertThatThrownBy(() -> mdmService.getCommoditySpecies("01061900", 0, 500))
        .isInstanceOf(MdmUnusableResponseException.class)
        .hasMessage("MDM returned no body for commodity species request for 01061900");
  }

  @Test
  void getCountries_doesNotApplyBlockFilter_whenBlocksParamIsNull() {
    List<MdmCountry> mdmCountries = List.of(
        MdmCountry.builder()
            .effectiveAlpha2("DE")
            .effectiveAlias("Germany")
            .blocks(List.of(MdmBlock.builder().name("GBNAG_SPS_EX").includeCountry(false).build()))
            .build(),
        MdmCountry.builder()
            .effectiveAlpha2("MQ")
            .effectiveAlias("Martinique")
            .blocks(List.of(MdmBlock.builder().name("OMR").includeCountry(true).build()))
            .build()
    );
    when(mdmClient.getCountries(any(), any(), any())).thenReturn(okWithTraceHeader(mdmCountries));

    List<MdmCountry> result = mdmService.getCountries(null);

    assertThat(result).hasSize(2);
  }
}

package uk.gov.defra.trade.imports.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import uk.gov.defra.trade.imports.configuration.MdmConfiguration;

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

  private ResponseEntity<List<MdmCountry>> responseWith(List<MdmCountry> body) {
    HttpHeaders headers = new HttpHeaders();
    headers.add(MDM_TRACE_HEADER, "trace-abc-123");
    return ResponseEntity.ok().headers(headers).body(body);
  }

  @Test
  void getCountries_returnsBodyFromMdmResponse() {
    // Given
    List<MdmCountry> mdmCountries = List.of(
        MdmCountry.builder()
            .effectiveAlpha2("FR")
            .effectiveAlias("France")
            .blocks(List.of(MdmBlock.builder().name("GBNAG_SPS_EX").includeCountry(true).build()))
            .build()
    );
    when(mdmClient.getCountries(any(), any(), any())).thenReturn(responseWith(mdmCountries));

    // When
    List<MdmCountry> result = mdmService.getCountries("GBNAG_SPS_EX");

    // Then
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getEffectiveAlpha2()).isEqualTo("FR");
  }

  @Test
  void getCountries_stillReturnsCountries_whenMdmTraceHeaderIsAbsent() {
    // Given: response has no x-ms-middleware-request-id header
    List<MdmCountry> mdmCountries = List.of(
        MdmCountry.builder()
            .effectiveAlpha2("FR")
            .effectiveAlias("France")
            .blocks(List.of(MdmBlock.builder().name("GBNAG_SPS_EX").includeCountry(true).build()))
            .build()
    );
    when(mdmClient.getCountries(any(), any(), any())).thenReturn(ResponseEntity.ok(mdmCountries));

    // When / Then: missing trace header is logged as a warning; countries are still returned
    List<MdmCountry> result = mdmService.getCountries("GBNAG_SPS_EX");
    assertThat(result).hasSize(1);
  }

  @Test
  void getCountries_callsMdmWithGbnagSystemAndPassedBlocksParam() {
    // Given
    when(mdmClient.getCountries(any(), any(), any())).thenReturn(responseWith(List.of()));

    // When
    mdmService.getCountries("GBNAG_SPS_EX");

    // Then: system is always "GBNAG" and blocks param is passed through
    verify(mdmClient).getCountries(SUBSCRIPTION_KEY, "GBNAG", "GBNAG_SPS_EX");
  }

  @Test
  void getCountries_filtersOutUk() {
    // Given: MDM returns a UK country
    List<MdmCountry> mdmCountries = List.of(
        MdmCountry.builder()
            .effectiveAlpha2("GB")
            .effectiveAlias("United Kingdom")
            .blocks(List.of(MdmBlock.builder().name("GBNAG_SPS_EX").includeCountry(true).build()))
            .build()
    );
    when(mdmClient.getCountries(any(), any(), any())).thenReturn(responseWith(mdmCountries));

    // When
    List<MdmCountry> result = mdmService.getCountries("GBNAG_SPS_EX");

    // Then: UK is filtered out
    assertThat(result).isEmpty();
  }

  @Test
  void getCountries_filtersOutCountriesWhereGbnagSpsExBlockHasIncludeCountryFalse() {
    // Given: MDM returns a country with includeCountry=false for requested block
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
    when(mdmClient.getCountries(any(), any(), any())).thenReturn(responseWith(mdmCountries));

    // When
    List<MdmCountry> result = mdmService.getCountries("GBNAG_SPS_EX");

    // Then: country with includeCountry=false for requested block is filtered out
    assertThat(result).isEmpty();
  }

  @Test
  void getCountries_returnsEmptyList_whenMdmBodyIsNull() {
    // Given: MDM returns 200 but with a null body
    HttpHeaders headers = new HttpHeaders();
    headers.add(MDM_TRACE_HEADER, "trace-null-body");
    ResponseEntity<List<MdmCountry>> nullBodyResponse =
        ResponseEntity.ok().headers(headers).body(null);
    when(mdmClient.getCountries(any(), any(), any())).thenReturn(nullBodyResponse);

    // When
    List<MdmCountry> result = mdmService.getCountries("GBNAG_SPS_EX");

    // Then: null body is treated as empty list rather than NPE
    assertThat(result).isEmpty();
  }

  @Test
  void getCountries_doesNotApplyBlockFilter_whenBlocksParamIsNull() {
    // Given: MDM returns multiple non-UK countries
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
    when(mdmClient.getCountries(any(), any(), any())).thenReturn(responseWith(mdmCountries));

    // When: no blocks filter applied
    List<MdmCountry> result = mdmService.getCountries(null);

    // Then: all non-UK countries returned regardless of blocks
    assertThat(result).hasSize(2);
  }
}

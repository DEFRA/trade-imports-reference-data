package uk.gov.defra.trade.imports.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
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
  void getCountries_joinsClassifiersAsCommaSeparatedParam() {
    // Given
    when(mdmClient.getCountries(any(), any(), any())).thenReturn(responseWith(List.of()));

    // When
    mdmService.getCountries(List.of("EU", "EFTA", "CTC"));

    // Then: classifiers joined and subscription key passed, system param is null
    verify(mdmClient).getCountries(SUBSCRIPTION_KEY, null, "EU,EFTA,CTC");
  }

  @Test
  void getCountries_passesNullClassifier_whenClassifiersListIsEmpty() {
    // Given
    when(mdmClient.getCountries(any(), any(), isNull())).thenReturn(responseWith(List.of()));

    // When
    mdmService.getCountries(List.of());

    // Then
    verify(mdmClient).getCountries(SUBSCRIPTION_KEY, null, null);
  }

  @Test
  void getCountries_passesNullClassifier_whenClassifiersIsNull() {
    // Given
    when(mdmClient.getCountries(any(), any(), isNull())).thenReturn(responseWith(List.of()));

    // When
    mdmService.getCountries(null);

    // Then
    verify(mdmClient).getCountries(SUBSCRIPTION_KEY, null, null);
  }

  @Test
  void getCountries_returnsBodyFromMdmResponse() {
    // Given
    List<MdmCountry> expected = List.of(
        MdmCountry.builder().alpha2("GB").name("United Kingdom").build()
    );
    when(mdmClient.getCountries(any(), any(), any())).thenReturn(responseWith(expected));

    // When
    List<MdmCountry> result = mdmService.getCountries(List.of("EU"));

    // Then
    assertThat(result).isSameAs(expected);
  }

  @Test
  void getCountries_stillReturnsCountries_whenMdmTraceHeaderIsAbsent() {
    // Given: response has no x-ms-middleware-request-id header
    List<MdmCountry> expected = List.of(
        MdmCountry.builder().alpha2("GB").name("United Kingdom").build()
    );
    when(mdmClient.getCountries(any(), any(), any())).thenReturn(ResponseEntity.ok(expected));

    // When / Then: missing trace header is logged as a warning; countries are still returned
    List<MdmCountry> result = mdmService.getCountries(List.of("EU"));
    assertThat(result).isSameAs(expected);
  }
}

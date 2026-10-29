package uk.gov.defra.trade.imports.interceptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;

import feign.FeignException;
import feign.Request;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import feign.Response;
import feign.RetryableException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.defra.trade.imports.client.Token;
import uk.gov.defra.trade.imports.client.TradeApiClient;
import uk.gov.defra.trade.imports.configuration.TradeAuthConfig;
import uk.gov.defra.trade.imports.exceptions.TokenEndpointUnavailableException;

@ExtendWith(MockitoExtension.class)
class MdmClientInterceptorTest {

  @Mock
  private TradeAuthConfig tradeAuthConfig;

  @Mock
  private TradeApiClient tradeApiClient;

  private RequestInterceptor interceptor;

  @BeforeEach
  void setUp() {
    when(tradeAuthConfig.getClientId()).thenReturn("client-id");
    when(tradeAuthConfig.getClientSecret()).thenReturn("client-secret");
    when(tradeAuthConfig.getScope()).thenReturn("scope");
    interceptor = new MdmClientInterceptor(tradeAuthConfig, tradeApiClient).mdmRequestInterceptor();
  }

  @Test
  void apply_fetchesTokenAndAddsBearerHeader_onFirstCall() {
    // Given
    when(tradeApiClient.getTradeAuthToken(any())).thenReturn(new Token(9999999999999L, "my-access-token"));

    // When
    RequestTemplate requestTemplate = new RequestTemplate();
    interceptor.apply(requestTemplate);

    // Then
    assertThat(requestTemplate.headers().get(AUTHORIZATION)).containsExactly("Bearer my-access-token");
    verify(tradeApiClient, times(1)).getTradeAuthToken(any());
  }

  @Test
  void apply_usesCachedToken_whenTokenHasNotExpired() {
    // Given: token with far-future expiry
    when(tradeApiClient.getTradeAuthToken(any())).thenReturn(new Token(9999999999999L, "my-access-token"));

    // When: interceptor applied twice
    interceptor.apply(new RequestTemplate());
    interceptor.apply(new RequestTemplate());

    // Then: trade API called only once — cached token reused
    verify(tradeApiClient, times(1)).getTradeAuthToken(any());
  }

  @Test
  void apply_refetchesToken_whenExpiresOnIsNull() {
    // Given: null expiresOn causes expiry to be set to the past
    when(tradeApiClient.getTradeAuthToken(any())).thenReturn(new Token(null, "my-access-token"));

    // When: interceptor applied twice
    interceptor.apply(new RequestTemplate());
    interceptor.apply(new RequestTemplate());

    // Then: token fetched each time as it is always considered expired
    verify(tradeApiClient, times(2)).getTradeAuthToken(any());
  }

  @ParameterizedTest
  @ValueSource(ints = {502, 503, 504})
  void apply_throwsTokenEndpointUnavailable_whenTheTokenEndpointAnswersAnUnavailableStatus(
      int status) {
    FeignException unavailable = tokenEndpointAnswering(status);
    when(tradeApiClient.getTradeAuthToken(any())).thenThrow(unavailable);
    RequestTemplate requestTemplate = new RequestTemplate();

    assertThatThrownBy(() -> interceptor.apply(requestTemplate))
        .isInstanceOf(TokenEndpointUnavailableException.class)
        .hasMessage("Token endpoint unavailable")
        .hasCause(unavailable);
  }

  @Test
  void apply_throwsTokenEndpointUnavailable_whenTheTokenEndpointDropsTheConnection() {
    RetryableException dropped = new RetryableException(
        -1,
        "connection reset",
        Request.HttpMethod.POST,
        new RuntimeException("reset"),
        0L,
        tokenRequest());
    when(tradeApiClient.getTradeAuthToken(any())).thenThrow(dropped);
    RequestTemplate requestTemplate = new RequestTemplate();

    assertThatThrownBy(() -> interceptor.apply(requestTemplate))
        .isInstanceOf(TokenEndpointUnavailableException.class)
        .hasMessage("Token endpoint unavailable")
        .hasCause(dropped);
  }

  @Test
  void apply_rethrowsTheTokenEndpointsError_whenItAnswersAnyOtherStatus() {
    FeignException unauthorised = tokenEndpointAnswering(401);
    when(tradeApiClient.getTradeAuthToken(any())).thenThrow(unauthorised);
    RequestTemplate requestTemplate = new RequestTemplate();

    assertThatThrownBy(() -> interceptor.apply(requestTemplate)).isSameAs(unauthorised);
  }

  private static FeignException tokenEndpointAnswering(int status) {
    return FeignException.errorStatus(
        "TradeApiClient#getTradeAuthToken",
        Response.builder()
            .status(status)
            .reason("Token endpoint error")
            .request(tokenRequest())
            .headers(Collections.emptyMap())
            .build());
  }

  private static Request tokenRequest() {
    return Request.create(
        Request.HttpMethod.POST,
        "http://trade-auth/token",
        Collections.emptyMap(),
        null,
        StandardCharsets.UTF_8,
        null);
  }
}

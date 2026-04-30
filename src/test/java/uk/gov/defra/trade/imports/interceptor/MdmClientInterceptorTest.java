package uk.gov.defra.trade.imports.interceptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.defra.trade.imports.client.Token;
import uk.gov.defra.trade.imports.client.TradeApiClient;
import uk.gov.defra.trade.imports.configuration.TradeAuthConfig;

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
}

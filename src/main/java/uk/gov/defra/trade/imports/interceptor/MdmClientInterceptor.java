package uk.gov.defra.trade.imports.interceptor;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

import feign.FeignException;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import feign.RetryableException;
import feign.codec.ErrorDecoder;
import java.time.Instant;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import uk.gov.defra.trade.imports.client.MdmErrorDecoder;
import uk.gov.defra.trade.imports.client.Token;
import uk.gov.defra.trade.imports.client.TradeApiClient;
import uk.gov.defra.trade.imports.configuration.TradeAuthConfig;
import uk.gov.defra.trade.imports.exceptions.TokenEndpointUnavailableException;

@AllArgsConstructor
public class MdmClientInterceptor {

  private final TradeAuthConfig tradeAuthConfig;
  private final TradeApiClient tradeApiClient;

  @Bean
  public ErrorDecoder mdmErrorDecoder() {
    return new MdmErrorDecoder();
  }

  @Bean
  public RequestInterceptor mdmRequestInterceptor() {
    return new RequestInterceptor() {
      String accessToken;
      Instant expiryTime = Instant.now().minusSeconds(10); // Initially expired

      @Override
      public void apply(RequestTemplate requestTemplate) {
        if (tokenExpired()) {
          getToken();
        }

        requestTemplate.header(AUTHORIZATION, "Bearer " + accessToken);
      }

      public boolean tokenExpired() {
        return Instant.now().isAfter(expiryTime);
      }

      public void getToken() {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "client_credentials");
        formData.add("client_id", tradeAuthConfig.getClientId());
        formData.add("client_secret", tradeAuthConfig.getClientSecret());
        formData.add("scope", tradeAuthConfig.getScope());

        Token token;
        try {
          token = tradeApiClient.getTradeAuthToken(formData);
        } catch (RetryableException ex) {
          throw new TokenEndpointUnavailableException("Token endpoint unavailable", ex);
        } catch (FeignException ex) {
          if (ex.status() == 502 || ex.status() == 503 || ex.status() == 504) {
            throw new TokenEndpointUnavailableException("Token endpoint unavailable", ex);
          }
          throw ex;
        }

        accessToken = token.getAccessToken();
        expiryTime = token.getExpiresOn() != null
            ? Instant.ofEpochMilli(token.getExpiresOn())
            : Instant.now().minusSeconds(10);
      }
    };
  }
}

package uk.gov.defra.trade.imports.interceptor;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import java.time.Instant;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import uk.gov.defra.trade.imports.client.Token;
import uk.gov.defra.trade.imports.client.TradeApiClient;
import uk.gov.defra.trade.imports.configuration.TradeAuthConfig;

@AllArgsConstructor
public class MdmApiClientInterceptor {

    private final TradeAuthConfig tradeAuthConfig;
    private final TradeApiClient tradeApiClient;

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

                Token token = tradeApiClient.getTradeAuthToken(formData);
                
                accessToken = token.getAccessToken();
                expiryTime = token.getExpiresOn() != null 
                    ? Instant.ofEpochMilli(token.getExpiresOn()) 
                    : Instant.now().minusSeconds(10);
            }
        };
    }
}

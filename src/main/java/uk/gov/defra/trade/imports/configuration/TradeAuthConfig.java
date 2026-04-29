package uk.gov.defra.trade.imports.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Setter
@Getter
@Component
@ConfigurationProperties(prefix = "trade-auth")
public class TradeAuthConfig {

    private String clientId;
    private String clientSecret;
    private String scope;
    private int connectionTimeout;
    private int readTimeout;

}

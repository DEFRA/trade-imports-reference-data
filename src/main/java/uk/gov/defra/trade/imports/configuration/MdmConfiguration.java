package uk.gov.defra.trade.imports.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Setter
@Getter
@Component
@ConfigurationProperties(prefix = "mdm-service")
public class MdmConfiguration {

  public String url;
  public String ocpApimSubscriptionKey;
  public int connectionTimeout;
  public int readTimeout;
}

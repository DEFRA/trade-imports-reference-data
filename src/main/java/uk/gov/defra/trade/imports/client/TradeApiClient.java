package uk.gov.defra.trade.imports.client;

import java.util.Map;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
    name = "trade-client", 
    url="${trade-auth.api.url}"
)
public interface TradeApiClient {
    
    @PostMapping(produces = "application/json", consumes = "application/x-www-form-urlencoded")
    Token getTradeAuthToken(@RequestBody Map<String, ?> form);

}

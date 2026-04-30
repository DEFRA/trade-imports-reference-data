package uk.gov.defra.trade.imports.client;

import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import uk.gov.defra.trade.imports.interceptor.MdmClientInterceptor;

@FeignClient(
    name = "mdm-client",
    url = "${mdm-service.url}",
    configuration = MdmClientInterceptor.class
)
public interface MdmClient {

  String OCP_APIM_SUBSCRIPTION_KEY = "Ocp-Apim-Subscription-Key";

  @GetMapping(value = "/mdm/geo/countries")
  ResponseEntity<List<MdmCountry>> getCountries(
      @RequestHeader(OCP_APIM_SUBSCRIPTION_KEY) String ocpApimSubscriptionKey,
      @RequestParam(value = "system", required = false) String system,
      @RequestParam(value = "classifier", required = false) String classifier
  );
}

package uk.gov.defra.trade.imports.client;

import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
      @RequestParam(value = "blocks", required = false) String blocks
  );

  @GetMapping(value = "/mdm/trade/bcp/poes")
  ResponseEntity<MdmPortsResponse> getPorts(
      @RequestHeader(OCP_APIM_SUBSCRIPTION_KEY) String ocpApimSubscriptionKey,
      @RequestParam(value = "system", required = false) String system
  );

  @GetMapping(value = "/mdm/trade/product/search")
  ResponseEntity<MdmProductPage<MdmProductSearchResult>> searchProducts(
      @RequestHeader(OCP_APIM_SUBSCRIPTION_KEY) String ocpApimSubscriptionKey,
      @RequestParam(value = "q") String query,
      @RequestParam(value = "dataset") String dataset,
      @RequestParam(value = "page") int page,
      @RequestParam(value = "size") int size
  );

  @GetMapping(value = "/mdm/trade/product/commodities")
  ResponseEntity<List<MdmCommodity>> getCommodities(
      @RequestHeader(OCP_APIM_SUBSCRIPTION_KEY) String ocpApimSubscriptionKey,
      @RequestParam(value = "codes") String commaSeparatedCodes,
      @RequestParam(value = "dataset") String dataset
  );

  @GetMapping(value = "/mdm/trade/product/commodities/{code}/species")
  ResponseEntity<MdmProductPage<MdmSpecies>> getCommoditySpecies(
      @RequestHeader(OCP_APIM_SUBSCRIPTION_KEY) String ocpApimSubscriptionKey,
      @PathVariable("code") String code,
      @RequestParam(value = "dataset") String dataset,
      @RequestParam(value = "page") int page,
      @RequestParam(value = "size") int size
  );
}

package uk.gov.defra.trade.imports.client;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import uk.gov.defra.trade.imports.configuration.MdmConfiguration;

@Slf4j
@AllArgsConstructor
@Service
public class MdmService {

  private static final String MDM_API_TRACE_ID_KEY = "x-ms-middleware-request-id";

  private final MdmClient mdmClient;
  private final MdmConfiguration mdmConfiguration;

  @Cacheable(value = "MDM_COUNTRIES_CACHE", unless = "#result == null || #result.isEmpty()")
  public List<MdmCountry> getCountries(List<String> classifiers) {

    String ocpApimSubscriptionKey = mdmConfiguration.ocpApimSubscriptionKey;

    String classifiersParam = classifiers != null && !classifiers.isEmpty()
        ? String.join(",", classifiers)
        : null;

    ResponseEntity<List<MdmCountry>> responseEntity =
        mdmClient.getCountries(ocpApimSubscriptionKey, null, classifiersParam);
    logTraceId(responseEntity);

    return responseEntity.getBody();
  }

  private void logTraceId(ResponseEntity<?> responseEntity) {
    List<String> traceHeaders = responseEntity.getHeaders().get(MDM_API_TRACE_ID_KEY);
    if (traceHeaders == null || traceHeaders.isEmpty()) {
      log.warn("No MDM trace id returned");
      return;
    }
    traceHeaders.stream().findFirst()
        .ifPresent(mdmApiTraceId -> log.info("MDM trace id for this call is: {}", mdmApiTraceId));
  }
}

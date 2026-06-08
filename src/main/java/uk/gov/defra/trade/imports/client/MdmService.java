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
  private static final String SYSTEM = "GBNAG";

  private final MdmClient mdmClient;
  private final MdmConfiguration mdmConfiguration;

  @Cacheable(value = "MDM_COUNTRIES_CACHE", unless = "#result == null || #result.isEmpty()")
  public List<MdmCountry> getCountries(String blocks) {
    String ocpApimSubscriptionKey = mdmConfiguration.getOcpApimSubscriptionKey();

    ResponseEntity<List<MdmCountry>> responseEntity =
        mdmClient.getCountries(ocpApimSubscriptionKey, SYSTEM, blocks);
    logTraceId(responseEntity);

    List<MdmCountry> body = responseEntity.getBody();
    if (body == null) {
      log.warn("MDM returned a null body for countries request");
      return List.of();
    }
    return body.stream()
        .filter(c -> !"GB".equals(c.getEffectiveAlpha2()))
        .filter(c -> isIncludedInRequestedBlock(c, blocks))
        .toList();
  }

  private boolean isIncludedInRequestedBlock(MdmCountry country, String requestedBlock) {
    if (requestedBlock == null || country.getBlocks() == null) return true;
    return country.getBlocks().stream()
        .anyMatch(b -> requestedBlock.equals(b.getName())
                 && Boolean.TRUE.equals(b.getIncludeCountry()));
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

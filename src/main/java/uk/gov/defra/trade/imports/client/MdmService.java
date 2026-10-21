package uk.gov.defra.trade.imports.client;

import java.util.Collection;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import uk.gov.defra.trade.imports.configuration.CacheConfig;
import uk.gov.defra.trade.imports.configuration.MdmConfiguration;

@Slf4j
@AllArgsConstructor
@Service
public class MdmService {

  private static final String MDM_API_TRACE_ID_KEY = "x-ms-middleware-request-id";
  private static final String SYSTEM = "GBNAG";
  private static final String DATASET = "gbnag";

  private final MdmClient mdmClient;
  private final MdmConfiguration mdmConfiguration;

  @Cacheable(value = CacheConfig.MDM_POE_CACHE, unless = "#result == null || #result.isEmpty()")
  public List<MdmPortOfEntry> getPortsOfEntry() {
    ResponseEntity<MdmPortsResponse> responseEntity =
        mdmClient.getPorts(mdmConfiguration.getOcpApimSubscriptionKey(), SYSTEM);
    logTraceId(responseEntity);
    MdmPortsResponse body = responseEntity.getBody();
    if (body == null || body.getResult() == null) {
      log.warn("MDM returned a null body for ports-of-entry request");
      return List.of();
    }
    return body.getResult();
  }

  @Cacheable(value = CacheConfig.MDM_COUNTRIES_CACHE, unless = "#result == null || #result.isEmpty()")
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

  public MdmProductPage<MdmProductSearchResult> searchProducts(String query, int page, int size) {
    ResponseEntity<MdmProductPage<MdmProductSearchResult>> responseEntity = mdmClient.searchProducts(
        mdmConfiguration.getOcpApimSubscriptionKey(), query, DATASET, page, size);
    logTraceId(responseEntity);
    return pageOrEmpty(responseEntity.getBody(), "product search", query);
  }

  public List<MdmCommodity> getCommodities(Collection<String> codes) {
    if (codes.isEmpty()) {
      return List.of();
    }
    ResponseEntity<List<MdmCommodity>> responseEntity = mdmClient.getCommodities(
        mdmConfiguration.getOcpApimSubscriptionKey(), String.join(",", codes), DATASET);
    logTraceId(responseEntity);
    List<MdmCommodity> body = responseEntity.getBody();
    if (body == null) {
      log.warn("MDM returned a null body for commodities request for codes {}", codes);
      return List.of();
    }
    return body;
  }

  public MdmProductPage<MdmSpecies> getCommoditySpecies(String code, int page, int size) {
    ResponseEntity<MdmProductPage<MdmSpecies>> responseEntity = mdmClient.getCommoditySpecies(
        mdmConfiguration.getOcpApimSubscriptionKey(), code, DATASET, page, size);
    logTraceId(responseEntity);
    return pageOrEmpty(responseEntity.getBody(), "commodity species", code);
  }

  private <T> MdmProductPage<T> pageOrEmpty(MdmProductPage<T> body, String requestName,
      String requestedFor) {
    if (body == null || body.content() == null) {
      log.warn("MDM returned a null body for {} request for {}", requestName, requestedFor);
      return MdmProductPage.<T>builder().content(List.of()).totalElements(0L).build();
    }
    return body;
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

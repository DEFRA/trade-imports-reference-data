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
import uk.gov.defra.trade.imports.exceptions.MdmUnavailableException;

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
    MdmPortsResponse body = bodyFrom(responseEntity, "ports-of-entry", SYSTEM);
    if (body.getResult() == null) {
      throw new MdmUnavailableException(
          "MDM returned a ports-of-entry body with no result for " + SYSTEM);
    }
    return body.getResult();
  }

  @Cacheable(value = CacheConfig.MDM_COUNTRIES_CACHE, unless = "#result == null || #result.isEmpty()")
  public List<MdmCountry> getCountries(String blocks) {
    String ocpApimSubscriptionKey = mdmConfiguration.getOcpApimSubscriptionKey();

    ResponseEntity<List<MdmCountry>> responseEntity =
        mdmClient.getCountries(ocpApimSubscriptionKey, SYSTEM, blocks);
    logTraceId(responseEntity);

    return bodyFrom(responseEntity, "countries", blocks).stream()
        .filter(c -> !"GB".equals(c.getEffectiveAlpha2()))
        .filter(c -> isIncludedInRequestedBlock(c, blocks))
        .toList();
  }

  public MdmProductPage<MdmProductSearchResult> searchProducts(String query, int page, int size) {
    ResponseEntity<MdmProductPage<MdmProductSearchResult>> responseEntity = mdmClient.searchProducts(
        mdmConfiguration.getOcpApimSubscriptionKey(), query, DATASET, page, size);
    logTraceId(responseEntity);
    return pageFrom(responseEntity, "product search", query);
  }

  public List<MdmCommodity> getCommodities(Collection<String> codes) {
    if (codes.isEmpty()) {
      return List.of();
    }
    ResponseEntity<List<MdmCommodity>> responseEntity = mdmClient.getCommodities(
        mdmConfiguration.getOcpApimSubscriptionKey(), String.join(",", codes), DATASET);
    logTraceId(responseEntity);
    return bodyFrom(responseEntity, "commodities", codes);
  }

  public MdmProductPage<MdmSpecies> getCommoditySpecies(String code, int page, int size) {
    ResponseEntity<MdmProductPage<MdmSpecies>> responseEntity = mdmClient.getCommoditySpecies(
        mdmConfiguration.getOcpApimSubscriptionKey(), code, DATASET, page, size);
    logTraceId(responseEntity);
    return pageFrom(responseEntity, "commodity species", code);
  }

  private static <T> T bodyFrom(ResponseEntity<T> responseEntity, String requestName,
      Object requestedFor) {
    T body = responseEntity.getBody();
    if (body == null) {
      throw new MdmUnavailableException(
          "MDM returned no body for %s request for %s".formatted(requestName, requestedFor));
    }
    return body;
  }

  private static <T> MdmProductPage<T> pageFrom(ResponseEntity<MdmProductPage<T>> responseEntity,
      String requestName, Object requestedFor) {
    MdmProductPage<T> page = bodyFrom(responseEntity, requestName, requestedFor);
    if (page.content() == null) {
      throw new MdmUnavailableException(
          "MDM returned a %s page with no content for %s".formatted(requestName, requestedFor));
    }
    if (page.totalElements() == null) {
      throw new MdmUnavailableException(
          "MDM returned a %s page with no total for %s".formatted(requestName, requestedFor));
    }
    return page;
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

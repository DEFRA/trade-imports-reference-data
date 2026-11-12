package uk.gov.defra.trade.imports.metrics;

import static uk.gov.defra.trade.imports.metrics.ExternalCall.MDM_GET_COMMODITIES;
import static uk.gov.defra.trade.imports.metrics.ExternalCall.MDM_GET_COMMODITY_SPECIES;
import static uk.gov.defra.trade.imports.metrics.ExternalCall.MDM_GET_COUNTRIES;
import static uk.gov.defra.trade.imports.metrics.ExternalCall.MDM_GET_PORTS_OF_ENTRY;
import static uk.gov.defra.trade.imports.metrics.ExternalCall.MDM_SEARCH_PRODUCTS;
import static uk.gov.defra.trade.imports.metrics.ExternalCall.TRADE_TOKEN;

import feign.Capability;
import feign.Client;
import feign.Request;
import feign.Response;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Times each Feign call mapped in {@link #CALLS_BY_CLIENT_METHOD} and records it: a response of
 * 400 or over, or an exception, is a failure. Any other call passes through unmeasured.
 */
@Component
@RequiredArgsConstructor
public class ExternalCallFeignCapability implements Capability {

  static final Map<String, ExternalCall> CALLS_BY_CLIENT_METHOD =
      Map.of(
          "mdm-client#getCountries", MDM_GET_COUNTRIES,
          "mdm-client#getPorts", MDM_GET_PORTS_OF_ENTRY,
          "mdm-client#searchProducts", MDM_SEARCH_PRODUCTS,
          "mdm-client#getCommodities", MDM_GET_COMMODITIES,
          "mdm-client#getCommoditySpecies", MDM_GET_COMMODITY_SPECIES,
          "trade-client#getTradeAuthToken", TRADE_TOKEN);

  private static final int HTTP_BAD_REQUEST = 400;

  private final ExternalCallRecorder externalCallRecorder;
  private final LastCallOutcomes lastCallOutcomes;

  @Override
  public Client enrich(Client client) {
    return (request, options) -> {
      ExternalCall call = CALLS_BY_CLIENT_METHOD.get(clientMethod(request));
      if (call == null) {
        return client.execute(request, options);
      }
      return measured(call, () -> client.execute(request, options));
    };
  }

  private interface FeignCall {
    Response execute() throws IOException;
  }

  private Response measured(ExternalCall call, FeignCall feignCall) throws IOException {
    long startedAt = System.nanoTime();
    Response response;
    try {
      response = feignCall.execute();
    } catch (IOException | RuntimeException e) {
      externalCallRecorder.recordFailure(call, elapsedSince(startedAt), e);
      lastCallOutcomes.recordFailure(call.dependency());
      throw e;
    }
    if (response.status() >= HTTP_BAD_REQUEST) {
      externalCallRecorder.recordFailure(call, elapsedSince(startedAt), response.status());
      lastCallOutcomes.recordFailure(call.dependency());
    } else {
      externalCallRecorder.recordSuccess(call, elapsedSince(startedAt));
      lastCallOutcomes.recordSuccess(call.dependency());
    }
    return response;
  }

  private static Duration elapsedSince(long startedAt) {
    return Duration.ofNanos(System.nanoTime() - startedAt);
  }

  static String clientMethod(Request request) {
    var template = request.requestTemplate();
    if (template == null
        || template.feignTarget() == null
        || template.methodMetadata() == null) {
      return "";
    }
    return template.feignTarget().name() + "#" + template.methodMetadata().method().getName();
  }
}

package uk.gov.defra.trade.imports.metrics;

import static uk.gov.defra.trade.imports.metrics.ExternalCall.MDM_GET_COUNTRIES;
import static uk.gov.defra.trade.imports.metrics.ExternalCall.MDM_GET_PORTS_OF_ENTRY;
import static uk.gov.defra.trade.imports.metrics.ExternalCall.TRADE_TOKEN;

import feign.Capability;
import feign.Client;
import feign.Request;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Measures every Feign call at the transport, rather than in {@code MdmService}.
 *
 * <p>{@code MdmClientInterceptor} fetches the Trade token inside the MDM request's interceptor, so
 * timing {@code MdmService} would add token time to MDM's figures on every expired-token call.
 * Interceptors have already run before {@link Client#execute}, so each call is timed on its own.
 * One application-level capability covers every Feign client.
 */
@Component
@RequiredArgsConstructor
public class ExternalCallFeignCapability implements Capability {

  static final Map<String, ExternalCall> CALLS_BY_CLIENT_METHOD =
      Map.of(
          "mdm-client#getCountries", MDM_GET_COUNTRIES,
          "mdm-client#getPorts", MDM_GET_PORTS_OF_ENTRY,
          "trade-client#getTradeAuthToken", TRADE_TOKEN);

  private static final int HTTP_BAD_REQUEST = 400;

  private final ExternalCallMetrics externalCallMetrics;

  @Override
  public Client enrich(Client client) {
    return (request, options) -> {
      ExternalCall call = CALLS_BY_CLIENT_METHOD.get(clientMethod(request));
      if (call == null) {
        return client.execute(request, options);
      }
      return externalCallMetrics.measure(
          call,
          () -> client.execute(request, options),
          response -> response.status() >= HTTP_BAD_REQUEST);
    };
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

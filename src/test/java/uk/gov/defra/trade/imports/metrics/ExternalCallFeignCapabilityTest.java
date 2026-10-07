package uk.gov.defra.trade.imports.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import feign.Client;
import feign.MethodMetadata;
import feign.Request;
import feign.RequestTemplate;
import feign.Response;
import feign.Target;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.support.SpringMvcContract;
import software.amazon.cloudwatchlogs.emf.logger.MetricsLogger;
import uk.gov.defra.trade.imports.client.MdmClient;
import uk.gov.defra.trade.imports.client.TradeApiClient;

class ExternalCallFeignCapabilityTest {

  private final CapturingEmfEnvironment environment = new CapturingEmfEnvironment();
  private final ExternalCallFeignCapability capability =
      new ExternalCallFeignCapability(
          new ExternalCallMetrics(
              true, "trade-imports-reference-data", () -> new MetricsLogger(environment)));

  @Test
  void everyFeignMethod_shouldBeMeasured() {
    for (Class<?> client : new Class<?>[] {MdmClient.class, TradeApiClient.class}) {
      String name = client.getAnnotation(FeignClient.class).name();
      Arrays.stream(client.getDeclaredMethods())
          .filter(method -> !method.isSynthetic())
          .forEach(
              method ->
                  assertThat(ExternalCallFeignCapability.CALLS_BY_CLIENT_METHOD)
                      .containsKey(name + "#" + method.getName()));
    }
  }

  @Test
  void enrich_shouldMeasureAKnownCall() throws Exception {
    Response served = response(503);
    Client delegate = (request, options) -> served;
    Client enriched = capability.enrich(delegate);
    MethodMetadata getCountries =
        new SpringMvcContract().parseAndValidateMetadata(MdmClient.class).stream()
            .filter(metadata -> metadata.method().getName().equals("getCountries"))
            .findFirst()
            .orElseThrow();
    var template =
        new RequestTemplate()
            .feignTarget(new Target.HardCodedTarget<>(MdmClient.class, "mdm-client", "http://mdm"))
            .methodMetadata(getCountries);

    Response returned = enriched.execute(request(template), new Request.Options());

    assertThat(returned).isSameAs(served);
    assertThat(environment.documents()).hasSize(1);
    var document = environment.documents().getFirst();
    assertThat(document.get("Operation").asText()).isEqualTo("get-countries");
    assertThat(document.get("ExternalCallFailure").asInt()).isEqualTo(1);
  }

  @Test
  void enrich_shouldPassAnUnknownCallThrough_unmeasured() throws IOException {
    Response served = response(200);
    Client delegate = (request, options) -> served;
    Client enriched = capability.enrich(delegate);

    Response returned = enriched.execute(request(new RequestTemplate()), new Request.Options());

    assertThat(returned).isSameAs(served);
    assertThat(environment.documents()).isEmpty();
  }

  private static Request request(RequestTemplate template) {
    return Request.create(
        Request.HttpMethod.GET,
        "http://mdm/mdm/geo/countries",
        Collections.emptyMap(),
        null,
        StandardCharsets.UTF_8,
        template);
  }

  private static Response response(int status) {
    return Response.builder()
        .status(status)
        .request(request(new RequestTemplate()))
        .headers(Collections.emptyMap())
        .build();
  }
}

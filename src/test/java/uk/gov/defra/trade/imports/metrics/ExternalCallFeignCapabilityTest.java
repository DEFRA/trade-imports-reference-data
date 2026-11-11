package uk.gov.defra.trade.imports.metrics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static uk.gov.defra.trade.imports.testsupport.CapturedLogs.keyValues;
import static uk.gov.defra.trade.imports.testsupport.CapturedLogs.loggedBy;
import static uk.gov.defra.trade.imports.testsupport.ExternalCallMeters.durationCount;
import static uk.gov.defra.trade.imports.testsupport.ExternalCallMeters.failureTotal;

import ch.qos.logback.classic.spi.ILoggingEvent;
import feign.Client;
import feign.MethodMetadata;
import feign.Request;
import feign.RequestTemplate;
import feign.Response;
import feign.Target;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.support.SpringMvcContract;
import uk.gov.defra.trade.imports.client.MdmClient;
import uk.gov.defra.trade.imports.client.TradeApiClient;

class ExternalCallFeignCapabilityTest {

  private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
  private final LastCallOutcomes lastCallOutcomes = new LastCallOutcomes();
  private final ExternalCallFeignCapability capability =
      new ExternalCallFeignCapability(new ExternalCallRecorder(meterRegistry), lastCallOutcomes);

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
  void everyMeasuredCall_shouldBeToAKnownDependency() {
    ExternalCallFeignCapability.CALLS_BY_CLIENT_METHOD
        .values()
        .forEach(call -> assertThat(ExternalCall.DEPENDENCIES).contains(call.dependency()));
  }

  @Test
  void enrich_shouldRecordASuccess_whenTheResponseIsBelow400() throws IOException {
    Response served = response(200);

    Response returned =
        enriched((request, options) -> served).execute(getCountriesRequest(), new Request.Options());

    assertThat(returned).isSameAs(served);
    assertThat(durationCount(meterRegistry, "mdm", "get-countries")).isEqualTo(1);
    assertThat(failureTotal(meterRegistry, "mdm", "get-countries")).isZero();
    assertThat(lastCallOutcomes.lastCallSucceeded("mdm")).contains(true);
  }

  @Test
  void enrich_shouldRecordAFailureWithNoErrorFields_whenTheResponseIs400OrOver()
      throws IOException {
    Client enriched = enriched((request, options) -> response(503));

    List<ILoggingEvent> lines =
        loggedBy(ExternalCallRecorder.class, () -> execute(enriched, getCountriesRequest()));

    assertThat(durationCount(meterRegistry, "mdm", "get-countries")).isEqualTo(1);
    assertThat(failureTotal(meterRegistry, "mdm", "get-countries")).isEqualTo(1);
    assertThat(lastCallOutcomes.lastCallSucceeded("mdm")).contains(false);
    assertThat(keyValues(lines.getFirst()))
        .containsEntry("event.outcome", "failure")
        .doesNotContainKey("error.type");
  }

  @Test
  void enrich_shouldRecordAFailureWithTheCauseAndRethrowIt_whenTheCallThrows() {
    IOException refused = new IOException("connection refused");
    Client enriched =
        enriched(
            (request, options) -> {
              throw refused;
            });

    List<ILoggingEvent> lines =
        loggedBy(
            ExternalCallRecorder.class,
            () ->
                assertThatThrownBy(
                        () -> enriched.execute(getCountriesRequest(), new Request.Options()))
                    .isSameAs(refused));

    assertThat(failureTotal(meterRegistry, "mdm", "get-countries")).isEqualTo(1);
    assertThat(lastCallOutcomes.lastCallSucceeded("mdm")).contains(false);
    assertThat(keyValues(lines.getFirst()))
        .containsEntry("error.type", "java.io.IOException")
        .containsEntry("error.message", "connection refused");
  }

  @Test
  void enrich_shouldPassAnUnknownCallThrough_unmeasured() throws IOException {
    Response served = response(200);

    Response returned =
        enriched((request, options) -> served)
            .execute(request(new RequestTemplate()), new Request.Options());

    assertThat(returned).isSameAs(served);
    assertThat(meterRegistry.getMeters()).isEmpty();
    assertThat(lastCallOutcomes.lastCallSucceeded("mdm")).isEmpty();
  }

  private Client enriched(Client delegate) {
    return capability.enrich(delegate);
  }

  private static void execute(Client client, Request request) {
    try {
      client.execute(request, new Request.Options());
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static Request getCountriesRequest() {
    MethodMetadata getCountries =
        new SpringMvcContract().parseAndValidateMetadata(MdmClient.class).stream()
            .filter(metadata -> metadata.method().getName().equals("getCountries"))
            .findFirst()
            .orElseThrow();
    return request(
        new RequestTemplate()
            .feignTarget(new Target.HardCodedTarget<>(MdmClient.class, "mdm-client", "http://mdm"))
            .methodMetadata(getCountries));
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

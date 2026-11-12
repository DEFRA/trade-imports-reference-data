package uk.gov.defra.trade.imports.integration;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import uk.gov.defra.trade.imports.commodities.SpeciesCheckRequest;

class TimingIT extends IntegrationBase {

  private static final List<String> TIMERS =
      List.of(
          "controller.getCountries.time",
          "controller.getPortsOfEntry.time",
          "controller.searchCommodities.time",
          "controller.checkSpecies.time",
          "service.searchCommodities.time",
          "service.checkSpecies.time");

  @Autowired private MeterRegistry meterRegistry;
  @Autowired private TestRestTemplate restTemplate;

  @BeforeEach
  void stubMdm() {
    stubTradeAuthToken();
    stubMdmCountriesResponse();
    stubMdmPortsResponse();
  }

  @Test
  void timedEndpoints_shouldAddOneCallToTheirOwnTimer_whenCalled() {
    Map<String, Long> before = counts();

    restTemplate.getForEntity("/countries", String.class);
    restTemplate.getForEntity("/ports-of-entry", String.class);
    restTemplate.getForEntity("/commodities?q=d", String.class);
    restTemplate.postForEntity(
        "/commodities/species-check",
        SpeciesCheckRequest.builder().lines(List.of()).build(),
        String.class);

    Map<String, Long> after = counts();
    TIMERS.forEach(name -> assertThat(after.get(name) - before.get(name)).as(name).isEqualTo(1));
  }

  private Map<String, Long> counts() {
    return TIMERS.stream().collect(Collectors.toMap(Function.identity(), this::countOf));
  }

  private long countOf(String name) {
    return meterRegistry.find(name).timers().stream().mapToLong(Timer::count).sum();
  }
}

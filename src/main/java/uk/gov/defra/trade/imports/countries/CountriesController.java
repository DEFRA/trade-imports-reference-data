package uk.gov.defra.trade.imports.countries;

import io.micrometer.core.annotation.Timed;
import java.util.Comparator;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.defra.trade.imports.client.MdmService;

@Slf4j
@RestController
@AllArgsConstructor
@RequestMapping("/countries")
public class CountriesController {

  private final MdmService countriesService;

  @GetMapping()
  @Timed("controller.getCountries.time")
  public ResponseEntity<List<Country>> getCountries(
      @RequestParam(required = false, value = "classifier") List<String> classifiers) {
    List<Country> countries = countriesService.getCountries(classifiers)
        .stream()
        .map(Country::new)
        .sorted(Comparator.comparing(Country::getName))
        .toList();

    return ResponseEntity.ok(countries);
  }
}

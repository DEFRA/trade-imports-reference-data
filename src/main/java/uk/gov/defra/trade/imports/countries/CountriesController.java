package uk.gov.defra.trade.imports.countries;

import io.micrometer.core.annotation.Timed;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@AllArgsConstructor
@RequestMapping("/countries")
public class CountriesController {
    
    private final MdmCountriesService countriesService;
    
    @GetMapping()
    @Timed("controller.getCountries.time")
    public ResponseEntity<List<MdmCountry>> getCountries() {
          return ResponseEntity.ok(countriesService.getCountries(null));
    }
}

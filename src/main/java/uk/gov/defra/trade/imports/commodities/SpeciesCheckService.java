package uk.gov.defra.trade.imports.commodities;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.defra.trade.imports.client.MdmProductPage;
import uk.gov.defra.trade.imports.client.MdmService;
import uk.gov.defra.trade.imports.client.MdmSpecies;

@Service
@AllArgsConstructor
public class SpeciesCheckService {

  static final int PAGE_SIZE = 100;

  private final MdmService mdmService;

  /**
   * Returns the lines whose Latin name MDM no longer lists under their code, matched exactly. A
   * renamed species looks the same as a removed one.
   */
  public SpeciesCheckResponse check(SpeciesCheckRequest request) {
    List<CommodityLine> lines =
        request == null || request.getLines() == null ? List.of() : request.getLines();
    Map<String, Set<String>> speciesByCode = new HashMap<>();
    List<CommodityLine> missing = lines.stream()
        .filter(line -> line.getCode() == null || line.getLatinName() == null
            || !speciesByCode.computeIfAbsent(line.getCode(), this::speciesOf)
                .contains(line.getLatinName()))
        .toList();
    return SpeciesCheckResponse.builder().missing(missing).build();
  }

  private Set<String> speciesOf(String code) {
    Set<String> names = new HashSet<>();
    int page = 0;
    while (true) {
      MdmProductPage<MdmSpecies> result = mdmService.getCommoditySpecies(code, page, PAGE_SIZE);
      result.getContent().forEach(species -> names.add(species.getSpeciesName()));
      long total = result.getTotalElements() == null ? 0 : result.getTotalElements();
      if (result.getContent().isEmpty() || (long) (page + 1) * PAGE_SIZE >= total) {
        return names;
      }
      page++;
    }
  }
}

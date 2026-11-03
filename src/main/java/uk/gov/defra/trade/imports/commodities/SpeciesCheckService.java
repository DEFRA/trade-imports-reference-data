package uk.gov.defra.trade.imports.commodities;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.defra.trade.imports.client.MdmProductPage;
import uk.gov.defra.trade.imports.client.MdmService;
import uk.gov.defra.trade.imports.client.MdmSpecies;

@Slf4j
@Service
@AllArgsConstructor
public class SpeciesCheckService {

  static final int PAGE_SIZE = 100;

  private final MdmService mdmService;

  public SpeciesCheckResponse check(SpeciesCheckRequest request) {
    Map<String, Set<String>> latinNamesByCode = request.lines().stream()
        .map(CommodityLine::code)
        .distinct()
        .collect(Collectors.toMap(Function.identity(), this::fetchLatinNamesOf));
    List<CommodityLine> unlisted = request.lines().stream()
        .filter(line -> !isListed(line, latinNamesByCode))
        .toList();
    if (!unlisted.isEmpty()) {
      log.warn("Species check found {} lines MDM does not list under their code: {}",
          unlisted.size(), unlisted);
    }
    return SpeciesCheckResponse.builder().unlisted(unlisted).build();
  }

  private static boolean isListed(CommodityLine line, Map<String, Set<String>> latinNamesByCode) {
    return latinNamesByCode.get(line.code()).contains(line.latinName());
  }

  private Set<String> fetchLatinNamesOf(String code) {
    Set<String> latinNames = new HashSet<>();
    int page = 0;
    while (true) {
      MdmProductPage<MdmSpecies> speciesPage =
          mdmService.getCommoditySpecies(code, page, PAGE_SIZE);
      speciesPage.content().forEach(species -> latinNames.add(species.latinName()));
      long total = speciesPage.totalElements();
      if (speciesPage.content().isEmpty() || (long) (page + 1) * PAGE_SIZE >= total) {
        return latinNames;
      }
      page++;
    }
  }
}

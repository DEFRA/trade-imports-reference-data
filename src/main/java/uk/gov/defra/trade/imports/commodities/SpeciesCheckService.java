package uk.gov.defra.trade.imports.commodities;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    Map<String, Set<String>> latinNamesByCode = new HashMap<>();
    List<CommodityLine> unlisted = request.lines().stream()
        .filter(line -> isUnlisted(line, latinNamesByCode))
        .toList();
    if (!unlisted.isEmpty()) {
      log.warn("Species check found {} lines MDM does not list under their code: {}",
          unlisted.size(), unlisted);
    }
    return SpeciesCheckResponse.builder().unlisted(unlisted).build();
  }

  private boolean isUnlisted(CommodityLine line, Map<String, Set<String>> latinNamesByCode) {
    return !latinNamesByCode.computeIfAbsent(line.code(), this::speciesOf)
        .contains(line.latinName());
  }

  private Set<String> speciesOf(String code) {
    Set<String> names = new HashSet<>();
    int page = 0;
    while (true) {
      MdmProductPage<MdmSpecies> speciesPage =
          mdmService.getCommoditySpecies(code, page, PAGE_SIZE);
      speciesPage.content().forEach(species -> names.add(species.latinName()));
      long total = speciesPage.totalElements();
      if (speciesPage.content().isEmpty() || (long) (page + 1) * PAGE_SIZE >= total) {
        return names;
      }
      page++;
    }
  }
}

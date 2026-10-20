package uk.gov.defra.trade.imports.commodities;

import static uk.gov.defra.trade.imports.client.MdmProductSearchResult.ResultType.COMMODITY;
import static uk.gov.defra.trade.imports.client.MdmProductSearchResult.ResultType.SPECIES;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.defra.trade.imports.client.MdmProductPage;
import uk.gov.defra.trade.imports.client.MdmProductSearchResult;
import uk.gov.defra.trade.imports.client.MdmService;

@Service
@AllArgsConstructor
public class CommoditySearchService {

  static final int MINIMUM_SEARCH_LENGTH = 2;
  static final int MAXIMUM_PAGE_SIZE = 100;
  private static final String LIVE_ANIMALS_CHAPTER = "01";

  private final MdmService mdmService;

  public CommoditySearchResponse search(String searchTerm, int page, int size) {
    int pageNumber = Math.max(page, 0);
    int pageSize = Math.clamp(size, 1, MAXIMUM_PAGE_SIZE);
    String trimmedSearchTerm = searchTerm == null ? "" : searchTerm.trim();
    if (trimmedSearchTerm.length() < MINIMUM_SEARCH_LENGTH) {
      return CommoditySearchResponse.builder()
          .total(0).page(pageNumber).size(pageSize).groups(List.of()).build();
    }

    MdmProductPage<MdmProductSearchResult> productPage =
        mdmService.searchProducts(trimmedSearchTerm, pageNumber, pageSize);
    Map<String, List<CommoditySpecies>> speciesByCode = new LinkedHashMap<>();
    SpeciesLimit speciesLimit = new SpeciesLimit(pageSize);
    productPage.content().forEach(row -> addRow(speciesByCode, row, speciesLimit));

    List<CommodityGroup> groups = speciesByCode.entrySet().stream()
        .filter(entry -> !entry.getValue().isEmpty())
        .map(entry -> CommodityGroup.builder().code(entry.getKey()).species(entry.getValue()).build())
        .toList();

    long total = productPage.totalElements() == null ? 0 : productPage.totalElements();
    return CommoditySearchResponse.builder()
        .total(total).page(pageNumber).size(pageSize).groups(groups).build();
  }

  private static void addRow(Map<String, List<CommoditySpecies>> speciesByCode,
      MdmProductSearchResult row, SpeciesLimit speciesLimit) {
    if (row.resultType() == SPECIES && isLiveAnimalCode(row.commodityCnCode())) {
      addSpecies(speciesByCode, row.commodityCnCode(), row.speciesName(), row.commonName(),
          speciesLimit);
    } else if (row.resultType() == COMMODITY && isLiveAnimalCode(row.code())) {
      speciesByCode.computeIfAbsent(row.code(), code -> new ArrayList<>());
      if (row.species() != null) {
        row.species().forEach(species -> addSpecies(speciesByCode, row.code(),
            species.speciesName(), species.commonName(), speciesLimit));
      }
    }
  }

  private static boolean isLiveAnimalCode(String code) {
    return code != null && code.startsWith(LIVE_ANIMALS_CHAPTER);
  }

  private static void addSpecies(Map<String, List<CommoditySpecies>> speciesByCode, String code,
      String latinName, String commonName, SpeciesLimit speciesLimit) {
    List<CommoditySpecies> species = speciesByCode.computeIfAbsent(code, key -> new ArrayList<>());
    if (speciesLimit.isReached() || latinName == null || isAlreadyListed(species, latinName)) {
      return;
    }
    species.add(CommoditySpecies.builder().latinName(latinName).commonName(commonName).build());
    speciesLimit.countOne();
  }

  private static boolean isAlreadyListed(List<CommoditySpecies> species, String latinName) {
    return species.stream().anyMatch(listed -> latinName.equals(listed.latinName()));
  }

  private static final class SpeciesLimit {

    private final int limit;
    private int count;

    private SpeciesLimit(int limit) {
      this.limit = limit;
    }

    private boolean isReached() {
      return count >= limit;
    }

    private void countOne() {
      count++;
    }
  }
}

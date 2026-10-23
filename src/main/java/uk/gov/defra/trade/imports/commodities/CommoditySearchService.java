package uk.gov.defra.trade.imports.commodities;

import static uk.gov.defra.trade.imports.client.MdmProductSearchResult.ResultType.COMMODITY;
import static uk.gov.defra.trade.imports.client.MdmProductSearchResult.ResultType.SPECIES;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.defra.trade.imports.client.MdmCommodity;
import uk.gov.defra.trade.imports.client.MdmProductPage;
import uk.gov.defra.trade.imports.client.MdmProductSearchResult;
import uk.gov.defra.trade.imports.client.MdmService;

@Slf4j
@Service
@AllArgsConstructor
public class CommoditySearchService {

  static final int MINIMUM_SEARCH_LENGTH = 2;
  static final int MAXIMUM_PAGE_SIZE = 100;

  private final MdmService mdmService;

  public CommoditySearchResponse search(String searchTerm, int page, int size) {
    String trimmedSearchTerm = searchTerm == null ? "" : searchTerm.trim();
    if (trimmedSearchTerm.length() < MINIMUM_SEARCH_LENGTH) {
      log.info("Commodity search term '{}' is shorter than {} characters, so MDM is not asked",
          trimmedSearchTerm, MINIMUM_SEARCH_LENGTH);
      return CommoditySearchResponse.builder()
          .total(0).page(page).size(size).groups(List.of()).build();
    }

    MdmProductPage<MdmProductSearchResult> productPage =
        mdmService.searchProducts(trimmedSearchTerm, page, size);
    Set<String> germinalCodes = germinalCodesAmong(productPage.content());
    if (!germinalCodes.isEmpty()) {
      log.info("Commodity search for '{}' leaves out germinal codes {}", trimmedSearchTerm,
          germinalCodes);
    }
    Map<String, List<CommoditySpecies>> speciesByCode = new LinkedHashMap<>();
    SpeciesLimit speciesLimit = new SpeciesLimit(size);
    productPage.content().stream()
        .filter(row -> !germinalCodes.contains(codeOf(row)))
        .forEach(row -> addRow(speciesByCode, row, speciesLimit));

    List<CommodityGroup> groups = speciesByCode.entrySet().stream()
        .filter(entry -> !entry.getValue().isEmpty())
        .map(entry -> CommodityGroup.builder().code(entry.getKey()).species(entry.getValue()).build())
        .toList();

    long total = productPage.totalElements();
    return CommoditySearchResponse.builder()
        .total(total).page(page).size(size).groups(groups).build();
  }

  private Set<String> germinalCodesAmong(List<MdmProductSearchResult> rows) {
    Set<String> codes = rows.stream()
        .map(CommoditySearchService::codeOf)
        .filter(Objects::nonNull)
        .collect(Collectors.toCollection(LinkedHashSet::new));
    return mdmService.getCommodities(codes).stream()
        .filter(CommoditySearchService::isGerminal)
        .map(MdmCommodity::code)
        .collect(Collectors.toSet());
  }

  private static boolean isGerminal(MdmCommodity commodity) {
    return Boolean.TRUE.equals(commodity.isGerminal());
  }

  private static String codeOf(MdmProductSearchResult row) {
    if (row.resultType() == SPECIES) {
      return row.commodityCnCode();
    }
    if (row.resultType() == COMMODITY) {
      return row.code();
    }
    return null;
  }

  private static void addRow(Map<String, List<CommoditySpecies>> speciesByCode,
      MdmProductSearchResult row, SpeciesLimit speciesLimit) {
    String code = codeOf(row);
    if (code == null) {
      return;
    }
    if (row.resultType() == SPECIES) {
      addSpecies(speciesByCode, code, row.latinName(), row.commonName(), speciesLimit);
      return;
    }
    speciesByCode.computeIfAbsent(code, key -> new ArrayList<>());
    if (row.species() != null) {
      row.species().forEach(species -> addSpecies(speciesByCode, code,
          species.latinName(), species.commonName(), speciesLimit));
    }
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

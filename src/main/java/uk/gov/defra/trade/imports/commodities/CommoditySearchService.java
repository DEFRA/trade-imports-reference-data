package uk.gov.defra.trade.imports.commodities;

import static uk.gov.defra.trade.imports.client.MdmProductSearchResult.ResultType.COMMODITY;
import static uk.gov.defra.trade.imports.client.MdmProductSearchResult.ResultType.SPECIES;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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
    List<SpeciesMatch> matches = speciesMatchesIn(productPage.content(), germinalCodes);

    return CommoditySearchResponse.builder()
        .total(matches.size())
        .page(page)
        .size(size)
        .groups(groupsOf(matches.stream().limit(size).toList()))
        .build();
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

  private static List<SpeciesMatch> speciesMatchesIn(List<MdmProductSearchResult> rows,
      Set<String> germinalCodes) {
    Map<List<String>, SpeciesMatch> matchesByCodeAndLatinName = new LinkedHashMap<>();
    rows.stream()
        .filter(row -> !germinalCodes.contains(codeOf(row)))
        .flatMap(CommoditySearchService::speciesMatchesOf)
        .filter(match -> match.latinName() != null)
        .forEach(match -> matchesByCodeAndLatinName.putIfAbsent(
            List.of(match.code(), match.latinName()), match));
    return List.copyOf(matchesByCodeAndLatinName.values());
  }

  private static Stream<SpeciesMatch> speciesMatchesOf(MdmProductSearchResult row) {
    String code = codeOf(row);
    if (code == null) {
      return Stream.empty();
    }
    if (row.resultType() == SPECIES) {
      return Stream.of(new SpeciesMatch(code, row.latinName(), row.commonName()));
    }
    if (row.species() == null) {
      return Stream.empty();
    }
    return row.species().stream()
        .map(species -> new SpeciesMatch(code, species.latinName(), species.commonName()));
  }

  private static List<CommodityGroup> groupsOf(List<SpeciesMatch> matches) {
    return matches.stream()
        .collect(Collectors.groupingBy(SpeciesMatch::code, LinkedHashMap::new,
            Collectors.mapping(SpeciesMatch::species, Collectors.toList())))
        .entrySet().stream()
        .map(entry -> CommodityGroup.builder().code(entry.getKey()).species(entry.getValue()).build())
        .toList();
  }

  private record SpeciesMatch(String code, String latinName, String commonName) {

    private CommoditySpecies species() {
      return CommoditySpecies.builder().latinName(latinName).commonName(commonName).build();
    }
  }
}

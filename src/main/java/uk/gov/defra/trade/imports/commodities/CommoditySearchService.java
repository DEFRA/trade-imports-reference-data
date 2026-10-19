package uk.gov.defra.trade.imports.commodities;

import static uk.gov.defra.trade.imports.client.MdmProductSearchResult.COMMODITY;
import static uk.gov.defra.trade.imports.client.MdmProductSearchResult.SPECIES;

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

  // MDM answers 400 to anything shorter.
  static final int MINIMUM_SEARCH_LENGTH = 2;
  static final int SEARCH_SIZE = 100;

  private final MdmService mdmService;

  /** Groups MDM's matches by commodity code, in the order MDM returns them. */
  public CommoditySearchResponse search(String term) {
    String query = term == null ? "" : term.trim();
    if (query.length() < MINIMUM_SEARCH_LENGTH) {
      return CommoditySearchResponse.builder().total(0).groups(List.of()).build();
    }

    MdmProductPage<MdmProductSearchResult> page = mdmService.searchProducts(query, 0, SEARCH_SIZE);
    Map<String, List<CommoditySpecies>> speciesByCode = new LinkedHashMap<>();
    page.getContent().forEach(row -> addRow(speciesByCode, row));

    List<CommodityGroup> groups = speciesByCode.entrySet().stream()
        .map(entry -> CommodityGroup.builder().code(entry.getKey()).species(entry.getValue()).build())
        .toList();

    long total = page.getTotalElements() == null ? 0 : page.getTotalElements();
    return CommoditySearchResponse.builder().total(total).groups(groups).build();
  }

  private static void addRow(
      Map<String, List<CommoditySpecies>> speciesByCode, MdmProductSearchResult row) {
    if (SPECIES.equals(row.getResultType()) && row.getCommodityCnCode() != null) {
      addSpecies(speciesByCode, row.getCommodityCnCode(), row.getSpeciesName(), row.getCommonName());
    } else if (COMMODITY.equals(row.getResultType()) && row.getCode() != null) {
      speciesByCode.computeIfAbsent(row.getCode(), code -> new ArrayList<>());
      if (row.getScientificNames() != null) {
        row.getScientificNames().forEach(species -> addSpecies(
            speciesByCode, row.getCode(), species.getSpeciesName(), species.getCommonName()));
      }
    }
  }

  // A code search returns the commodity with its species, and a name search can return the same
  // species again as a row of its own, so a species is listed once per code.
  private static void addSpecies(Map<String, List<CommoditySpecies>> speciesByCode, String code,
      String latinName, String commonName) {
    List<CommoditySpecies> species = speciesByCode.computeIfAbsent(code, key -> new ArrayList<>());
    if (latinName == null
        || species.stream().anyMatch(existing -> latinName.equals(existing.getLatinName()))) {
      return;
    }
    species.add(CommoditySpecies.builder().latinName(latinName).commonName(commonName).build());
  }
}

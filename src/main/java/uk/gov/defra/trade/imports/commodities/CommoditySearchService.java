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
  // MDM cuts any larger page to 100.
  static final int MAXIMUM_SIZE = 100;

  private final MdmService mdmService;

  /**
   * Groups one page of MDM's matches by commodity code, in the order MDM returns them, listing at
   * most {@code size} species.
   */
  public CommoditySearchResponse search(String term, int page, int size) {
    int pageNumber = Math.max(page, 0);
    int pageSize = Math.clamp(size, 1, MAXIMUM_SIZE);
    String query = term == null ? "" : term.trim();
    if (query.length() < MINIMUM_SEARCH_LENGTH) {
      return CommoditySearchResponse.builder()
          .total(0).page(pageNumber).size(pageSize).groups(List.of()).build();
    }

    MdmProductPage<MdmProductSearchResult> results =
        mdmService.searchProducts(query, pageNumber, pageSize);
    Map<String, List<CommoditySpecies>> speciesByCode = new LinkedHashMap<>();
    SpeciesCount listed = new SpeciesCount(pageSize);
    results.getContent().forEach(row -> addRow(speciesByCode, row, listed));

    List<CommodityGroup> groups = speciesByCode.entrySet().stream()
        .filter(entry -> !entry.getValue().isEmpty())
        .map(entry -> CommodityGroup.builder().code(entry.getKey()).species(entry.getValue()).build())
        .toList();

    long total = results.getTotalElements() == null ? 0 : results.getTotalElements();
    return CommoditySearchResponse.builder()
        .total(total).page(pageNumber).size(pageSize).groups(groups).build();
  }

  private static void addRow(Map<String, List<CommoditySpecies>> speciesByCode,
      MdmProductSearchResult row, SpeciesCount listed) {
    if (SPECIES.equals(row.getResultType()) && row.getCommodityCnCode() != null) {
      addSpecies(speciesByCode, row.getCommodityCnCode(), row.getSpeciesName(), row.getCommonName(),
          listed);
    } else if (COMMODITY.equals(row.getResultType()) && row.getCode() != null) {
      speciesByCode.computeIfAbsent(row.getCode(), code -> new ArrayList<>());
      if (row.getScientificNames() != null) {
        row.getScientificNames().forEach(species -> addSpecies(speciesByCode, row.getCode(),
            species.getSpeciesName(), species.getCommonName(), listed));
      }
    }
  }

  // A code search returns the commodity with all its species in one row, so a page of rows can
  // hold more species than the page size; listing stops once the page size is reached. A name
  // search can return the same species again as a row of its own, so a species is listed once per
  // code.
  private static void addSpecies(Map<String, List<CommoditySpecies>> speciesByCode, String code,
      String latinName, String commonName, SpeciesCount listed) {
    List<CommoditySpecies> species = speciesByCode.computeIfAbsent(code, key -> new ArrayList<>());
    if (listed.isFull() || latinName == null
        || species.stream().anyMatch(existing -> latinName.equals(existing.getLatinName()))) {
      return;
    }
    species.add(CommoditySpecies.builder().latinName(latinName).commonName(commonName).build());
    listed.add();
  }

  private static final class SpeciesCount {

    private final int limit;
    private int count;

    private SpeciesCount(int limit) {
      this.limit = limit;
    }

    private boolean isFull() {
      return count >= limit;
    }

    private void add() {
      count++;
    }
  }
}

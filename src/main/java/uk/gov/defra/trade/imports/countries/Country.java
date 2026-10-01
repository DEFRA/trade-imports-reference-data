package uk.gov.defra.trade.imports.countries;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.defra.trade.imports.client.MdmCountry;
import uk.gov.defra.trade.imports.client.MdmSubDivision;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Country {

  private String code;
  private String name;
  private List<SubDivision> subDivisions;

  public Country(MdmCountry mdmCountry) {
    this.code = mdmCountry.getEffectiveAlpha2();
    this.name = mdmCountry.getEffectiveAlias();
    this.subDivisions = mapSubDivisions(mdmCountry.getSubDivisions());
  }

  private static List<SubDivision> mapSubDivisions(List<MdmSubDivision> mdmSubDivisions) {
    if (mdmSubDivisions == null || mdmSubDivisions.isEmpty()) {
      return List.of();
    }
    return mdmSubDivisions.stream()
        .filter(subDivision -> subDivision.getCode() != null
            && subDivision.getCode().getValue() != null)
        .map(subDivision -> SubDivision.builder()
            .code(subDivision.getCode().getValue())
            .name(subDivision.getName())
            .build())
        .toList();
  }
}

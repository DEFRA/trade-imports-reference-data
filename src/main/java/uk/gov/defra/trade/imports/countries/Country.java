package uk.gov.defra.trade.imports.countries;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.defra.trade.imports.client.MdmCountry;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Country {

  private String code;
  private String name;

  public Country(MdmCountry mdmCountry) {
    this.code = mdmCountry.getEffectiveAlpha2();
    this.name = mdmCountry.getEffectiveAlias();
  }
}

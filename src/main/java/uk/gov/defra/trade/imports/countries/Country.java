package uk.gov.defra.trade.imports.countries;

import java.util.List;
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
  private List<String> classifiers;
  private List<String> internalClassifiers;

  public Country(MdmCountry mdmCountry) {
    this.code = mdmCountry.getAlpha2();
    this.name = mdmCountry.getName();
    this.classifiers = mdmCountry.getClassifiers();
    this.internalClassifiers = mdmCountry.getInternalClassifiers();
  }
}

package uk.gov.defra.trade.imports.countries;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MdmCountry {

  private String id;
  private Integer isoNumeric;
  private String alpha2;
  private String alpha3;
  private String longName;
  private String name;
  private Boolean independent;
  private String statusName;
  private String system;
  private String systemAlias;
  private String systemAlpha2;
  private String systemAlpha3;
  private String systemLongName;
  private String effectiveAlias;
  private String effectiveAlpha2;
  private String effectiveAlpha3;
  private String effectiveLongName;
  private List<String> classifiers;
  private List<String> internalClassifiers;
}


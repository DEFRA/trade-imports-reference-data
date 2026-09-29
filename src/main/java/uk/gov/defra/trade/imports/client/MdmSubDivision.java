package uk.gov.defra.trade.imports.client;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MdmSubDivision {

  private MdmCodedValue code;
  private String name;
  private List<String> blocs;
}

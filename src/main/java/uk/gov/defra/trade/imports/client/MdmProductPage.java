package uk.gov.defra.trade.imports.client;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** One page of an MDM product API list. Pages count from 0; species pages carry no totalPages. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MdmProductPage<T> {

  private List<T> content;
  private Integer page;
  private Integer size;
  private Long totalElements;
  private Integer totalPages;
}

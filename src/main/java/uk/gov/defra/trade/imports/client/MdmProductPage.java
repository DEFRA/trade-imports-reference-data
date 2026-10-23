package uk.gov.defra.trade.imports.client;

import java.util.List;
import lombok.Builder;

@Builder
public record MdmProductPage<T>(List<T> content, Long totalElements) {
}

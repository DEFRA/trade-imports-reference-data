package uk.gov.defra.trade.imports.client;

import lombok.Builder;

@Builder
public record MdmCommodity(String code, Boolean isGerminal) {
}

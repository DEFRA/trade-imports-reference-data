package uk.gov.defra.trade.imports.client;

import lombok.Builder;

@Builder
public record MdmSpecies(String speciesName, String commonName, String commodityCnCode) {
}

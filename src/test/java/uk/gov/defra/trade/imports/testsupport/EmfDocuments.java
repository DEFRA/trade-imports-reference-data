package uk.gov.defra.trade.imports.testsupport;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;

public final class EmfDocuments {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private EmfDocuments() {}

  public static JsonNode parse(String document) {
    try {
      return OBJECT_MAPPER.readTree(document);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException(e);
    }
  }

  public static boolean isEmfDocument(JsonNode node) {
    return node.isObject() && node.has("_aws");
  }

  public static List<JsonNode> documentsFor(List<JsonNode> documents, String metricName) {
    return documents.stream().filter(document -> document.has(metricName)).toList();
  }

  public static String namespaceOf(JsonNode document) {
    return directiveOf(document).path("Namespace").asText();
  }

  public static String unitOf(JsonNode document) {
    return directiveOf(document).path("Metrics").get(0).path("Unit").asText();
  }

  public static List<List<String>> dimensionSetsOf(JsonNode document) {
    List<List<String>> dimensionSets = new ArrayList<>();
    directiveOf(document)
        .path("Dimensions")
        .forEach(
            dimensionSet -> {
              List<String> keys = new ArrayList<>();
              dimensionSet.forEach(key -> keys.add(key.asText()));
              dimensionSets.add(keys);
            });
    return dimensionSets;
  }

  public static List<Double> valuesOf(JsonNode document, String metricName) {
    JsonNode values = document.path(metricName);
    if (!values.isArray()) {
      return List.of(values.asDouble());
    }
    List<Double> doubles = new ArrayList<>();
    values.forEach(value -> doubles.add(value.asDouble()));
    return doubles;
  }

  private static JsonNode directiveOf(JsonNode document) {
    return document.path("_aws").path("CloudWatchMetrics").get(0);
  }
}

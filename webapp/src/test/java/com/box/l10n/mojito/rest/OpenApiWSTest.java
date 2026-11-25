package com.box.l10n.mojito.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

/**
 * Verifies OpenAPI documents {@code ZonedDateTime} as epoch milliseconds ({@code integer}/{@code
 * int64}), matching Mojito's JSON wire format.
 *
 * @author ehoogerbeets
 */
@TestPropertySource(properties = "springdoc.api-docs.enabled=true")
public class OpenApiWSTest extends WSTestBase {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  public void zonedDateTimePropertiesAreDocumentedAsEpochMillis() throws Exception {
    JsonNode root = fetchOpenApiSpec();

    JsonNode schemas = root.path("components").path("schemas");
    assertFalse("expected components.schemas in OpenAPI spec", schemas.isMissingNode());

    int createdDateCount = 0;
    for (Map.Entry<String, JsonNode> entry : schemas.properties()) {
      JsonNode createdDate = entry.getValue().path("properties").path("createdDate");
      if (createdDate.isMissingNode()) {
        continue;
      }
      createdDateCount++;
      assertEpochMillisSchema("schema " + entry.getKey() + ".createdDate", createdDate);
    }
    assertTrue("expected at least one createdDate property in schemas", createdDateCount > 0);
  }

  @Test
  public void zonedDateTimeQueryParametersAreDocumentedAsEpochMillis() throws Exception {
    JsonNode root = fetchOpenApiSpec();

    int createdBeforeCount = 0;
    for (JsonNode pathItem : root.path("paths")) {
      for (JsonNode operation : pathItem) {
        JsonNode parameters = operation.path("parameters");
        if (!parameters.isArray()) {
          continue;
        }
        for (JsonNode parameter : parameters) {
          if (!"createdBefore".equals(parameter.path("name").asText())) {
            continue;
          }
          createdBeforeCount++;
          assertEpochMillisSchema("parameter createdBefore", parameter.path("schema"));
        }
      }
    }
    assertTrue("expected at least one createdBefore query parameter", createdBeforeCount > 0);
  }

  private JsonNode fetchOpenApiSpec() throws Exception {
    ResponseEntity<String> response =
        authenticatedRestTemplate
            .getRestTemplate()
            .getForEntity(authenticatedRestTemplate.getURIForResource("/api-docs"), String.class);
    assertEquals(200, response.getStatusCodeValue());
    return objectMapper.readTree(response.getBody());
  }

  private static void assertEpochMillisSchema(String label, JsonNode schema) {
    assertEquals(label + " type", "integer", schemaType(schema));
    assertEquals(label + " format", "int64", schema.path("format").asText());
  }

  private static String schemaType(JsonNode schema) {
    if (schema.hasNonNull("type")) {
      return schema.get("type").asText();
    }
    JsonNode types = schema.path("types");
    if (types.isArray() && !types.isEmpty()) {
      return types.get(0).asText();
    }
    return null;
  }
}

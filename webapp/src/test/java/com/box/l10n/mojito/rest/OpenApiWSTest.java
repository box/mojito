package com.box.l10n.mojito.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.springframework.test.context.TestPropertySource;

/**
 * Smoke test that OpenAPI documents Mojito's epoch-millis datetime wire format for every temporal
 * type used on the REST API ({@code ZonedDateTime}, {@code Instant}, {@code java.util.Date}).
 *
 * <p>Requires springdoc enabled for this test only (disabled by default in application config).
 */
@TestPropertySource(properties = "springdoc.api-docs.enabled=true")
public class OpenApiWSTest extends WSTestBase {

  @Test
  public void apiTemporalFieldsAreDocumentedAsEpochMillisInt64() throws Exception {
    String spec =
        authenticatedRestTemplate
            .getRestTemplate()
            .getForObject(authenticatedRestTemplate.getURIForResource("/api-docs"), String.class);

    assertNotNull(spec);

    ObjectMapper objectMapper = new ObjectMapper();
    JsonNode root = objectMapper.readTree(spec);

    // ZonedDateTime — AuditableEntity field on Repository responses
    assertEpochMillisInt64(
        root.at("/components/schemas/Repository/properties/createdDate"),
        "Repository.createdDate");

    // Instant — DbMonitoringSnapshot from /api/monitoring/db
    assertEpochMillisInt64(
        root.at("/components/schemas/DbMonitoringSnapshot/properties/timestamp"),
        "DbMonitoringSnapshot.timestamp");

    // java.util.Date — ExportDropConfig from drop export
    assertEpochMillisInt64(
        root.at("/components/schemas/ExportDropConfig/properties/uploadTime"),
        "ExportDropConfig.uploadTime");
  }

  private static void assertEpochMillisInt64(JsonNode propertySchema, String fieldName) {
    assertFalse("expected schema for " + fieldName, propertySchema.isMissingNode());
    assertEquals(fieldName + " type", "integer", propertySchema.path("type").asText());
    assertEquals(fieldName + " format", "int64", propertySchema.path("format").asText());
  }
}

package com.box.l10n.mojito.rest.entity;

import static org.junit.Assert.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

/**
 * The restclient enum is deserialized from server JSON by name. A missing constant (historically
 * {@code FLUENT}) makes {@code repo-view} / {@code repo-update} fail for that checker.
 */
public class IntegrityCheckerTypeTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  public void everyTypeDeserializesFromItsJsonName() throws Exception {
    for (IntegrityCheckerType type : IntegrityCheckerType.values()) {
      IntegrityCheckerType parsed =
          objectMapper.readValue("\"" + type.name() + "\"", IntegrityCheckerType.class);
      assertEquals(type.name(), type, parsed);
    }
  }

  @Test
  public void everyTypeDeserializesInACheckerPayload() throws Exception {
    for (IntegrityCheckerType type : IntegrityCheckerType.values()) {
      String json = "{\"assetExtension\":\"ftl\",\"integrityCheckerType\":\"" + type.name() + "\"}";
      IntegrityChecker checker = objectMapper.readValue(json, IntegrityChecker.class);
      assertEquals(type.name(), "ftl", checker.getAssetExtension());
      assertEquals(type.name(), type, checker.getIntegrityCheckerType());
    }
  }
}

package com.box.l10n.mojito.cli.command;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.box.l10n.mojito.json.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class JsonOutputTest {

  PrintStream originalOut;
  ByteArrayOutputStream capturedOut;
  JsonOutput jsonOutput;

  @Before
  public void setUp() {
    originalOut = System.out;
    capturedOut = new ByteArrayOutputStream();
    System.setOut(new PrintStream(capturedOut, true, StandardCharsets.UTF_8));

    jsonOutput = new JsonOutput();
    jsonOutput.objectMapper = new ObjectMapper();
  }

  @After
  public void tearDown() {
    System.setOut(originalOut);
  }

  @Test
  public void writeSuccessEmitsEnvelopeWithData() throws Exception {
    Map<String, Object> data = new LinkedHashMap<>();
    data.put("repo", "MyNewRepo");
    data.put("id", 42);
    data.put("locales", List.of("bn-IN", "hi-IN"));

    jsonOutput.writeSuccess("repo-create", data);

    JsonNode root = new ObjectMapper().readTree(capturedOut.toString(StandardCharsets.UTF_8));
    assertTrue(root.get("successful").asBoolean());
    assertEquals("repo-create", root.get("command").asText());
    assertEquals("MyNewRepo", root.get("data").get("repo").asText());
    assertEquals(42, root.get("data").get("id").asInt());
    assertEquals(2, root.get("data").get("locales").size());
    assertFalse(root.has("error"));
  }

  @Test
  public void writeFailureEmitsEnvelopeWithError() throws Exception {
    jsonOutput.writeFailure("repo-create", "There is a conflict");

    JsonNode root = new ObjectMapper().readTree(capturedOut.toString(StandardCharsets.UTF_8));
    assertFalse(root.get("successful").asBoolean());
    assertEquals("repo-create", root.get("command").asText());
    assertEquals("There is a conflict", root.get("error").asText());
    assertFalse(root.has("data"));
  }
}

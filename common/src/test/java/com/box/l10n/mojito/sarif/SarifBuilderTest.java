package com.box.l10n.mojito.sarif;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import com.box.l10n.mojito.json.ObjectMapper;
import com.box.l10n.mojito.sarif.builder.SarifBuilder;
import com.box.l10n.mojito.sarif.model.Location;
import com.box.l10n.mojito.sarif.model.Result;
import com.box.l10n.mojito.sarif.model.ResultLevel;
import com.box.l10n.mojito.sarif.model.Run;
import com.box.l10n.mojito.sarif.model.Sarif;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.List;
import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.junit.jupiter.api.Test;

class SarifBuilderTest {

  private final Schema schemaValidator;

  SarifBuilderTest() throws FileNotFoundException {
    InputStream schemaStream =
        SarifBuilderTest.class.getResourceAsStream("/sarif-schema-2.1.0.json");
    if (schemaStream == null) {
      throw new FileNotFoundException("Schema file not found");
    }
    JSONObject rawSchema = new JSONObject(new JSONTokener(schemaStream));
    schemaValidator = SchemaLoader.load(rawSchema);
  }

  @Test
  void testBuildSimpleRun() {
    Sarif sarif = new SarifBuilder().addRun("MyTool", "https://tool.example").build();

    assertNotNull(sarif);
    assertEquals(1, sarif.runs.size());
    Run run = sarif.runs.get(0);
    assertNotNull(run.getTool());
    assertEquals("MyTool", run.getTool().getDriver().getName());
    assertEquals("https://tool.example", run.getTool().getDriver().getInformationUri());
    assertTrue(run.getResults().isEmpty());
    assertDoesNotThrow(() -> validateSchema(sarif));
  }

  @Test
  void testAddResultWithLocations_SingleLocation() {
    Sarif sarif =
        new SarifBuilder()
            .addRun("MyTool", "https://tool.example")
            .addResultWithLocations(
                "RULE_A",
                ResultLevel.ERROR,
                "Critical error detected",
                List.of(new Location("src/Main.java", 101)))
            .build();

    Run run = sarif.runs.get(0);
    assertEquals(1, run.getResults().size());
    Result result = run.getResults().get(0);

    assertEquals("RULE_A", result.getRuleId());
    assertEquals(ResultLevel.ERROR, result.getLevel());
    assertEquals("Critical error detected", result.getMessage().getText());

    assertEquals(1, result.getLocations().size());
    Location location = result.getLocations().get(0);
    assertEquals("src/Main.java", location.getPhysicalLocation().getArtifactLocation().getUri());
    assertEquals(101, location.getPhysicalLocation().getRegion().getStartLine());
    assertNull(location.getPhysicalLocation().getRegion().getEndLine());
    assertDoesNotThrow(() -> validateSchema(sarif));
  }

  @Test
  void testAddResultWithLocation_WithMultipleLocations() {
    Sarif sarif =
        new SarifBuilder()
            .addRun("MyTool", "https://tool.example")
            .addResultWithLocations(
                "RULE_B",
                ResultLevel.WARNING,
                "Check multi-line block",
                List.of(
                    new Location("src/Main.java", 120, 125),
                    new Location("src/Util.java", 10),
                    new Location("src/Text/Util.java", 1)))
            .build();

    Run run = sarif.getRuns().get(0);
    assertEquals(1, run.getResults().size());
    Result result = run.getResults().get(0);

    assertEquals("RULE_B", result.getRuleId());
    assertEquals(ResultLevel.WARNING, result.getLevel());
    assertEquals("Check multi-line block", result.getMessage().getText());

    assertEquals(3, result.getLocations().size());
    Location location = result.getLocations().get(0);
    assertEquals("src/Main.java", location.getPhysicalLocation().getArtifactLocation().getUri());
    assertEquals(120, location.getPhysicalLocation().getRegion().getStartLine());
    assertEquals(125, location.getPhysicalLocation().getRegion().getEndLine());
    location = result.getLocations().get(1);
    assertEquals("src/Util.java", location.getPhysicalLocation().getArtifactLocation().getUri());
    assertEquals(10, location.getPhysicalLocation().getRegion().getStartLine());
    assertNull(location.getPhysicalLocation().getRegion().getEndLine());
    location = result.getLocations().get(2);
    assertEquals(
        "src/Text/Util.java", location.getPhysicalLocation().getArtifactLocation().getUri());
    assertEquals(1, location.getPhysicalLocation().getRegion().getStartLine());
    assertNull(location.getPhysicalLocation().getRegion().getEndLine());
    assertDoesNotThrow(() -> validateSchema(sarif));
  }

  @Test
  void testAddResultWithoutLocation() {
    SarifBuilder builder = new SarifBuilder();
    builder
        .addRun("tool", "infoUri")
        .addResultWithoutLocation("RULE_X", ResultLevel.NOTE, "Note: <something_important>");
    Sarif sarif = builder.build();
    Run run = sarif.runs.get(0);
    assertEquals(1, run.getResults().size());
    Result result = run.getResults().get(0);
    assertEquals("RULE_X", result.getRuleId());
    assertEquals(ResultLevel.NOTE, result.getLevel());
    assertEquals("Note: <something_important>", result.getMessage().getText());
    assertTrue(result.getLocations() == null || result.getLocations().isEmpty());
    assertDoesNotThrow(() -> validateSchema(sarif));
  }

  @Test
  void testCombinedUsage() {
    SarifBuilder builder = new SarifBuilder();
    builder
        .addRun("tool", "uri")
        .addResultWithoutLocation("RULE_1", ResultLevel.NOTE, "Note 1")
        .addResultWithLocations(
            "RULE_2", ResultLevel.ERROR, "Error 2", List.of(new Location("src/Main.java", 101)));
    Sarif sarif = builder.build();
    Run run = sarif.runs.get(0);
    assertEquals(2, run.getResults().size());
    assertEquals("RULE_1", run.getResults().get(0).getRuleId());
    assertEquals("RULE_2", run.getResults().get(1).getRuleId());
    assertTrue(
        run.getResults().get(0).getLocations() == null
            || run.getResults().get(0).getLocations().isEmpty());
  }

  private void validateSchema(Sarif sarif) throws ValidationException, FileNotFoundException {
    ObjectMapper objectMapper = new ObjectMapper();
    String jsonStr = objectMapper.writeValueAsStringUnchecked(sarif);
    JSONObject jsonData = new JSONObject(new JSONTokener(jsonStr));
    schemaValidator.validate(jsonData);
  }
}

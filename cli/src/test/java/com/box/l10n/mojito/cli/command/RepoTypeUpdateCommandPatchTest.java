package com.box.l10n.mojito.cli.command;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.box.l10n.mojito.rest.entity.RepoType;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

public class RepoTypeUpdateCommandPatchTest {

  private static final ObjectMapper NON_NULL =
      new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL);

  @Test
  public void repoTypePatchOmitsPromptAndCheckersWhenNull() throws Exception {
    RepoType patch = RepoTypeUpdateCommand.repoTypePatch("React", "new desc", null);
    assertEquals("React", patch.getName());
    assertEquals("new desc", patch.getDescription());
    assertNull(patch.getAiPrompt());
    assertNull(
        "null checkers are omitted on the wire (leave unchanged); empty set would clear them",
        patch.getIntegrityCheckers());

    String json = NON_NULL.writeValueAsString(patch);
    assertFalse(json.contains("aiPrompt"));
    assertFalse(json.contains("integrityCheckers"));
  }

  @Test
  public void repoTypePatchSetsEmptyAiPromptToClear() throws Exception {
    RepoType patch = RepoTypeUpdateCommand.repoTypePatch(null, null, "");
    assertNull(patch.getName());
    assertNull(patch.getDescription());
    assertEquals("", patch.getAiPrompt());
    assertNull(patch.getIntegrityCheckers());

    String json = NON_NULL.writeValueAsString(patch);
    assertTrue(json.contains("\"aiPrompt\":\"\""));
    assertFalse(json.contains("integrityCheckers"));
  }

  @Test
  public void repoTypePatchSetsAiPromptWhenPassed() throws Exception {
    RepoType patch = RepoTypeUpdateCommand.repoTypePatch(null, null, "Preserve {placeholders}.");
    assertNull(patch.getName());
    assertNull(patch.getDescription());
    assertEquals("Preserve {placeholders}.", patch.getAiPrompt());
    assertNull(patch.getIntegrityCheckers());

    String json = NON_NULL.writeValueAsString(patch);
    assertTrue(json.contains("\"aiPrompt\":\"Preserve {placeholders}.\""));
    assertFalse(json.contains("\"name\""));
  }
}

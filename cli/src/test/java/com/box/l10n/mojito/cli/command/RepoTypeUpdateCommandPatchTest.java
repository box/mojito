package com.box.l10n.mojito.cli.command;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.box.l10n.mojito.rest.entity.RepoType;
import org.junit.Test;

public class RepoTypeUpdateCommandPatchTest {

  @Test
  public void nameAndDescriptionPatchLeavesPromptAndCheckersNull() {
    RepoType patch = RepoTypeUpdateCommand.nameAndDescriptionPatch("React", "new desc");
    assertEquals("React", patch.getName());
    assertEquals("new desc", patch.getDescription());
    assertNull(patch.getAiPrompt());
    assertNull(
        "null checkers are omitted on the wire (leave unchanged); empty set would clear them",
        patch.getIntegrityCheckers());
  }
}

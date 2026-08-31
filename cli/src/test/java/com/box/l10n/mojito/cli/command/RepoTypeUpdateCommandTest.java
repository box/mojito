package com.box.l10n.mojito.cli.command;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.box.l10n.mojito.cli.CLITestBase;
import com.box.l10n.mojito.cli.command.param.Param;
import com.box.l10n.mojito.entity.RepoType;
import com.box.l10n.mojito.service.repotype.RepoTypeRepository;
import com.box.l10n.mojito.service.repotype.RepoTypeService;
import java.util.regex.Pattern;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class RepoTypeUpdateCommandTest extends CLITestBase {

  static Logger logger = LoggerFactory.getLogger(RepoTypeUpdateCommandTest.class);

  @Autowired RepoTypeService repoTypeService;

  @Autowired RepoTypeRepository repoTypeRepository;

  @Test
  public void testUpdateName() throws Exception {
    String name = testIdWatcher.getEntityName("Android");
    String newName = name + "_updated";
    String description = "Android strings.xml apps";

    RepoType created = repoTypeService.createRepoType(name, description, null, null);

    getL10nJCommander()
        .run(
            "repo-type-update",
            Param.REPO_TYPE_NAME_SHORT,
            name,
            Param.REPO_TYPE_NEW_NAME_SHORT,
            newName);

    assertUpdatedIdLine(created.getId());

    assertNull("Should not find repo type by old name", repoTypeRepository.findByName(name));

    RepoType updated = repoTypeRepository.findByName(newName);
    assertNotNull("Should find repo type by the new name", updated);
    assertEquals(created.getId(), updated.getId());
    assertEquals(description, updated.getDescription());
  }

  @Test
  public void testUpdateDescription() throws Exception {
    String name = testIdWatcher.getEntityName("iOS");
    String oldDescription = "old description";
    String newDescription = "Swift / Localizable.strings apps";

    RepoType created = repoTypeService.createRepoType(name, oldDescription, null, null);

    getL10nJCommander()
        .run(
            "repo-type-update",
            Param.REPO_TYPE_NAME_SHORT,
            name,
            Param.REPO_TYPE_DESCRIPTION_SHORT,
            newDescription);

    assertUpdatedIdLine(created.getId());

    RepoType updated = repoTypeRepository.findByName(name);
    assertNotNull(updated);
    assertEquals(created.getId(), updated.getId());
    assertEquals(newDescription, updated.getDescription());
  }

  @Test
  public void testUpdateDescriptionDoesNotClearPrompt() throws Exception {
    String name = testIdWatcher.getEntityName("React");
    String prompt = "You are a React i18n expert";

    RepoType created = repoTypeService.createRepoType(name, "old", prompt, null);

    getL10nJCommander()
        .run(
            "repo-type-update",
            Param.REPO_TYPE_NAME_SHORT,
            name,
            Param.REPO_TYPE_DESCRIPTION_SHORT,
            "new desc");

    assertUpdatedIdLine(created.getId());

    RepoType updated = repoTypeService.getRepoTypeById(created.getId());
    assertEquals("new desc", updated.getDescription());
    assertEquals(prompt, updated.getAiPrompt());
  }

  @Test
  public void testUpdateNameDoesNotClearPrompt() throws Exception {
    String name = testIdWatcher.getEntityName("Android");
    String newName = name + "_renamed";
    String prompt = "You are an Android i18n expert";

    RepoType created = repoTypeService.createRepoType(name, "old", prompt, null);

    getL10nJCommander()
        .run(
            "repo-type-update",
            Param.REPO_TYPE_NAME_SHORT,
            name,
            Param.REPO_TYPE_NEW_NAME_SHORT,
            newName);

    assertUpdatedIdLine(created.getId());

    RepoType updated = repoTypeService.getRepoTypeById(created.getId());
    assertEquals(newName, updated.getName());
    assertEquals("old", updated.getDescription());
    assertEquals(prompt, updated.getAiPrompt());
  }

  @Test
  public void testUpdateAiPromptLongFlag() throws Exception {
    updateAiPrompt(Param.REPO_TYPE_AI_PROMPT_LONG, "PromptUpdateLong");
  }

  @Test
  public void testUpdateAiPromptShortFlag() throws Exception {
    updateAiPrompt(Param.REPO_TYPE_AI_PROMPT_SHORT, "PromptUpdateShort");
  }

  @Test
  public void testUpdateAiPromptFileLongFlag() throws Exception {
    updateAiPromptFile(Param.REPO_TYPE_AI_PROMPT_FILE_LONG, "PromptFileUpdateLong");
  }

  @Test
  public void testUpdateAiPromptFileShortFlag() throws Exception {
    updateAiPromptFile(Param.REPO_TYPE_AI_PROMPT_FILE_SHORT, "PromptFileUpdateShort");
  }

  private void updateAiPrompt(String aiPromptFlag, String entityKey) throws Exception {
    String name = testIdWatcher.getEntityName(entityKey);
    String newPrompt = "Updated type prompt";

    RepoType created = repoTypeService.createRepoType(name, "desc", "old prompt", null);

    getL10nJCommander()
        .run("repo-type-update", Param.REPO_TYPE_NAME_SHORT, name, aiPromptFlag, newPrompt);

    assertUpdatedIdLine(created.getId());

    RepoType updated = repoTypeService.getRepoTypeById(created.getId());
    assertEquals(name, updated.getName());
    assertEquals("desc", updated.getDescription());
    assertEquals(newPrompt, updated.getAiPrompt());
  }

  private void updateAiPromptFile(String aiPromptFileFlag, String entityKey) throws Exception {
    String name = testIdWatcher.getEntityName(entityKey);
    String newPrompt = "Updated from file\n\nPreserve {placeholders}.";
    java.io.File promptFile = java.io.File.createTempFile("mojito-ai-prompt-", ".md");
    promptFile.deleteOnExit();
    java.nio.file.Files.writeString(promptFile.toPath(), newPrompt);

    RepoType created = repoTypeService.createRepoType(name, "desc", "old prompt", null);

    getL10nJCommander()
        .run(
            "repo-type-update",
            Param.REPO_TYPE_NAME_SHORT,
            name,
            aiPromptFileFlag,
            promptFile.getAbsolutePath());

    assertUpdatedIdLine(created.getId());

    RepoType updated = repoTypeService.getRepoTypeById(created.getId());
    assertEquals(name, updated.getName());
    assertEquals("desc", updated.getDescription());
    assertEquals(newPrompt, updated.getAiPrompt());
  }

  @Test
  public void testClearAiPromptLongFlag() throws Exception {
    clearAiPrompt(Param.REPO_TYPE_AI_PROMPT_LONG, "PromptClearLong");
  }

  @Test
  public void testClearAiPromptShortFlag() throws Exception {
    clearAiPrompt(Param.REPO_TYPE_AI_PROMPT_SHORT, "PromptClearShort");
  }

  @Test
  public void testClearAiPromptWithEmptyFile() throws Exception {
    String name = testIdWatcher.getEntityName("PromptClearFile");
    java.io.File promptFile = java.io.File.createTempFile("mojito-ai-prompt-", ".md");
    promptFile.deleteOnExit();
    java.nio.file.Files.writeString(promptFile.toPath(), "");

    RepoType created = repoTypeService.createRepoType(name, "desc", "prompt to clear", null);

    getL10nJCommander()
        .run(
            "repo-type-update",
            Param.REPO_TYPE_NAME_SHORT,
            name,
            Param.REPO_TYPE_AI_PROMPT_FILE_SHORT,
            promptFile.getAbsolutePath());

    assertUpdatedIdLine(created.getId());

    RepoType updated = repoTypeService.getRepoTypeById(created.getId());
    assertEquals(name, updated.getName());
    assertEquals("desc", updated.getDescription());
    assertEquals("", updated.getAiPrompt());
  }

  private void clearAiPrompt(String aiPromptFlag, String entityKey) throws Exception {
    String name = testIdWatcher.getEntityName(entityKey);

    RepoType created = repoTypeService.createRepoType(name, "desc", "prompt to clear", null);

    getL10nJCommander().run("repo-type-update", Param.REPO_TYPE_NAME_SHORT, name, aiPromptFlag, "");

    assertUpdatedIdLine(created.getId());

    RepoType updated = repoTypeService.getRepoTypeById(created.getId());
    assertEquals(name, updated.getName());
    assertEquals("desc", updated.getDescription());
    assertEquals("", updated.getAiPrompt());
  }

  @Test
  public void testUpdateRejectsBothAiPromptAndAiPromptFile() throws Exception {
    String name = testIdWatcher.getEntityName("BothPromptFlagsUpdate");
    String originalPrompt = "keep me";
    java.io.File promptFile = java.io.File.createTempFile("mojito-ai-prompt-", ".md");
    promptFile.deleteOnExit();
    java.nio.file.Files.writeString(promptFile.toPath(), "from file");

    RepoType created = repoTypeService.createRepoType(name, "desc", originalPrompt, null);

    getL10nJCommander()
        .run(
            "repo-type-update",
            Param.REPO_TYPE_NAME_SHORT,
            name,
            Param.REPO_TYPE_AI_PROMPT_SHORT,
            "from flag",
            Param.REPO_TYPE_AI_PROMPT_FILE_SHORT,
            promptFile.getAbsolutePath());

    String output = outputCapture.toString();
    assertTrue(output.contains("Cannot specify both --ai-prompt and --ai-prompt-file"));
    assertFalse(
        "Mapped exclusive-flag error must not go through L10nJCommander's Unexpected error stack dump",
        output.contains("Unexpected error"));

    RepoType unchanged = repoTypeService.getRepoTypeById(created.getId());
    assertEquals(originalPrompt, unchanged.getAiPrompt());
  }

  @Test
  public void testUpdateMissingAiPromptFileIsAShortError() throws Exception {
    String name = testIdWatcher.getEntityName("MissingPromptFileUpdate");
    String originalPrompt = "keep me";
    String missing =
        java.nio.file.Files.createTempDirectory("mojito-ai-prompt-")
            .resolve("missing.md")
            .toString();

    RepoType created = repoTypeService.createRepoType(name, "desc", originalPrompt, null);

    getL10nJCommander()
        .run(
            "repo-type-update",
            Param.REPO_TYPE_NAME_SHORT,
            name,
            Param.REPO_TYPE_AI_PROMPT_FILE_LONG,
            missing);

    String output = outputCapture.toString();
    assertTrue(output.contains("Failed to read AI prompt file: " + missing));
    assertFalse(
        "Mapped file error must not go through L10nJCommander's Unexpected error stack dump",
        output.contains("Unexpected error"));

    RepoType unchanged = repoTypeService.getRepoTypeById(created.getId());
    assertEquals(originalPrompt, unchanged.getAiPrompt());
  }

  private void assertUpdatedIdLine(Long id) {
    assertTrue(
        "Repo type is not updated successfully",
        Pattern.compile(
                "updated --> repo type id: " + Pattern.quote(String.valueOf(id)) + "$",
                Pattern.MULTILINE)
            .matcher(outputCapture.toString())
            .find());
  }

  @Test
  public void testUpdateWithoutNewNameOrDescription() throws Exception {
    String name = testIdWatcher.getEntityName("NoOptions");

    repoTypeService.createRepoType(name, "original description", null, null);

    getL10nJCommander().run("repo-type-update", Param.REPO_TYPE_NAME_SHORT, name);

    assertTrue(
        "Expecting error when no optional update flags are provided",
        outputCapture
            .toString()
            .contains(
                "Must provide at least one of the following options: --new-name, --description, --ai-prompt, --ai-prompt-file"));

    RepoType unchanged = repoTypeRepository.findByName(name);
    assertNotNull(unchanged);
    assertEquals("original description", unchanged.getDescription());
  }

  @Test
  public void testUpdateNonExistingRepoType() throws Exception {
    String name = testIdWatcher.getEntityName("missing");

    getL10nJCommander()
        .run(
            "repo-type-update",
            Param.REPO_TYPE_NAME_SHORT,
            name,
            Param.REPO_TYPE_DESCRIPTION_SHORT,
            "new description");

    assertTrue(
        "Expecting error from updating non-existing repo type",
        outputCapture.toString().contains("Repo type with name [" + name + "] is not found"));
  }

  @Test
  public void testUpdateDuplicateName() throws Exception {
    String name1 = testIdWatcher.getEntityName("TypeA");
    String name2 = testIdWatcher.getEntityName("TypeB");

    RepoType typeA = repoTypeService.createRepoType(name1, null, null, null);
    RepoType typeB = repoTypeService.createRepoType(name2, null, null, null);

    getL10nJCommander()
        .run(
            "repo-type-update",
            Param.REPO_TYPE_NAME_SHORT,
            name1,
            Param.REPO_TYPE_NEW_NAME_SHORT,
            name2);

    String output = outputCapture.toString();
    assertTrue(
        "Expecting error from renaming to an existing repo type name",
        output.contains("RepoType with name [" + name2 + "] already exists"));
    assertFalse(
        "Mapped 409 must not go through L10nJCommander's Unexpected error stack dump",
        output.contains("Unexpected error"));

    RepoType stillA = repoTypeRepository.findByName(name1);
    assertNotNull("Rejected rename must leave the source type under its original name", stillA);
    assertEquals(typeA.getId(), stillA.getId());
    assertEquals(name1, stillA.getName());

    RepoType stillB = repoTypeRepository.findByName(name2);
    assertNotNull("Rejected rename must not overwrite the target type", stillB);
    assertEquals(typeB.getId(), stillB.getId());
    assertEquals(name2, stillB.getName());
  }

  @Test
  public void testUpdateBlankNameDoesNotSelectTheOnlyType() throws Exception {
    String name = testIdWatcher.getEntityName("OnlyType");
    String description = "must stay";

    repoTypeService.createRepoType(name, description, null, null);

    getL10nJCommander()
        .run(
            "repo-type-update",
            Param.REPO_TYPE_NAME_SHORT,
            " ",
            Param.REPO_TYPE_DESCRIPTION_SHORT,
            "oops");

    String output = outputCapture.toString();
    assertTrue(
        "Blank -n must fail with a required-name error, not list-all lookup",
        output.contains("Repo type name is required"));
    assertFalse(
        "Must not fall through to not-found from a list-all of size != 1",
        output.contains("is not found"));

    RepoType unchanged = repoTypeRepository.findByName(name);
    assertNotNull(unchanged);
    assertEquals(description, unchanged.getDescription());
  }

  @Test
  public void testUpdateDescriptionLongerThanMaxIsAShortError() throws Exception {
    String name = testIdWatcher.getEntityName("TooLong");
    String originalDescription = "keep me";

    repoTypeService.createRepoType(name, originalDescription, null, null);

    getL10nJCommander()
        .run(
            "repo-type-update",
            Param.REPO_TYPE_NAME_SHORT,
            name,
            Param.REPO_TYPE_DESCRIPTION_SHORT,
            "B".repeat(RepoType.DESCRIPTION_MAX_LENGTH + 1));

    String output = outputCapture.toString();
    assertTrue(
        "400 body must be shown as a CommandException, not a generic HTTP dump",
        output.contains(
            "description must be at most " + RepoType.DESCRIPTION_MAX_LENGTH + " characters"));
    assertFalse(
        "Mapped 400 must not go through L10nJCommander's Unexpected error stack dump",
        output.contains("Unexpected error"));

    RepoType unchanged = repoTypeRepository.findByName(name);
    assertNotNull(unchanged);
    assertEquals(originalDescription, unchanged.getDescription());
  }
}

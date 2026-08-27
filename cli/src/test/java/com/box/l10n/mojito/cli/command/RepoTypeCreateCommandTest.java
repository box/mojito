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
import java.util.regex.Pattern;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class RepoTypeCreateCommandTest extends CLITestBase {

  static Logger logger = LoggerFactory.getLogger(RepoTypeCreateCommandTest.class);

  @Autowired RepoTypeRepository repoTypeRepository;

  @Test
  public void testCreateRepoType() throws Exception {
    String name = testIdWatcher.getEntityName("React");
    String description = "FormatJS / react-intl apps";

    getL10nJCommander()
        .run(
            "repo-type-create",
            Param.REPO_TYPE_NAME_SHORT,
            name,
            Param.REPO_TYPE_DESCRIPTION_SHORT,
            description);

    RepoType created = repoTypeRepository.findByName(name);
    assertNotNull(created);
    assertCreatedIdLine(created.getId());
    assertEquals(name, created.getName());
    assertEquals(description, created.getDescription());
    assertEquals("", created.getAiPrompt());
  }

  @Test
  public void testCreateRepoTypeWithAiPromptLongFlag() throws Exception {
    createWithAiPrompt(Param.REPO_TYPE_AI_PROMPT_LONG, "ReactPromptLong");
  }

  @Test
  public void testCreateRepoTypeWithAiPromptShortFlag() throws Exception {
    createWithAiPrompt(Param.REPO_TYPE_AI_PROMPT_SHORT, "ReactPromptShort");
  }

  @Test
  public void testCreateRepoTypeWithAiPromptFileLongFlag() throws Exception {
    createWithAiPromptFile(Param.REPO_TYPE_AI_PROMPT_FILE_LONG, "ReactPromptFileLong");
  }

  @Test
  public void testCreateRepoTypeWithAiPromptFileShortFlag() throws Exception {
    createWithAiPromptFile(Param.REPO_TYPE_AI_PROMPT_FILE_SHORT, "ReactPromptFileShort");
  }

  private void createWithAiPrompt(String aiPromptFlag, String entityKey) throws Exception {
    String name = testIdWatcher.getEntityName(entityKey);
    String prompt = "You are a React i18n expert";

    getL10nJCommander()
        .run("repo-type-create", Param.REPO_TYPE_NAME_SHORT, name, aiPromptFlag, prompt);

    RepoType created = repoTypeRepository.findByName(name);
    assertNotNull(created);
    assertCreatedIdLine(created.getId());
    assertEquals(name, created.getName());
    assertNull(created.getDescription());
    assertEquals(prompt, created.getAiPrompt());
  }

  private void createWithAiPromptFile(String aiPromptFileFlag, String entityKey) throws Exception {
    String name = testIdWatcher.getEntityName(entityKey);
    String prompt = "Line one\n\n## Heading\nPreserve {placeholders}.";
    java.io.File promptFile = java.io.File.createTempFile("mojito-ai-prompt-", ".md");
    promptFile.deleteOnExit();
    java.nio.file.Files.writeString(promptFile.toPath(), prompt);

    getL10nJCommander()
        .run(
            "repo-type-create",
            Param.REPO_TYPE_NAME_SHORT,
            name,
            aiPromptFileFlag,
            promptFile.getAbsolutePath());

    RepoType created = repoTypeRepository.findByName(name);
    assertNotNull(created);
    assertCreatedIdLine(created.getId());
    assertEquals(name, created.getName());
    assertNull(created.getDescription());
    assertEquals(prompt, created.getAiPrompt());
  }

  @Test
  public void testCreateAiPromptFileStripsTrailingNewline() throws Exception {
    String name = testIdWatcher.getEntityName("PromptFileTrailingNewline");
    java.io.File promptFile = java.io.File.createTempFile("mojito-ai-prompt-", ".md");
    promptFile.deleteOnExit();
    java.nio.file.Files.writeString(promptFile.toPath(), "hello\n");

    getL10nJCommander()
        .run(
            "repo-type-create",
            Param.REPO_TYPE_NAME_SHORT,
            name,
            Param.REPO_TYPE_AI_PROMPT_FILE_SHORT,
            promptFile.getAbsolutePath());

    RepoType created = repoTypeRepository.findByName(name);
    assertNotNull(created);
    assertEquals("hello", created.getAiPrompt());
  }

  @Test
  public void testCreateRejectsBothAiPromptAndAiPromptFile() throws Exception {
    String name = testIdWatcher.getEntityName("BothPromptFlags");
    java.io.File promptFile = java.io.File.createTempFile("mojito-ai-prompt-", ".md");
    promptFile.deleteOnExit();
    java.nio.file.Files.writeString(promptFile.toPath(), "from file");

    getL10nJCommander()
        .run(
            "repo-type-create",
            Param.REPO_TYPE_NAME_SHORT,
            name,
            Param.REPO_TYPE_AI_PROMPT_SHORT,
            "from flag",
            Param.REPO_TYPE_AI_PROMPT_FILE_SHORT,
            promptFile.getAbsolutePath());

    String output = outputCapture.toString();
    assertTrue(output.contains("Cannot specify both --ai-prompt and --ai-prompt-file"));
    assertNull(repoTypeRepository.findByName(name));
  }

  @Test
  public void testCreateMissingAiPromptFileIsAShortError() throws Exception {
    String name = testIdWatcher.getEntityName("MissingPromptFile");
    String missing =
        java.nio.file.Files.createTempDirectory("mojito-ai-prompt-")
            .resolve("missing.md")
            .toString();

    getL10nJCommander()
        .run(
            "repo-type-create",
            Param.REPO_TYPE_NAME_SHORT,
            name,
            Param.REPO_TYPE_AI_PROMPT_FILE_LONG,
            missing);

    String output = outputCapture.toString();
    assertTrue(output.contains("Failed to read AI prompt file: " + missing));
    assertFalse(
        "Mapped file error must not go through L10nJCommander's Unexpected error stack dump",
        output.contains("Unexpected error"));
    assertNull(repoTypeRepository.findByName(name));
  }

  @Test
  public void testCreateInvalidAiPromptFilePathIsAShortError() throws Exception {
    String name = testIdWatcher.getEntityName("InvalidPromptFilePath");
    String invalid = "bad\0path.md";

    getL10nJCommander()
        .run(
            "repo-type-create",
            Param.REPO_TYPE_NAME_SHORT,
            name,
            Param.REPO_TYPE_AI_PROMPT_FILE_LONG,
            invalid);

    String output = outputCapture.toString();
    assertTrue(
        "Invalid path must map to the same short file-read error as a missing file",
        output.contains("Failed to read AI prompt file:"));
    assertFalse(
        "Mapped file error must not go through L10nJCommander's Unexpected error dump",
        output.contains("Unexpected error"));
    assertNull(repoTypeRepository.findByName(name));
  }

  @Test
  public void testCreateRepoTypeWithoutDescription() throws Exception {
    String name = testIdWatcher.getEntityName("NoDesc");

    getL10nJCommander().run("repo-type-create", Param.REPO_TYPE_NAME_SHORT, name);

    RepoType created = repoTypeRepository.findByName(name);
    assertNotNull(created);
    assertCreatedIdLine(created.getId());
    assertEquals(name, created.getName());
    assertNull(created.getDescription());
    assertEquals("", created.getAiPrompt());
  }

  private void assertCreatedIdLine(Long id) {
    assertTrue(
        Pattern.compile(
                "created --> repo type id: " + Pattern.quote(String.valueOf(id)) + "$",
                Pattern.MULTILINE)
            .matcher(outputCapture.toString())
            .find());
  }

  @Test
  public void testCreateRepoTypeDuplicateName() throws Exception {
    String name = testIdWatcher.getEntityName("Conflict");

    getL10nJCommander().run("repo-type-create", Param.REPO_TYPE_NAME_SHORT, name);
    getL10nJCommander().run("repo-type-create", Param.REPO_TYPE_NAME_SHORT, name);

    String output = outputCapture.toString();
    assertTrue(output.contains("RepoType with name [" + name + "] already exists"));
    assertFalse(
        "Mapped 409 must not go through L10nJCommander's Unexpected error stack dump",
        output.contains("Unexpected error"));
  }

  @Test
  public void testCreateNameLongerThanMaxIsAShortError() throws Exception {
    String name = "A".repeat(RepoType.NAME_MAX_LENGTH + 1);

    getL10nJCommander().run("repo-type-create", Param.REPO_TYPE_NAME_SHORT, name);

    String output = outputCapture.toString();
    assertTrue(
        "400 body must be shown as a CommandException, not a generic HTTP dump",
        output.contains("name must be at most " + RepoType.NAME_MAX_LENGTH + " characters"));
    assertFalse(
        "Mapped 400 must not go through L10nJCommander's Unexpected error stack dump",
        output.contains("Unexpected error"));
    assertNull(repoTypeRepository.findByName(name));
  }
}

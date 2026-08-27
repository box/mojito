package com.box.l10n.mojito.cli.command;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class CommandHelperRepoTypeAiPromptTest {

  @Rule public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Test
  public void resolveReturnsNullWhenNeitherFlagSet() {
    assertNull(CommandHelper.resolveRepoTypeAiPrompt(null, null));
  }

  @Test
  public void resolveReturnsInlinePrompt() {
    assertEquals(
        "You are a React i18n expert",
        CommandHelper.resolveRepoTypeAiPrompt("You are a React i18n expert", null));
  }

  @Test
  public void resolveReturnsEmptyInlinePrompt() {
    assertEquals("", CommandHelper.resolveRepoTypeAiPrompt("", null));
  }

  @Test
  public void resolveDoesNotStripInlineTrailingNewline() {
    assertEquals("hello\n", CommandHelper.resolveRepoTypeAiPrompt("hello\n", null));
  }

  @Test
  public void resolveReadsMultilinePromptFromFile() throws Exception {
    Path promptFile = temporaryFolder.newFile("prompt.md").toPath();
    String prompt = "Line one\n\n## Heading\nPreserve {placeholders}.";
    Files.writeString(promptFile, prompt, StandardCharsets.UTF_8);

    assertEquals(
        prompt,
        CommandHelper.resolveRepoTypeAiPrompt(null, promptFile.toAbsolutePath().toString()));
  }

  @Test
  public void resolveReadsEmptyFileAsEmptyPrompt() throws Exception {
    Path promptFile = temporaryFolder.newFile("empty.md").toPath();
    Files.writeString(promptFile, "", StandardCharsets.UTF_8);

    assertEquals(
        "", CommandHelper.resolveRepoTypeAiPrompt(null, promptFile.toAbsolutePath().toString()));
  }

  @Test
  public void resolveStripsOneTrailingLf() throws Exception {
    Path promptFile = temporaryFolder.newFile("lf.md").toPath();
    Files.writeString(promptFile, "hello\n", StandardCharsets.UTF_8);

    assertEquals(
        "hello",
        CommandHelper.resolveRepoTypeAiPrompt(null, promptFile.toAbsolutePath().toString()));
  }

  @Test
  public void resolveStripsOneTrailingCrlf() throws Exception {
    Path promptFile = temporaryFolder.newFile("crlf.md").toPath();
    Files.writeString(promptFile, "hello\r\n", StandardCharsets.UTF_8);

    assertEquals(
        "hello",
        CommandHelper.resolveRepoTypeAiPrompt(null, promptFile.toAbsolutePath().toString()));
  }

  @Test
  public void resolveKeepsASecondTrailingNewline() throws Exception {
    Path promptFile = temporaryFolder.newFile("two-newlines.md").toPath();
    Files.writeString(promptFile, "hello\n\n", StandardCharsets.UTF_8);

    assertEquals(
        "hello\n",
        CommandHelper.resolveRepoTypeAiPrompt(null, promptFile.toAbsolutePath().toString()));
  }

  @Test
  public void resolveStripsUtf8Bom() throws Exception {
    Path promptFile = temporaryFolder.newFile("bom.md").toPath();
    Files.writeString(promptFile, "\uFEFFhello\n", StandardCharsets.UTF_8);

    assertEquals(
        "hello",
        CommandHelper.resolveRepoTypeAiPrompt(null, promptFile.toAbsolutePath().toString()));
  }

  @Test
  public void resolveNewlineOnlyFileIsEmptyPrompt() throws Exception {
    Path promptFile = temporaryFolder.newFile("newline-only.md").toPath();
    Files.writeString(promptFile, "\n", StandardCharsets.UTF_8);

    assertEquals(
        "", CommandHelper.resolveRepoTypeAiPrompt(null, promptFile.toAbsolutePath().toString()));
  }

  @Test
  public void resolveDirectoryPathIsAShortError() throws Exception {
    String directory = temporaryFolder.newFolder("prompt-dir").getAbsolutePath();
    try {
      CommandHelper.resolveRepoTypeAiPrompt(null, directory);
      fail("Expected CommandException");
    } catch (CommandException e) {
      assertTrue(e.getMessage().startsWith("Failed to read AI prompt file: " + directory + ":"));
    }
  }

  @Test
  public void resolveRejectsBothFlags() {
    try {
      CommandHelper.resolveRepoTypeAiPrompt("inline", "prompt.md");
      fail("Expected CommandException");
    } catch (CommandException e) {
      assertEquals("Cannot specify both --ai-prompt and --ai-prompt-file", e.getMessage());
    }
  }

  @Test
  public void resolveMissingFileIsAShortError() {
    String missing = temporaryFolder.getRoot().toPath().resolve("missing.md").toString();
    try {
      CommandHelper.resolveRepoTypeAiPrompt(null, missing);
      fail("Expected CommandException");
    } catch (CommandException e) {
      assertTrue(e.getMessage().startsWith("Failed to read AI prompt file: " + missing + ":"));
    }
  }

  @Test
  public void resolveInvalidPathIsAShortError() {
    String invalid = "bad\0path.md";
    try {
      CommandHelper.resolveRepoTypeAiPrompt(null, invalid);
      fail("Expected CommandException");
    } catch (CommandException e) {
      assertTrue(e.getMessage().startsWith("Failed to read AI prompt file: " + invalid + ":"));
    }
  }
}

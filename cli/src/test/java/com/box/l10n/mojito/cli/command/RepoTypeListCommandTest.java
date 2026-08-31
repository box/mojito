package com.box.l10n.mojito.cli.command;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.box.l10n.mojito.cli.CLITestBase;
import com.box.l10n.mojito.cli.command.param.Param;
import com.box.l10n.mojito.cli.console.ConsoleWriter;
import com.box.l10n.mojito.entity.RepoType;
import com.box.l10n.mojito.rest.client.RepoTypeClient;
import com.box.l10n.mojito.service.repotype.RepoTypeService;
import java.util.Collections;
import java.util.regex.Pattern;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class RepoTypeListCommandTest extends CLITestBase {

  static Logger logger = LoggerFactory.getLogger(RepoTypeListCommandTest.class);

  @Autowired RepoTypeService repoTypeService;

  @Test
  public void testListAllRepoTypes() throws Exception {
    String nameApple = testIdWatcher.getEntityName("Apple");
    String nameZebra = testIdWatcher.getEntityName("Zebra");
    String prompt = "You are a React i18n expert — keep this body off the list";

    RepoType apple = repoTypeService.createRepoType(nameApple, "first type", prompt, null);
    RepoType zebra = repoTypeService.createRepoType(nameZebra, null, null, null);

    getL10nJCommander().run("repo-type-list");

    String output = outputCapture.toString();
    assertTrue(output.contains("List repo types"));

    int appleIdx = labeledLineIndex(output, "Name --> ", nameApple);
    int zebraIdx = labeledLineIndex(output, "Name --> ", nameZebra);
    assertTrue("Listed types must be ordered by name", appleIdx < zebraIdx);

    assertLabeledLine(output, "Repo type id --> ", String.valueOf(apple.getId()));
    assertLabeledLine(output, "Description --> ", "first type");
    assertLabeledLine(output, "AI prompt --> ", "set");
    assertLabeledLine(output, "Repo type id --> ", String.valueOf(zebra.getId()));
    assertTrue(
        "Null description must print an empty value after the label",
        Pattern.compile("Description --> $", Pattern.MULTILINE).matcher(output).find());
    assertTrue(
        "Empty AI prompt must print an empty value after the label, not \"set\"",
        Pattern.compile("AI prompt --> $", Pattern.MULTILINE).matcher(output).find());
    assertFalse(
        "List must not dump the AI prompt body; use repo-type-view for that",
        output.contains(prompt));
  }

  @Test
  public void testListEmptyCatalog() throws Exception {
    RepoTypeListCommand command = new RepoTypeListCommand();
    RepoTypeClient repoTypeClient = mock(RepoTypeClient.class);
    when(repoTypeClient.getRepoTypes(null)).thenReturn(Collections.emptyList());
    command.repoTypeClient = repoTypeClient;
    command.consoleWriter =
        new ConsoleWriter(false, ConsoleWriter.OutputType.ANSI_CONSOLE_AND_LOGGER);

    command.execute();

    String output = outputCapture.toString();
    assertTrue(output.contains("List repo types"));
    assertTrue(output.contains("No repo types found"));
    assertFalse(output.contains("Repo type id -->"));
  }

  @Test
  public void testListHelpDocumentsListAll() throws Exception {
    getL10nJCommander().run("repo-type-list", "-h");

    String output = outputCapture.toString();
    assertTrue("CLI help must document list-all usage", output.contains("List all repo types"));
    assertTrue(output.contains("repo-type-list"));
    assertTrue(output.contains(Param.REPO_TYPE_LIST_VERBOSE_LONG));
  }

  @Test
  public void testListVerbosePrintsAiPromptBody() throws Exception {
    String name = testIdWatcher.getEntityName("WithPrompt");
    String prompt = "You are a React i18n expert";

    repoTypeService.createRepoType(name, "desc", prompt, null);

    getL10nJCommander().run("repo-type-list", Param.REPO_TYPE_LIST_VERBOSE_LONG);

    String output = outputCapture.toString();
    assertLabeledLine(output, "Name --> ", name);
    assertLabeledLine(output, "AI prompt --> ", prompt);
    assertFalse(
        "Verbose must print the prompt body, not the compact \"set\" marker",
        Pattern.compile("AI prompt --> set$", Pattern.MULTILINE).matcher(output).find());
  }

  @Test
  public void testViewUnknownNameStillFails() throws Exception {
    String name = testIdWatcher.getEntityName("OnlyForList");
    repoTypeService.createRepoType(name, "listed type", null, null);

    String missing = testIdWatcher.getEntityName("missing");
    getL10nJCommander().run("repo-type-view", Param.REPO_TYPE_NAME_SHORT, missing);

    String output = outputCapture.toString();
    assertTrue(
        "Exact-name view must still fail when the name is unknown",
        output.contains("Repo type with name [" + missing + "] is not found"));
    assertFalse(output.contains("List repo types"));
  }

  private static int labeledLineIndex(String output, String label, String value) {
    Pattern pattern =
        Pattern.compile(Pattern.quote(label) + Pattern.quote(value) + "$", Pattern.MULTILINE);
    java.util.regex.Matcher matcher = pattern.matcher(output);
    assertTrue(label + " is missing or incorrect from output", matcher.find());
    return matcher.start();
  }

  private static void assertLabeledLine(String output, String label, String value) {
    labeledLineIndex(output, label, value);
  }
}

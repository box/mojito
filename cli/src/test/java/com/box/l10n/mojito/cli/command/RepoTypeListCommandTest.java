package com.box.l10n.mojito.cli.command;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.box.l10n.mojito.cli.CLITestBase;
import com.box.l10n.mojito.cli.command.param.Param;
import com.box.l10n.mojito.entity.RepoType;
import com.box.l10n.mojito.service.repotype.RepoTypeService;
import java.util.regex.Matcher;
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
    String nameSpace = testIdWatcher.getEntityName("Space");
    String nameZebra = testIdWatcher.getEntityName("Zebra");
    String prompt = "compact-omit-body-" + nameApple;

    RepoType apple = repoTypeService.createRepoType(nameApple, "first type", prompt, null);
    RepoType space = repoTypeService.createRepoType(nameSpace, "whitespace prompt", " ", null);
    RepoType zebra = repoTypeService.createRepoType(nameZebra, null, null, null);

    getL10nJCommander().run("repo-type-list");

    String output = outputCapture.toString();
    assertTrue(output.contains("List repo types"));

    int appleIdx = labeledLineIndex(output, "Name --> ", nameApple);
    int spaceIdx = labeledLineIndex(output, "Name --> ", nameSpace);
    int zebraIdx = labeledLineIndex(output, "Name --> ", nameZebra);
    assertTrue("Listed types must be ordered by name", appleIdx < spaceIdx && spaceIdx < zebraIdx);

    String appleBlock = typeBlock(output, apple.getId());
    assertLabeledLine(appleBlock, "Name --> ", nameApple);
    assertLabeledLine(appleBlock, "Description --> ", "first type");
    assertLabeledLine(appleBlock, "AI prompt --> ", "contains a value");
    assertFalse(
        "Default list must not dump this type's AI prompt body (use --verbose)",
        appleBlock.contains(prompt));

    String spaceBlock = typeBlock(output, space.getId());
    assertLabeledLine(spaceBlock, "Name --> ", nameSpace);
    assertLabeledLine(spaceBlock, "AI prompt --> ", "contains a value");

    String zebraBlock = typeBlock(output, zebra.getId());
    assertLabeledLine(zebraBlock, "Name --> ", nameZebra);
    assertTrue(
        "Null description must print an empty value after the label",
        Pattern.compile("Description --> $", Pattern.MULTILINE).matcher(zebraBlock).find());
    assertTrue(
        "Empty AI prompt must print an empty value after the label, not \"contains a value\"",
        Pattern.compile("AI prompt --> $", Pattern.MULTILINE).matcher(zebraBlock).find());
  }

  @Test
  public void testListHelpDocumentsListAll() throws Exception {
    getL10nJCommander().run("repo-type-list", "-h");

    String output = outputCapture.toString();
    assertTrue("CLI help must document list-all usage", output.contains("List all repo types"));
    assertTrue(output.contains("repo-type-list"));
    assertTrue(output.contains(Param.REPO_TYPE_LIST_VERBOSE_LONG));
    assertTrue(output.contains(Param.REPO_TYPE_LIST_VERBOSE_SHORT));
  }

  @Test
  public void testListVerbosePrintsAiPromptBody() throws Exception {
    String name = testIdWatcher.getEntityName("WithPrompt");
    String emptyName = testIdWatcher.getEntityName("EmptyPrompt");
    String prompt = "verbose-body-" + name;

    RepoType created = repoTypeService.createRepoType(name, "desc", prompt, null);
    RepoType empty = repoTypeService.createRepoType(emptyName, "no prompt", null, null);

    getL10nJCommander().run("repo-type-list", Param.REPO_TYPE_LIST_VERBOSE_LONG);

    String output = outputCapture.toString();
    String block = typeBlock(output, created.getId());
    assertLabeledLine(block, "Name --> ", name);
    assertLabeledLine(block, "AI prompt --> ", prompt);

    String emptyBlock = typeBlock(output, empty.getId());
    assertLabeledLine(emptyBlock, "Name --> ", emptyName);
    assertTrue(
        "Verbose null prompt must print an empty value, not the string \"null\"",
        Pattern.compile("AI prompt --> $", Pattern.MULTILINE).matcher(emptyBlock).find());
  }

  static String typeBlock(String output, Long id) {
    Pattern start =
        Pattern.compile("^" + Pattern.quote("Repo type id --> " + id) + "$", Pattern.MULTILINE);
    Matcher matcher = start.matcher(output);
    assertTrue("missing output block for repo type id " + id, matcher.find());
    int from = matcher.start();
    Matcher next = Pattern.compile("^Repo type id --> ", Pattern.MULTILINE).matcher(output);
    int end = next.find(matcher.end()) ? next.start() : output.length();
    return output.substring(from, end);
  }

  private static int labeledLineIndex(String output, String label, String value) {
    Pattern pattern =
        Pattern.compile(Pattern.quote(label) + Pattern.quote(value) + "$", Pattern.MULTILINE);
    Matcher matcher = pattern.matcher(output);
    assertTrue(label + " is missing or incorrect from output", matcher.find());
    return matcher.start();
  }

  private static void assertLabeledLine(String output, String label, String value) {
    labeledLineIndex(output, label, value);
  }
}

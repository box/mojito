package com.box.l10n.mojito.cli.command;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.box.l10n.mojito.cli.CLITestBase;
import com.box.l10n.mojito.cli.command.utils.SlackNotificationSender;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcher;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcherParameters;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.AssertionErrors;

/**
 * @author jeanaurambault
 */
public class ExtractionDiffCommandTest extends CLITestBase {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(ExtractionDiffCommandTest.class);

  @Autowired TextUnitSearcher textUnitSearcher;

  @Before
  public void init() {
    resetHost();
  }

  @Test
  public void extractDiff() throws Exception {
    Repository repository = createTestRepoUsingRepoService();

    getL10nJCommander()
        .run(
            "extract",
            "-s",
            getInputResourcesTestDir("source1").getAbsolutePath(),
            "-o",
            getTargetTestDir("extractions").getAbsolutePath(),
            "-n",
            "source1",
            "-fo",
            "sometestoption=value1");

    getL10nJCommander()
        .run(
            "extract",
            "-s",
            getInputResourcesTestDir("source2").getAbsolutePath(),
            "-o",
            getTargetTestDir("extractions").getAbsolutePath(),
            "-n",
            "source2",
            "-fo",
            "sometestoption=value1");

    getL10nJCommander()
        .run(
            "extract",
            "-s",
            getInputResourcesTestDir("source3").getAbsolutePath(),
            "-o",
            getTargetTestDir("extractions").getAbsolutePath(),
            "-n",
            "source3",
            "-fo",
            "sometestoption=value1");

    getL10nJCommander()
        .run(
            "extract",
            "-s",
            getInputResourcesTestDir("source4").getAbsolutePath(),
            "-o",
            getTargetTestDir("extractions").getAbsolutePath(),
            "-n",
            "source4",
            "-fo",
            "sometestoption=value1");

    getL10nJCommander()
        .run(
            "extract-diff",
            "-i",
            getTargetTestDir("extractions").getAbsolutePath(),
            "-o",
            getTargetTestDir("extraction-diffs").getAbsolutePath(),
            "-c",
            "source1",
            "-b",
            "source1",
            "--push-to",
            repository.getName());

    AssertionErrors.assertTrue(
        "there shouldn't be any text units", getTextUnitDTOS(repository).isEmpty());

    getL10nJCommander()
        .run(
            "extract-diff",
            "-i",
            getTargetTestDir("extractions").getAbsolutePath(),
            "-o",
            getTargetTestDir("extraction-diffs").getAbsolutePath(),
            "-c",
            "source2",
            "-b",
            "source1",
            "--push-to",
            repository.getName());

    AssertionErrors.assertTrue(
        "there shouldn't be any text units", getTextUnitDTOS(repository).isEmpty());

    getL10nJCommander()
        .run(
            "extract-diff",
            "-i",
            getTargetTestDir("extractions").getAbsolutePath(),
            "-o",
            getTargetTestDir("extraction-diffs").getAbsolutePath(),
            "-c",
            "source3",
            "-b",
            "source1",
            "--push-to",
            repository.getName());

    List<TextUnitDTO> textUnitDTOS = getTextUnitDTOS(repository);
    assertEquals(1L, textUnitDTOS.size());
    assertEquals("1 day update --- 1_day_duration", textUnitDTOS.get(0).getName());
    assertEquals("1 day update", textUnitDTOS.get(0).getSource());
    assertEquals("File lock dialog duration", textUnitDTOS.get(0).getComment());
    assertEquals("LC_MESSAGES/messages.pot", textUnitDTOS.get(0).getAssetPath());

    getL10nJCommander()
        .run(
            "extract-diff",
            "-i",
            getTargetTestDir("extractions").getAbsolutePath(),
            "-o",
            getTargetTestDir("extraction-diffs").getAbsolutePath(),
            "-c",
            "source4",
            "-b",
            "source1",
            "--push-to",
            repository.getName());

    textUnitDTOS = getTextUnitDTOS(repository);
    assertEquals(8L, textUnitDTOS.size());
    assertEquals("1 day update --- 1_day_duration", textUnitDTOS.get(0).getName());
    assertEquals("1 day update", textUnitDTOS.get(0).getSource());
    assertEquals("File lock dialog duration", textUnitDTOS.get(0).getComment());
    assertEquals("LC_MESSAGES/messages.pot", textUnitDTOS.get(0).getAssetPath());

    assertEquals(
        "100 character description: --- 100_character_description_", textUnitDTOS.get(1).getName());
    assertEquals("100 character description:", textUnitDTOS.get(1).getSource());
    assertEquals(null, textUnitDTOS.get(1).getComment());
    assertEquals("LC_MESSAGES/messages2.pot", textUnitDTOS.get(1).getAssetPath());

    assertEquals("15 min --- 15_min_duration", textUnitDTOS.get(2).getName());
    assertEquals("15 min", textUnitDTOS.get(2).getSource());
    assertEquals("File lock dialog duration", textUnitDTOS.get(2).getComment());
    assertEquals("LC_MESSAGES/messages2.pot", textUnitDTOS.get(2).getAssetPath());

    assertEquals("1 day --- 1_day_duration", textUnitDTOS.get(3).getName());
    assertEquals("1 day", textUnitDTOS.get(3).getSource());
    assertEquals("File lock dialog duration", textUnitDTOS.get(3).getComment());
    assertEquals("LC_MESSAGES/messages2.pot", textUnitDTOS.get(3).getAssetPath());

    assertEquals("1 hour --- 1_hour_duration", textUnitDTOS.get(4).getName());
    assertEquals("1 hour", textUnitDTOS.get(4).getSource());
    assertEquals("File lock dialog duration", textUnitDTOS.get(4).getComment());
    assertEquals("LC_MESSAGES/messages2.pot", textUnitDTOS.get(4).getAssetPath());

    assertEquals("1 month --- 1_month_duration", textUnitDTOS.get(5).getName());
    assertEquals("1 month", textUnitDTOS.get(5).getSource());
    assertEquals("File lock dialog duration", textUnitDTOS.get(5).getComment());
    assertEquals("LC_MESSAGES/messages2.pot", textUnitDTOS.get(5).getAssetPath());

    assertEquals("There is {number} car --- car _one", textUnitDTOS.get(6).getName());
    assertEquals("There is {number} car", textUnitDTOS.get(6).getSource());
    assertEquals("Test plural", textUnitDTOS.get(6).getComment());
    assertEquals("one", textUnitDTOS.get(6).getPluralForm());
    assertEquals("LC_MESSAGES/messages2.pot", textUnitDTOS.get(6).getAssetPath());

    assertEquals("There is {number} car --- car _other", textUnitDTOS.get(7).getName());
    assertEquals("There are {number} cars", textUnitDTOS.get(7).getSource());
    assertEquals("Test plural", textUnitDTOS.get(7).getComment());
    assertEquals("other", textUnitDTOS.get(7).getPluralForm());
    assertEquals("LC_MESSAGES/messages2.pot", textUnitDTOS.get(7).getAssetPath());

    checkExpectedGeneratedResources();
  }

  @Test
  public void buildFailSafeMailCommand() {
    ExtractionDiffCommand extractionDiffCommand = new ExtractionDiffCommand();
    extractionDiffCommand.failSafeEmail = "username@test.com";
    extractionDiffCommand.failSafeMessage = "https://mybuildurl.org/1234";
    extractionDiffCommand.pushToBranchName = "BRANCH";
    assertEquals(
        "echo 'https://mybuildurl.org/1234' | mail -s 'Extraction diff command failed for branch: BRANCH' username@test.com",
        extractionDiffCommand.buildFailSafeMailCommand());
  }

  @Test
  public void buildFailSafeMailCommandAllNull() {
    ExtractionDiffCommand extractionDiffCommand = new ExtractionDiffCommand();
    assertEquals(
        "echo 'null' | mail -s 'Extraction diff command failed for branch: null' null",
        extractionDiffCommand.buildFailSafeMailCommand());
  }

  @Test
  public void testRepositoryFallback() throws Exception {
    Repository repository = createTestRepoUsingRepoService();

    getL10nJCommander()
        .run(
            "extract",
            "-s",
            getInputResourcesTestDir("source1").getAbsolutePath(),
            "-o",
            getTargetTestDir("extractions").getAbsolutePath(),
            "-n",
            "source1",
            "-fo",
            "sometestoption=value1");

    getL10nJCommander()
        .run(
            "extract",
            "-s",
            getInputResourcesTestDir("source3").getAbsolutePath(),
            "-o",
            getTargetTestDir("extractions").getAbsolutePath(),
            "-n",
            "source3",
            "-fo",
            "sometestoption=value1");

    getL10nJCommander()
        .run(
            "extract-diff",
            "-i",
            getTargetTestDir("extractions").getAbsolutePath(),
            "-o",
            getTargetTestDir("extraction-diffs").getAbsolutePath(),
            "-c",
            "source3",
            "-b",
            "source1",
            "--push-to",
            "missingRepo",
            "--push-to-fallback",
            repository.getName());

    // Expecting the fallback repo to be used and to see one text unit in it
    List<TextUnitDTO> textUnitDTOS = getTextUnitDTOS(repository);
    assertEquals(1L, textUnitDTOS.size());

    L10nJCommander l10nJCommander = getL10nJCommander();
    l10nJCommander.run(
        "extract-diff",
        "-i",
        getTargetTestDir("extractions").getAbsolutePath(),
        "-o",
        getTargetTestDir("extraction-diffs").getAbsolutePath(),
        "-c",
        "source3",
        "-b",
        "source1",
        "--push-to",
        "missingRepo",
        "--push-to-fallback",
        "anotherMissingRepo");

    // Expecting the command to fail because none of the provided repos were valid options
    Assert.assertEquals(1L, l10nJCommander.getExitCode());
  }

  @Test
  public void noDiffChangesNoDatabaseCalls() throws Exception {
    Repository repository = createTestRepoUsingRepoService();

    getL10nJCommander()
        .run(
            "extract",
            "-s",
            getInputResourcesTestDir("source1").getAbsolutePath(),
            "-o",
            getTargetTestDir("extractions").getAbsolutePath(),
            "-n",
            "source1",
            "-fo",
            "sometestoption=value1");

    getL10nJCommander()
        .run(
            "extract",
            "-s",
            getInputResourcesTestDir("source3").getAbsolutePath(),
            "-o",
            getTargetTestDir("extractions").getAbsolutePath(),
            "-n",
            "source3",
            "-fo",
            "sometestoption=value1");

    setNonExistentHost();

    L10nJCommander l10nJCommander = getL10nJCommander();
    l10nJCommander.run(
        "extract-diff",
        "-i",
        getTargetTestDir("extractions").getAbsolutePath(),
        "-o",
        getTargetTestDir("extraction-diffs").getAbsolutePath(),
        "-c",
        "source1",
        "-b",
        "source1",
        "--push-to",
        repository.getName());

    // No failure expected as no database call should be made here
    Assert.assertEquals(0L, l10nJCommander.getExitCode());

    l10nJCommander = getL10nJCommander();
    l10nJCommander.run(
        "extract-diff",
        "-i",
        getTargetTestDir("extractions").getAbsolutePath(),
        "-o",
        getTargetTestDir("extraction-diffs").getAbsolutePath(),
        "-c",
        "source3",
        "-b",
        "source1",
        "--push-to",
        repository.getName());

    // Failure expected as a database call will be made when text units are added
    Assert.assertNotEquals(0L, l10nJCommander.getExitCode());
  }

  List<TextUnitDTO> getTextUnitDTOS(Repository repository) {
    TextUnitSearcherParameters textUnitSearcherParameters = new TextUnitSearcherParameters();
    textUnitSearcherParameters.setRepositoryIds(repository.getId());
    textUnitSearcherParameters.setForRootLocale(true);
    return textUnitSearcher.search(textUnitSearcherParameters).stream()
        .sorted(Comparator.comparing(TextUnitDTO::getTmTextUnitVariantId))
        .collect(Collectors.toList());
  }

  @Test
  public void testMaxStringsBlockSuccess() throws Exception {
    Repository repository = createTestRepoUsingRepoService();

    getL10nJCommander()
        .run(
            "extract",
            "-s",
            getInputResourcesTestDir("source1").getAbsolutePath(),
            "-o",
            getTargetTestDir("extractions").getAbsolutePath(),
            "-n",
            "source1");

    getL10nJCommander()
        .run(
            "extract",
            "-s",
            getInputResourcesTestDir("source3").getAbsolutePath(),
            "-o",
            getTargetTestDir("extractions").getAbsolutePath(),
            "-n",
            "source3");

    getL10nJCommander()
        .run(
            "extract-diff",
            "-i",
            getTargetTestDir("extractions").getAbsolutePath(),
            "-o",
            getTargetTestDir("extraction-diffs").getAbsolutePath(),
            "-c",
            "source3",
            "-b",
            "source1",
            "-p",
            repository.getName(),
            "-pb",
            "branch");

    List<TextUnitDTO> textUnitDTOS = getTextUnitDTOS(repository);
    assertEquals(3L, textUnitDTOS.size());

    assertEquals("1 day update --- 1_day_duration", textUnitDTOS.get(0).getName());
    assertEquals("1 day update", textUnitDTOS.get(0).getSource());
    assertEquals("File lock dialog duration", textUnitDTOS.get(0).getComment());
    assertEquals("LC_MESSAGES/messages.pot", textUnitDTOS.get(0).getAssetPath());

    assertEquals("1 month update --- 1_month_duration", textUnitDTOS.get(1).getName());
    assertEquals("1 month update", textUnitDTOS.get(1).getSource());
    assertEquals("File lock dialog duration", textUnitDTOS.get(1).getComment());
    assertEquals("LC_MESSAGES/messages.pot", textUnitDTOS.get(1).getAssetPath());

    assertEquals("1 year --- 1_year_duration", textUnitDTOS.get(2).getName());
    assertEquals("1 year", textUnitDTOS.get(2).getSource());
    assertEquals("File lock dialog duration", textUnitDTOS.get(2).getComment());
    assertEquals("LC_MESSAGES/messages.pot", textUnitDTOS.get(2).getAssetPath());

    L10nJCommander l10nJCommander = getL10nJCommander();
    l10nJCommander.run(
        "extract-diff",
        "-i",
        getTargetTestDir("extractions").getAbsolutePath(),
        "-o",
        getTargetTestDir("extraction-diffs").getAbsolutePath(),
        "-c",
        "source3",
        "-b",
        "source1",
        "-p",
        repository.getName(),
        "-pb",
        "branch",
        "-smsb",
        "-msab",
        "1");

    Assert.assertEquals(0L, l10nJCommander.getExitCode());

    l10nJCommander = getL10nJCommander();
    l10nJCommander.run(
        "extract-diff",
        "-i",
        getTargetTestDir("extractions").getAbsolutePath(),
        "-o",
        getTargetTestDir("extraction-diffs").getAbsolutePath(),
        "-c",
        "source3",
        "-b",
        "source1",
        "-msab",
        "1");

    Assert.assertEquals(0L, l10nJCommander.getExitCode());
  }

  @Test
  public void testMaxStringsBlockFailure() throws Exception {
    Repository repository = createTestRepoUsingRepoService();

    getL10nJCommander()
        .run(
            "extract",
            "-s",
            getInputResourcesTestDir("source1").getAbsolutePath(),
            "-o",
            getTargetTestDir("extractions").getAbsolutePath(),
            "-n",
            "source1");

    getL10nJCommander()
        .run(
            "extract",
            "-s",
            getInputResourcesTestDir("source3").getAbsolutePath(),
            "-o",
            getTargetTestDir("extractions").getAbsolutePath(),
            "-n",
            "source3");

    getL10nJCommander()
        .run(
            "extract-diff",
            "-i",
            getTargetTestDir("extractions").getAbsolutePath(),
            "-o",
            getTargetTestDir("extraction-diffs").getAbsolutePath(),
            "-c",
            "source3",
            "-b",
            "source1",
            "-p",
            repository.getName(),
            "-pb",
            "branch");

    List<TextUnitDTO> textUnitDTOS = getTextUnitDTOS(repository);
    assertEquals(3L, textUnitDTOS.size());

    assertEquals("1 day update --- 1_day_duration", textUnitDTOS.get(0).getName());
    assertEquals("1 day update", textUnitDTOS.get(0).getSource());
    assertEquals("File lock dialog duration", textUnitDTOS.get(0).getComment());
    assertEquals("LC_MESSAGES/messages.pot", textUnitDTOS.get(0).getAssetPath());

    assertEquals("1 month update --- 1_month_duration", textUnitDTOS.get(1).getName());
    assertEquals("1 month update", textUnitDTOS.get(1).getSource());
    assertEquals("File lock dialog duration", textUnitDTOS.get(1).getComment());
    assertEquals("LC_MESSAGES/messages.pot", textUnitDTOS.get(1).getAssetPath());

    assertEquals("1 year --- 1_year_duration", textUnitDTOS.get(2).getName());
    assertEquals("1 year", textUnitDTOS.get(2).getSource());
    assertEquals("File lock dialog duration", textUnitDTOS.get(2).getComment());
    assertEquals("LC_MESSAGES/messages.pot", textUnitDTOS.get(2).getAssetPath());

    L10nJCommander l10nJCommander = getL10nJCommander();
    l10nJCommander.consoleWriter = Mockito.spy(l10nJCommander.consoleWriter);
    l10nJCommander.run(
        "extract-diff-test",
        "-i",
        getTargetTestDir("extractions").getAbsolutePath(),
        "-o",
        getTargetTestDir("extraction-diffs").getAbsolutePath(),
        "-c",
        "source3",
        "-b",
        "source1",
        "-p",
        repository.getName(),
        "-pb",
        "branch",
        "-snc",
        "CHANNEL_ID",
        "-msab",
        "2",
        "-msrb",
        "2");

    ExtractionDiffCommandForTest extractionDiffCommandForTest =
        l10nJCommander.getCommand(ExtractionDiffCommandForTest.class);
    Optional<SlackNotificationSender> mockedNotificationSender =
        extractionDiffCommandForTest.getMockedNotificationSender();
    assertTrue(mockedNotificationSender.isPresent());
    verify(mockedNotificationSender.get(), times(1))
        .sendMessage(
            "CHANNEL_ID",
            String.format(ExtractionDiffCommand.MAX_STRINGS_ADDED_BLOCK_MESSAGE, "branch", 3, 2));
    Assert.assertEquals(1L, l10nJCommander.getExitCode());
    Mockito.verify(l10nJCommander.consoleWriter, Mockito.times(1))
        .a("There are more than 2 strings added");

    getL10nJCommander()
        .run(
            "extract",
            "-s",
            getInputResourcesTestDir("source1").getAbsolutePath(),
            "-o",
            getTargetTestDir("extractions").getAbsolutePath(),
            "-n",
            "source1");

    getL10nJCommander()
        .run(
            "extract",
            "-s",
            getInputResourcesTestDir("source2").getAbsolutePath(),
            "-o",
            getTargetTestDir("extractions").getAbsolutePath(),
            "-n",
            "source2");

    l10nJCommander = getL10nJCommander();
    l10nJCommander.consoleWriter = Mockito.spy(l10nJCommander.consoleWriter);
    l10nJCommander.run(
        "extract-diff-test",
        "-i",
        getTargetTestDir("extractions").getAbsolutePath(),
        "-o",
        getTargetTestDir("extraction-diffs").getAbsolutePath(),
        "-c",
        "source2",
        "-b",
        "source1",
        "-p",
        repository.getName(),
        "-pb",
        "branch",
        "-snc",
        "CHANNEL_ID",
        "-msab",
        "2",
        "-msrb",
        "2");

    extractionDiffCommandForTest = l10nJCommander.getCommand(ExtractionDiffCommandForTest.class);
    mockedNotificationSender = extractionDiffCommandForTest.getMockedNotificationSender();
    assertTrue(mockedNotificationSender.isPresent());
    verify(mockedNotificationSender.get(), times(1))
        .sendMessage(
            "CHANNEL_ID",
            String.format(ExtractionDiffCommand.MAX_STRINGS_REMOVED_BLOCK_MESSAGE, "branch", 3, 2));
    Assert.assertEquals(1L, l10nJCommander.getExitCode());
    Mockito.verify(l10nJCommander.consoleWriter, Mockito.times(1))
        .a("There are more than 2 strings removed");

    l10nJCommander = getL10nJCommander();
    l10nJCommander.consoleWriter = Mockito.spy(l10nJCommander.consoleWriter);
    l10nJCommander.run(
        "extract-diff-test",
        "-i",
        getTargetTestDir("extractions").getAbsolutePath(),
        "-o",
        getTargetTestDir("extraction-diffs").getAbsolutePath(),
        "-c",
        "source2",
        "-b",
        "source1",
        "-p",
        repository.getName(),
        "-pb",
        "branch",
        "-msab",
        "2",
        "-msrb",
        "2");

    extractionDiffCommandForTest = l10nJCommander.getCommand(ExtractionDiffCommandForTest.class);
    mockedNotificationSender = extractionDiffCommandForTest.getMockedNotificationSender();
    assertFalse(mockedNotificationSender.isPresent());
    Assert.assertEquals(1L, l10nJCommander.getExitCode());
    Mockito.verify(l10nJCommander.consoleWriter, Mockito.times(1))
        .a("There are more than 2 strings removed");
  }

  @Test
  public void testMaxStringsAddedOrRemovedBlockFailure() throws Exception {
    Repository repository = createTestRepoUsingRepoService();

    getL10nJCommander()
        .run(
            "extract",
            "-s",
            getInputResourcesTestDir("source1").getAbsolutePath(),
            "-o",
            getTargetTestDir("extractions").getAbsolutePath(),
            "-n",
            "source1");

    getL10nJCommander()
        .run(
            "extract",
            "-s",
            getInputResourcesTestDir("source3").getAbsolutePath(),
            "-o",
            getTargetTestDir("extractions").getAbsolutePath(),
            "-n",
            "source3");

    getL10nJCommander()
        .run(
            "extract-diff",
            "-i",
            getTargetTestDir("extractions").getAbsolutePath(),
            "-o",
            getTargetTestDir("extraction-diffs").getAbsolutePath(),
            "-c",
            "source3",
            "-b",
            "source1",
            "-p",
            repository.getName(),
            "-pb",
            "branch");
    List<TextUnitDTO> textUnitDTOS = getTextUnitDTOS(repository);
    assertEquals(3L, textUnitDTOS.size());

    assertEquals("1 century --- 1_century_duration", textUnitDTOS.get(0).getName());
    assertEquals("1 century", textUnitDTOS.get(0).getSource());
    assertEquals("File lock dialog duration", textUnitDTOS.get(0).getComment());
    assertEquals("LC_MESSAGES/messages.pot", textUnitDTOS.get(0).getAssetPath());

    assertEquals("1 decade --- 1_decade_duration", textUnitDTOS.get(1).getName());
    assertEquals("1 decade", textUnitDTOS.get(1).getSource());
    assertEquals("File lock dialog duration", textUnitDTOS.get(1).getComment());
    assertEquals("LC_MESSAGES/messages.pot", textUnitDTOS.get(1).getAssetPath());

    assertEquals("1 year --- 1_year_duration", textUnitDTOS.get(2).getName());
    assertEquals("1 year", textUnitDTOS.get(2).getSource());
    assertEquals("File lock dialog duration", textUnitDTOS.get(2).getComment());
    assertEquals("LC_MESSAGES/messages.pot", textUnitDTOS.get(2).getAssetPath());

    L10nJCommander l10nJCommander = getL10nJCommander();
    l10nJCommander.consoleWriter = Mockito.spy(l10nJCommander.consoleWriter);
    l10nJCommander.run(
        "extract-diff",
        "-i",
        getTargetTestDir("extractions").getAbsolutePath(),
        "-o",
        getTargetTestDir("extraction-diffs").getAbsolutePath(),
        "-c",
        "source3",
        "-b",
        "source1",
        "-p",
        repository.getName(),
        "-pb",
        "branch",
        "-msab",
        "2",
        "-msrb",
        "2");
    Assert.assertEquals(1L, l10nJCommander.getExitCode());
    Mockito.verify(l10nJCommander.consoleWriter, Mockito.times(1))
        .a("There are more than 2 strings added");

    getL10nJCommander()
        .run(
            "extract",
            "-s",
            getInputResourcesTestDir("source1").getAbsolutePath(),
            "-o",
            getTargetTestDir("extractions").getAbsolutePath(),
            "-n",
            "source1");

    getL10nJCommander()
        .run(
            "extract",
            "-s",
            getInputResourcesTestDir("source2").getAbsolutePath(),
            "-o",
            getTargetTestDir("extractions").getAbsolutePath(),
            "-n",
            "source2");

    l10nJCommander = getL10nJCommander();
    l10nJCommander.consoleWriter = Mockito.spy(l10nJCommander.consoleWriter);
    l10nJCommander.run(
        "extract-diff",
        "-i",
        getTargetTestDir("extractions").getAbsolutePath(),
        "-o",
        getTargetTestDir("extraction-diffs").getAbsolutePath(),
        "-c",
        "source2",
        "-b",
        "source1",
        "-p",
        repository.getName(),
        "-pb",
        "branch",
        "-msab",
        "2",
        "-msrb",
        "2");

    Assert.assertEquals(1L, l10nJCommander.getExitCode());
    Mockito.verify(l10nJCommander.consoleWriter, Mockito.times(1))
        .a("There are more than 2 strings removed");

    l10nJCommander = getL10nJCommander();
    l10nJCommander.consoleWriter = Mockito.spy(l10nJCommander.consoleWriter);
    l10nJCommander.run(
        "extract-diff",
        "-i",
        getTargetTestDir("extractions").getAbsolutePath(),
        "-o",
        getTargetTestDir("extraction-diffs").getAbsolutePath(),
        "-c",
        "source2",
        "-b",
        "source1",
        "-p",
        repository.getName(),
        "-pb",
        "branch",
        "-msab",
        "2",
        "-msrb",
        "2");

    Assert.assertEquals(1L, l10nJCommander.getExitCode());
    Mockito.verify(l10nJCommander.consoleWriter, Mockito.times(1))
        .a("There are more than 2 strings removed");
  }
}

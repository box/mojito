package com.box.l10n.mojito.cli.command;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

import com.box.l10n.mojito.cli.CLITestBase;
import com.box.l10n.mojito.cli.console.Console;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.rest.client.DropClient;
import com.box.l10n.mojito.rest.client.RepositoryClient;
import com.box.l10n.mojito.rest.entity.Drop;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.OutputCaptureRule;

/**
 * @author wadimw
 */
public class DropCancelCommandTest extends CLITestBase {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(DropCancelCommandTest.class);

  @Autowired DropClient dropClient;

  @Autowired private RepositoryClient repositoryClient;

  @Test
  public void dropCancel() throws Exception {

    Repository repository = createTestRepoUsingRepoService();

    getL10nJCommander()
        .run(
            "push",
            "-r",
            repository.getName(),
            "-s",
            getInputResourcesTestDir("source").getAbsolutePath());

    RepositoryStatusChecker repositoryStatusChecker = new RepositoryStatusChecker();
    waitForCondition(
        "wait for repository stats to show forTranslationCount > 0 before exporting a drop",
        () ->
            repositoryStatusChecker.hasStringsForTranslationsForExportableLocales(
                repositoryClient.getRepositoryById(repository.getId())));

    getL10nJCommander().run("drop-export", "-r", repository.getName());

    final Long dropId = getLastDropIdFromOutput(outputCapture);

    logger.debug("Mocking the console input for drop id: {}", dropId);
    Console mockConsole = mock(Console.class);
    when(mockConsole.readLine(Long.class))
        .thenAnswer(
            new Answer<Long>() {
              @Override
              public Long answer(InvocationOnMock invocation) throws Throwable {
                return getAvailableDropNumberForDropIdFromOutput(dropId);
              }
            });

    L10nJCommander l10nJCommander = getL10nJCommander();

    DropCancelCommand dropCancelCommand = l10nJCommander.getCommand(DropCancelCommand.class);

    dropCancelCommand.console = mockConsole;

    l10nJCommander.run(
        new String[] {"drop-cancel", "-r", repository.getName(), "--number-drop-fetched", "1000"});

    Drop drop = dropClient.getDrops(repository.getId(), null, null, null).getContent().getFirst();

    assertEquals("The Drop should be cancelled", true, drop.getCanceled());
  }

  @Test
  public void dropCancelSpecifiedId() throws Exception {

    Repository repository = createTestRepoUsingRepoService();

    getL10nJCommander()
        .run(
            "push",
            "-r",
            repository.getName(),
            "-s",
            getInputResourcesTestDir("source").getAbsolutePath());

    RepositoryStatusChecker repositoryStatusChecker = new RepositoryStatusChecker();
    waitForCondition(
        "wait for repository stats to show forTranslationCount > 0 before exporting a drop",
        () ->
            repositoryStatusChecker.hasStringsForTranslationsForExportableLocales(
                repositoryClient.getRepositoryById(repository.getId())));

    getL10nJCommander().run("drop-export", "-r", repository.getName());

    final Long dropId = getLastDropIdFromOutput(outputCapture);

    logger.debug("Mocking the console input for drop id: {}", dropId);
    Console mockConsole = mock(Console.class);
    when(mockConsole.readLine(Long.class))
        .thenAnswer(
            new Answer<Long>() {
              @Override
              public Long answer(InvocationOnMock invocation) throws Throwable {
                return getAvailableDropNumberForDropIdFromOutput(dropId);
              }
            });

    L10nJCommander l10nJCommander = getL10nJCommander();

    DropCancelCommand dropCancelCommand = l10nJCommander.getCommand(DropCancelCommand.class);

    dropCancelCommand.console = mockConsole;

    Drop dropBefore =
        dropClient.getDrops(repository.getId(), null, null, null).getContent().getFirst();

    l10nJCommander.run(
        new String[] {
          "drop-cancel", "-r", repository.getName(), "-i", dropBefore.getId().toString()
        });

    Drop dropAfter =
        dropClient.getDrops(repository.getId(), null, null, null).getContent().getFirst();

    assertEquals("The Drop should be cancelled", true, dropAfter.getCanceled());
  }

  public static Long getLastDropIdFromOutput(OutputCaptureRule outputCapture) {
    Pattern compile = Pattern.compile("Drop id: ([\\d]+)");
    Matcher matcher = compile.matcher(outputCapture.toString());
    String dropId = null;
    while (matcher.find()) {
      dropId = matcher.group(1);
    }
    return Long.valueOf(dropId);
  }

  private Long getAvailableDropNumberForDropIdFromOutput(Long dropId) {
    Pattern compile = Pattern.compile("  ([\\d]+) - id: " + dropId + ", name:");
    Matcher matcher = compile.matcher(outputCapture.toString());
    String dropNumber = null;
    while (matcher.find()) {
      dropNumber = matcher.group(1);
    }
    return Long.valueOf(dropNumber);
  }
}

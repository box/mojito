package com.box.l10n.mojito.cli.command;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.box.l10n.mojito.cli.CLITestBase;
import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class RetryCommandTest extends CLITestBase {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(RetryCommandTest.class);

  @Autowired RetryCommand retryCommand;

  RetryCommand retryCommandSpy;

  @Before
  public void beforeTest() {
    retryCommandSpy = spy(retryCommand);
    retryCommandSpy.initialInterval = 1;
  }

  @Test
  public void executeWithNoError() throws Exception {
    retryCommandSpy.originalArgs = Arrays.asList("retry", "-c", "ls", "-al");
    doReturn(0).when(retryCommandSpy).executeCommand(Arrays.asList("ls", "-al"));
    retryCommandSpy.execute();
  }

  @Test
  public void executeWithError() throws Exception {
    List<String> command = Arrays.asList("ls", "-al");

    doReturn(1).when(retryCommandSpy).executeCommand(command);
    retryCommandSpy.originalArgs = Arrays.asList("retry", "-c", "ls", "-al");

    try {
      retryCommandSpy.execute();
    } catch (CommandWithExitStatusException cwese) {
      assertEquals(1, cwese.getExitCode());
    }

    verify(retryCommandSpy, times(3)).executeCommand(command);
  }

  @Test
  public void executeWithSuccesfulRetry() throws Exception {
    List<String> command = Arrays.asList("ls", "-al");

    doReturn(1).doReturn(0).when(retryCommandSpy).executeCommand(command);
    retryCommandSpy.originalArgs = Arrays.asList("retry", "-c", "ls", "-al");

    retryCommandSpy.execute();

    verify(retryCommandSpy, times(2)).executeCommand(command);
  }

  @Test
  public void getIndexOfHelpParameter() {
    retryCommandSpy.originalArgs = Arrays.asList("retry", "-h");
    assertEquals(1, retryCommandSpy.getIndexOfHelpParameter());
  }

  @Test
  public void getIndexOfCommandParameter() {
    retryCommandSpy.originalArgs = Arrays.asList("retry", "-d", "tmp", "--command");
    assertEquals(3, retryCommandSpy.getIndexOfCommandParameter());
  }

  @Test
  public void getCommandToExecute() {
    retryCommandSpy.originalArgs = Arrays.asList("retry", "-d", "tmp", "-c", "ls", "-al");
    assertEquals(Arrays.asList("ls", "-al"), retryCommandSpy.getCommandToExecute());
  }

  @Test
  public void shouldShowUsageHelp() {
    retryCommandSpy.originalArgs = Arrays.asList("retry", "-h", "-d", "tmp", "-c", "ls", "-al");
    assertTrue(retryCommandSpy.shouldShowUsage());
  }

  @Test
  public void shouldShowUsageHelpMissingCommand() {
    retryCommandSpy.originalArgs = Arrays.asList("retry", "-h", "-d", "tmp", "ls", "-al");
    assertTrue(retryCommandSpy.shouldShowUsage());
  }

  @Test
  public void shouldShowUsageHelpNo() {
    retryCommandSpy.originalArgs = Arrays.asList("retry", "-d", "tmp", "-c", "ls", "-al");
    assertFalse(retryCommandSpy.shouldShowUsage());
  }
}

package com.box.l10n.mojito.cli.command;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.box.l10n.mojito.cli.CLITestBase;
import com.box.l10n.mojito.slack.SlackClient;
import com.box.l10n.mojito.slack.SlackClientException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatusCode;
import org.springframework.security.web.authentication.session.SessionAuthenticationException;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;

public class L10nJCommanderTest extends CLITestBase {
  SlackClient slackClientMock;

  Command commandMock;

  @Before
  public void before() {
    this.slackClientMock = Mockito.mock(SlackClient.class);
    this.commandMock = Mockito.mock(Command.class);
  }

  private L10nJCommander getL10nJCommanderSpy() {
    Mockito.reset(this.slackClientMock);
    Mockito.reset(this.commandMock);
    when(this.commandMock.getName()).thenReturn("repo-create");
    when(this.commandMock.getFailureSlackNotificationChannel()).thenReturn("@testslackchannel");
    L10nJCommander commander = Mockito.spy(this.getL10nJCommander());
    when(commander.getCommand(anyString())).thenReturn(this.commandMock);
    commander.slackClient = this.slackClientMock;
    return commander;
  }

  @Test
  public void testRunSlackClientIsCalled() throws SlackClientException {
    String[] args =
        new String[] {
          "repo-create", "-n", "test_repo", "-l", "ar-SA", "-fsnc", "@testslackchannel"
        };

    L10nJCommander commander = this.getL10nJCommanderSpy();
    doThrow(new SessionAuthenticationException("error message")).when(this.commandMock).run();
    commander.run(args);

    verify(this.slackClientMock, times(1)).sendInstantMessage(any());
    assertEquals(1, commander.getExitCode());

    commander = this.getL10nJCommanderSpy();
    doThrow(new CommandWithExitStatusException(2)).when(this.commandMock).run();
    commander.run(args);

    verify(this.slackClientMock, times(1)).sendInstantMessage(any());
    assertEquals(2, commander.getExitCode());

    commander = this.getL10nJCommanderSpy();
    doThrow(new CommandException("error message")).when(this.commandMock).run();
    commander.run(args);

    verify(this.slackClientMock, times(1)).sendInstantMessage(any());
    assertEquals(1, commander.getExitCode());

    commander = this.getL10nJCommanderSpy();
    doThrow(new ResourceAccessException("error message")).when(this.commandMock).run();
    commander.run(args);

    verify(this.slackClientMock, times(1)).sendInstantMessage(any());
    assertEquals(1, commander.getExitCode());

    commander = this.getL10nJCommanderSpy();
    doThrow(new HttpClientErrorException(HttpStatusCode.valueOf(500))).when(this.commandMock).run();
    commander.run(args);

    verify(this.slackClientMock, times(1)).sendInstantMessage(any());
    assertEquals(1, commander.getExitCode());

    commander = this.getL10nJCommanderSpy();
    doThrow(new ArrayIndexOutOfBoundsException()).when(this.commandMock).run();
    commander.run(args);

    verify(this.slackClientMock, times(1)).sendInstantMessage(any());
    assertEquals(1, commander.getExitCode());
  }

  @Test
  public void testRunSlackClientIsNotCalled() throws SlackClientException {
    L10nJCommander commander = this.getL10nJCommanderSpy();
    doThrow(new CommandException("error message", false)).when(this.commandMock).run();
    commander.run("repo-create", "-n", "test_repo", "-l", "ar-SA", "-fsnc", "@testslackchannel");

    verify(this.slackClientMock, times(0)).sendInstantMessage(any());
    assertEquals(1, commander.getExitCode());

    commander = this.getL10nJCommanderSpy();
    doThrow(new ResourceAccessException("error message")).when(this.commandMock).run();
    when(this.commandMock.getFailureSlackNotificationChannel()).thenReturn(null);
    commander.run("repo-create", "-n", "test_repo", "-l", "ar-SA");

    verify(this.slackClientMock, times(0)).sendInstantMessage(any());
    assertEquals(1, commander.getExitCode());

    commander = this.getL10nJCommanderSpy();
    doThrow(new ResourceAccessException("error message")).when(this.commandMock).run();
    commander.slackClient = null;
    commander.run("repo-create", "-n", "test_repo", "-l", "ar-SA", "-fsnc", "@testslackchannel");

    verify(this.slackClientMock, times(0)).sendInstantMessage(any());
    assertEquals(1, commander.getExitCode());
  }
}

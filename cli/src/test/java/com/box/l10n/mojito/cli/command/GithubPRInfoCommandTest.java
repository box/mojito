package com.box.l10n.mojito.cli.command;

import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.box.l10n.mojito.cli.console.ConsoleWriter;
import com.box.l10n.mojito.github.GithubClient;
import com.box.l10n.mojito.github.GithubClients;
import com.google.common.collect.Lists;
import java.io.IOException;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.kohsuke.github.GHIssueComment;
import org.kohsuke.github.ReactionContent;
import org.mockito.Mockito;

public class GithubPRInfoCommandTest {

  GithubClients githubClientsMock;

  GithubClient githubMock;

  ConsoleWriter consoleWriterMock;

  GHIssueComment ghIssueCommentMock;

  GithubPRInfoCommand githubPRInfoCommand;

  @Before
  public void setup() {
    githubMock = Mockito.mock(GithubClient.class);
    consoleWriterMock = Mockito.mock(ConsoleWriter.class);
    ghIssueCommentMock = Mockito.mock(GHIssueComment.class);
    githubClientsMock = Mockito.mock(GithubClients.class);
    githubPRInfoCommand = new GithubPRInfoCommand();
    githubPRInfoCommand.repository = "testRepo";
    githubPRInfoCommand.owner = "testOwner";
    githubPRInfoCommand.prNumber = 1;
    githubPRInfoCommand.githubClients = githubClientsMock;
    githubPRInfoCommand.consoleWriterAnsiCodeEnabledFalse = consoleWriterMock;

    when(githubMock.getPRBaseCommit("testRepo", 1)).thenReturn("baseSha");
    when(githubMock.getPRAuthorEmail("testRepo", 1)).thenReturn("some@email.com");
    when(githubClientsMock.getClient(isA(String.class))).thenReturn(githubMock);
    List<GHIssueComment> mockComments = Lists.newArrayList(ghIssueCommentMock);
    when(ghIssueCommentMock.getBody()).thenReturn("some comment");
    when(githubMock.getPRComments("testRepo", 1)).thenReturn(mockComments);
    when(consoleWriterMock.a(isA(String.class))).thenReturn(consoleWriterMock);
    when(consoleWriterMock.println()).thenReturn(consoleWriterMock);
  }

  @Test
  public void testExecute() {
    githubPRInfoCommand.execute();
    verify(consoleWriterMock, times(1)).a("MOJTIO_GITHUB_BASE_COMMIT=");
    verify(consoleWriterMock, times(1)).a("baseSha");
    verify(consoleWriterMock, times(1)).a("MOJITO_GITHUB_AUTHOR_EMAIL=");
    verify(consoleWriterMock, times(1)).a("some@email.com");
    verify(consoleWriterMock, times(1)).a("MOJITO_GITHUB_AUTHOR_USERNAME=");
    verify(consoleWriterMock, times(1)).a("some");
    verify(consoleWriterMock, times(1)).a("MOJITO_SKIP_I18N_CHECKS=false");
  }

  @Test
  public void testExecuteWithChecksSkipped() throws IOException {
    when(ghIssueCommentMock.getBody()).thenReturn("SKIP_I18N_CHECKS");
    githubPRInfoCommand.execute();
    verify(consoleWriterMock, times(1)).a("MOJTIO_GITHUB_BASE_COMMIT=");
    verify(consoleWriterMock, times(1)).a("baseSha");
    verify(consoleWriterMock, times(1)).a("MOJITO_GITHUB_AUTHOR_EMAIL=");
    verify(consoleWriterMock, times(1)).a("some@email.com");
    verify(consoleWriterMock, times(1)).a("MOJITO_GITHUB_AUTHOR_USERNAME=");
    verify(consoleWriterMock, times(1)).a("some");
    verify(consoleWriterMock, times(1)).a("MOJITO_SKIP_I18N_CHECKS=true");
    verify(ghIssueCommentMock, times(1)).createReaction(ReactionContent.PLUS_ONE);
  }

  @Test(expected = CommandException.class)
  public void testGithubClientNotAvailable() {
    when(githubClientsMock.getClient(isA(String.class))).thenReturn(null);
    githubPRInfoCommand.execute();
  }
}

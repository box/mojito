package com.box.l10n.mojito.cli.command;

import static com.box.l10n.mojito.cli.command.utils.DiffInfoUtils.getUsernameForAuthorEmail;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.box.l10n.mojito.cli.console.ConsoleWriter;
import com.box.l10n.mojito.github.GithubClient;
import com.box.l10n.mojito.github.GithubClientsFactory;
import com.box.l10n.mojito.github.GithubException;
import java.io.IOException;
import java.util.List;
import org.kohsuke.github.GHIssueComment;
import org.kohsuke.github.ReactionContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Command to export Github Pull Request info to a list of environment variables
 *
 * @author maallen
 */
@Component
@Scope("prototype")
@Parameters(
    commandNames = {"github-pr-to-env-variables"},
    commandDescription = "Get PR info: base commit")
public class GithubPRInfoCommand extends Command {

  static Logger logger = LoggerFactory.getLogger(GithubPRInfoCommand.class);

  protected static final String SKIP_I18N_CHECKS_FLAG = "SKIP_I18N_CHECKS";

  @Qualifier("ansiCodeEnabledFalse")
  @Autowired
  ConsoleWriter consoleWriterAnsiCodeEnabledFalse;

  @Autowired(required = false)
  GithubClientsFactory githubClientsFactory;

  @Parameter(
      names = {"--repository"},
      arity = 1,
      required = true,
      description = "Github repository name")
  String repository;

  @Parameter(
      names = {"--pr-number"},
      arity = 1,
      required = true,
      description = "The Github PR number")
  Integer prNumber;

  @Parameter(
      names = {"--github-repo-owner"},
      arity = 1,
      required = true,
      description = "The Github repository owner")
  String owner;

  @Override
  public void execute() throws CommandException {

    if (githubClientsFactory == null) {
      throw new CommandException(
          "Github must be configured with properties: l10n.githubClients.<client>.appId, l10n.githubClients.<client>.key and l10n.githubClients.<client>.owner");
    }

    try {
      GithubClient github = githubClientsFactory.getClient(owner);
      if (github == null) {
        throw new CommandException(
            String.format("Github client with owner '%s' not found.", owner));
      }
      consoleWriterAnsiCodeEnabledFalse
          .a("MOJTIO_GITHUB_BASE_COMMIT=")
          .a(github.getPRBaseCommit(repository, prNumber))
          .println();
      String authorEmail = github.getPRAuthorEmail(repository, prNumber);
      consoleWriterAnsiCodeEnabledFalse.a("MOJITO_GITHUB_AUTHOR_EMAIL=").a(authorEmail).println();
      consoleWriterAnsiCodeEnabledFalse
          .a("MOJITO_GITHUB_AUTHOR_USERNAME=")
          .a(getUsernameForAuthorEmail(authorEmail))
          .println();
      List<GHIssueComment> prComments = github.getPRComments(repository, prNumber);
      if (isSkipChecks(prComments)) {
        addReactionToSkipChecksComment(prComments);
        consoleWriterAnsiCodeEnabledFalse.a("MOJITO_SKIP_I18N_CHECKS=true").println();
      } else {
        consoleWriterAnsiCodeEnabledFalse.a("MOJITO_SKIP_I18N_CHECKS=false").println();
      }
    } catch (GithubException e) {
      throw new CommandException(e);
    }
  }

  private static boolean isSkipChecks(List<GHIssueComment> prComments) {
    return prComments.stream()
        .anyMatch(ghIssueComment -> ghIssueComment.getBody().contains(SKIP_I18N_CHECKS_FLAG));
  }

  private static void addReactionToSkipChecksComment(List<GHIssueComment> prComments) {
    prComments.stream()
        .filter(ghIssueComment -> ghIssueComment.getBody().contains(SKIP_I18N_CHECKS_FLAG))
        .findFirst()
        .map(ghIssueComment -> addReactionToComment(ghIssueComment));
  }

  private static GHIssueComment addReactionToComment(GHIssueComment ghIssueComment) {
    try {
      ghIssueComment.createReaction(ReactionContent.PLUS_ONE);
      return ghIssueComment;
    } catch (IOException e) {
      logger.error("Error adding reaction to PR comment: " + e.getMessage());
    }
    return ghIssueComment;
  }
}

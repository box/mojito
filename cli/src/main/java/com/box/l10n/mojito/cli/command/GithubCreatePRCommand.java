package com.box.l10n.mojito.cli.command;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.box.l10n.mojito.cli.console.ConsoleWriter;
import com.box.l10n.mojito.github.GithubClient;
import com.box.l10n.mojito.github.GithubClients;
import com.box.l10n.mojito.github.GithubException;
import java.util.List;
import org.fusesource.jansi.Ansi;
import org.kohsuke.github.GHPullRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
@Parameters(
    commandNames = {"github-pr-create"},
    commandDescription = "Create a Github PR")
public class GithubCreatePRCommand extends Command {

  @Autowired GithubClients githubClients;

  @Qualifier("ansiCodeEnabledFalse")
  @Autowired
  ConsoleWriter consoleWriter;

  @Parameter(
      names = {"--owner", "-o"},
      required = true,
      arity = 1,
      description = "The Github repository owner")
  String owner;

  @Parameter(
      names = {"--repository", "-r"},
      required = true,
      arity = 1,
      description = "The Github repository name")
  String repository;

  @Parameter(
      names = {"--title"},
      required = true,
      arity = 1,
      description = "The PR title")
  String title;

  @Parameter(
      names = {"--head"},
      required = true,
      arity = 1,
      description = "The PR head")
  String head;

  @Parameter(
      names = {"--base"},
      required = true,
      arity = 1,
      description = "The PR base")
  String base;

  @Parameter(
      names = {"--body"},
      required = false,
      arity = 1,
      description = "The PR body")
  String body;

  @Parameter(
      names = {"--reviewers"},
      required = false,
      variableArity = true,
      description = "The PR reviewers")
  List<String> reviewers;

  @Parameter(
      names = {"--enable-auto-merge"},
      required = false,
      arity = 1,
      description = "Enable auto-merge with the specified method")
  EnableAutoMergeType enableAutoMerge = EnableAutoMergeType.NONE;

  @Parameter(
      names = {"--labels"},
      required = false,
      variableArity = true,
      description = "The PR labels")
  List<String> labels;

  enum EnableAutoMergeType {
    SQUASH,
    MERGE,
    NONE
  }

  @Override
  public boolean shouldShowInCommandList() {
    return false;
  }

  @Override
  protected void execute() throws CommandException {
    try {

      GithubClient githubClient = githubClients.getClient(owner);

      GHPullRequest pr = githubClient.createPR(repository, title, head, base, body, reviewers);

      consoleWriter.a("PR created: ").fg(Ansi.Color.CYAN).a(pr.getHtmlUrl().toString()).println();
      if (!EnableAutoMergeType.NONE.equals(enableAutoMerge)) {
        githubClient.enableAutoMerge(pr, GithubClient.AutoMergeType.SQUASH);
      }

      githubClient.addLabelsToPR(pr, labels);

    } catch (GithubException e) {
      throw new CommandException(e);
    }
  }
}

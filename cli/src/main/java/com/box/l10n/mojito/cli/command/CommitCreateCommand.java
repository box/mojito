package com.box.l10n.mojito.cli.command;

import static java.util.Optional.ofNullable;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.box.l10n.mojito.JSR310Migration;
import com.box.l10n.mojito.cli.apiclient.CommitWsApi;
import com.box.l10n.mojito.cli.command.param.Param;
import com.box.l10n.mojito.cli.console.ConsoleWriter;
import com.box.l10n.mojito.cli.model.CommitBody;
import com.box.l10n.mojito.cli.model.CommitCommit;
import com.box.l10n.mojito.cli.model.RepositoryRepository;
import com.google.common.collect.Streams;
import java.io.IOException;
import java.time.ZonedDateTime;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.fusesource.jansi.Ansi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * @author garion
 */
@Component
@Scope("prototype")
@Parameters(
    commandNames = {"commit-create", "cc"},
    commandDescription = "Create commit information in Mojito.")
public class CommitCreateCommand extends Command {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(CommitCreateCommand.class);

  @Autowired ConsoleWriter consoleWriter;

  @Autowired CommandHelper commandHelper;

  @Autowired CommitWsApi commitClient;

  @Parameter(
      names = {Param.COMMIT_HASH_LONG, Param.COMMIT_HASH_SHORT},
      arity = 1,
      description = Param.COMMIT_CREATE_DESCRIPTION)
  String commitHash;

  @Parameter(
      names = {Param.REPOSITORY_LONG, Param.REPOSITORY_SHORT},
      arity = 1,
      required = true,
      description = Param.REPOSITORY_DESCRIPTION)
  String repositoryParam;

  @Parameter(
      names = {Param.AUTHOR_NAME_LONG, Param.AUTHOR_NAME_SHORT},
      arity = 1,
      description = Param.AUTHOR_NAME_DESCRIPTION)
  String authorNameParam;

  @Parameter(
      names = {Param.AUTHOR_EMAIL_LONG, Param.AUTHOR_EMAIL_SHORT},
      arity = 1,
      description = Param.AUTHOR_EMAIL_DESCRIPTION)
  String authorEmailParam;

  @Parameter(
      names = {Param.CREATION_DATE_LONG, Param.CREATION_DATE_SHORT},
      arity = 1,
      description = Param.CREATION_DATE_DESCRIPTION,
      converter = DateTimeConverter.class)
  ZonedDateTime creationDateParam;

  @Parameter(
      names = {"--read-from-git"},
      description =
          "To read the commit information (author name, author email, and commit date) from git instead "
              + "of passing them in as command arguments. '--commit-hash' is optional. If not provided"
              + " the first commit from 'git log' is used.")
  boolean readInfoFromGit = false;

  @Override
  protected void execute() throws CommandException {
    consoleWriter
        .newLine()
        .a("Store single commit information for repository: ")
        .fg(Ansi.Color.CYAN)
        .a(repositoryParam)
        .println(2);

    RepositoryRepository repository = commandHelper.findRepositoryByName(repositoryParam);

    final CommitInfo commitInfo;

    if (readInfoFromGit) {
      commitInfo = readFromGit();
    } else {
      commitInfo = new CommitInfo(commitHash, authorEmailParam, authorNameParam, creationDateParam);
    }

    CommitBody commitBody = new CommitBody();
    commitBody.setCommitName(commitInfo.hash);
    commitBody.setRepositoryId(repository.getId());
    commitBody.setAuthorName(commitInfo.authorName);
    commitBody.setAuthorEmail(commitInfo.authorEmail);
    Long creationDateMilliSeconds =
        ofNullable(commitInfo.creationDate).map(JSR310Migration::getMillis).orElse(null);
    commitBody.setSourceCreationDate(creationDateMilliSeconds);
    CommitCommit commit = commitClient.createCommit(commitBody);

    consoleWriter
        .fg(Ansi.Color.GREEN)
        .newLine()
        .a("Finished. Stored in the database with commit ID: ")
        .fg(Ansi.Color.CYAN)
        .a(commit.getId())
        .println(2);
  }

  CommitInfo readFromGit() {
    final CommandDirectories commandDirectories = new CommandDirectories(null);
    final GitRepository gitRepository = new GitRepository();

    gitRepository.init(commandDirectories.getSourceDirectoryPath().toString());

    try {
      final RevCommit revCommit;

      if (commitHash != null) {
        RevWalk walk = new RevWalk(gitRepository.jgitRepository);
        ObjectId commitObjectId = gitRepository.jgitRepository.resolve(commitHash);
        if (commitObjectId == null) {
          throw new CommandException(
              "The provided commit hash: '" + commitHash + "' cannot be resolved");
        }
        revCommit = walk.parseCommit(commitObjectId);
      } else {
        revCommit =
            Streams.stream(new Git(gitRepository.jgitRepository).log().setMaxCount(1).call())
                .findFirst()
                .orElseThrow(() -> new CommandException("There is no commits in the log"));
      }

      final PersonIdent authorIdent = revCommit.getAuthorIdent();

      final CommitInfo commitInfo =
          new CommitInfo(
              revCommit.getName(),
              authorIdent.getEmailAddress(),
              authorIdent.getName(),
              JSR310Migration.dateTimeOfEpochSecond(revCommit.getCommitTime()));

      consoleWriter
          .a("Read from Git - hash: '")
          .a(commitInfo.hash)
          .a("', author email: '")
          .a(commitInfo.authorEmail)
          .a("', author name: '")
          .a(commitInfo.authorName)
          .a("', date: '")
          .a(commitInfo.creationDate)
          .println();

      return commitInfo;

    } catch (IOException | GitAPIException ioException) {
      throw new CommandException("Can't retreive information from Git", ioException);
    }
  }

  static final class CommitInfo {

    String hash;
    String authorEmail;
    String authorName;
    ZonedDateTime creationDate;

    public CommitInfo(
        String hash, String authorEmail, String authorName, ZonedDateTime creationDate) {
      this.hash = hash;
      this.authorEmail = authorEmail;
      this.authorName = authorName;
      this.creationDate = creationDate;
    }
  }
}

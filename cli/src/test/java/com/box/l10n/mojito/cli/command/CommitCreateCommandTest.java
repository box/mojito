package com.box.l10n.mojito.cli.command;

import static org.junit.Assert.assertEquals;

import com.box.l10n.mojito.cli.CLITestBase;
import com.box.l10n.mojito.cli.command.param.Param;
import com.box.l10n.mojito.entity.Commit;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.service.commit.CommitService;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.joda.time.DateTime;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;

/** @author garion */
public class CommitCreateCommandTest extends CLITestBase {

  @Autowired CommitService commitService;

  @Test
  public void testCommitCreate() throws Exception {
    Repository repository = createTestRepoUsingRepoService();

    String commitHash = "ABC123";
    String authorEmail = "authorEmail";
    String authorName = "authorName";
    DateTime creationDate = DateTime.now();

    L10nJCommander l10nJCommander = getL10nJCommander();
    l10nJCommander.run(
        "commit-create",
        "-r",
        repository.getName(),
        Param.COMMIT_HASH_LONG,
        commitHash,
        Param.AUTHOR_EMAIL_LONG,
        authorEmail,
        Param.AUTHOR_NAME_LONG,
        authorName,
        Param.CREATION_DATE_LONG,
        creationDate.toString());
    assertEquals(0, l10nJCommander.getExitCode());
    Commit createdCommit =
        commitService
            .getCommits(
                repository.getId(),
                Collections.singletonList(commitHash),
                null,
                null,
                null,
                null,
                Pageable.unpaged())
            .stream()
            .findFirst()
            .get();

    assertEquals(commitHash, createdCommit.getName());
    assertEquals(authorEmail, createdCommit.getAuthorEmail());
    assertEquals(authorName, createdCommit.getAuthorName());
    assertEquals(creationDate.withMillisOfSecond(0).getMillis(), createdCommit.getSourceCreationDate().withMillisOfSecond(0).getMillis());
  }

  @Test
  public void testCommitCreateIsIdempotent() throws Exception {
    Repository repository = createTestRepoUsingRepoService();

    String commitHash = "ABC456";
    String authorEmail = "authorEmail";
    String authorName = "authorName";
    DateTime creationDate = DateTime.now();

    L10nJCommander l10nJCommanderFirstRun = getL10nJCommander();
    l10nJCommanderFirstRun.run(
        "commit-create",
        "-r",
        repository.getName(),
        Param.COMMIT_HASH_LONG,
        commitHash,
        Param.AUTHOR_EMAIL_LONG,
        authorEmail,
        Param.AUTHOR_NAME_LONG,
        authorName,
        Param.CREATION_DATE_LONG,
        creationDate.toString());
    assertEquals(0, l10nJCommanderFirstRun.getExitCode());

    L10nJCommander l10nJCommanderSecondRun = getL10nJCommander();
    l10nJCommanderSecondRun.run(
        "commit-create",
        "-r",
        repository.getName(),
        Param.COMMIT_HASH_LONG,
        commitHash,
        Param.AUTHOR_EMAIL_LONG,
        authorEmail,
        Param.AUTHOR_NAME_LONG,
        authorName,
        Param.CREATION_DATE_LONG,
        creationDate.toString());
    assertEquals(0, l10nJCommanderSecondRun.getExitCode());

    List<Commit> commits =
        commitService
            .getCommits(
                repository.getId(),
                Collections.singletonList(commitHash),
                null,
                null,
                null,
                null,
                Pageable.unpaged())
            .stream()
            .collect(Collectors.toList());
    assertEquals(1, commits.size());
    Commit createdCommit = commits.stream().findFirst().get();

    assertEquals(commitHash, createdCommit.getName());
    assertEquals(authorEmail, createdCommit.getAuthorEmail());
    assertEquals(authorName, createdCommit.getAuthorName());
    assertEquals(creationDate.withMillisOfSecond(0).getMillis(), createdCommit.getSourceCreationDate().withMillisOfSecond(0).getMillis());
  }
}

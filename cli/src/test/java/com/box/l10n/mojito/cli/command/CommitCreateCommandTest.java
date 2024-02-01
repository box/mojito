package com.box.l10n.mojito.cli.command;

import static org.junit.Assert.assertEquals;

import com.box.l10n.mojito.JSR310Migration;
import com.box.l10n.mojito.cli.CLITestBase;
import com.box.l10n.mojito.cli.command.param.Param;
import com.box.l10n.mojito.entity.Commit;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.service.commit.CommitService;
import com.google.common.collect.Streams;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.Assume;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * @author garion
 */
public class CommitCreateCommandTest extends CLITestBase {

  @Autowired CommitService commitService;

  @Test
  public void testCommitCreate() throws Exception {
    Repository repository = createTestRepoUsingRepoService();

    String commitHash = "ABC123";
    String authorEmail = "authorEmail";
    String authorName = "authorName";
    ZonedDateTime creationDate = ZonedDateTime.now();

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
    // That test seems brittle because of different rounding / truncating?
    assertEquals(
        JSR310Migration.dateTimeWith0MillisAsMillis(creationDate),
        JSR310Migration.dateTimeWith0MillisAsMillis(createdCommit.getSourceCreationDate()));
  }

  @Test
  public void testCommitCreateIsIdempotent() throws Exception {
    Repository repository = createTestRepoUsingRepoService();

    String commitHash = "ABC456";
    String authorEmail = "authorEmail";
    String authorName = "authorName";
    ZonedDateTime creationDate = ZonedDateTime.now();

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
    assertEquals(
        JSR310Migration.dateTimeWith0MillisAsMillis(creationDate),
        JSR310Migration.dateTimeWith0MillisAsMillis(createdCommit.getSourceCreationDate()));
  }

  @Test
  public void testReadFromGitWithHash() throws Exception {
    // shallow clone, etc won't work for this test. Skip it on CI
    Assume.assumeFalse(isGitActions());

    Repository repository = createTestRepoUsingRepoService();

    L10nJCommander l10nJCommanderFirstRun = getL10nJCommander();
    l10nJCommanderFirstRun.run(
        "commit-create", "-r", repository.getName(), "--read-from-git", "--commit-hash", "104e24");

    final Page<Commit> commits =
        commitService.getCommits(
            repository.getId(), null, null, null, null, null, Pageable.unpaged());

    assertEquals(1, commits.getTotalElements());
    final Commit commit = commits.get().findFirst().get();
    assertEquals("104e243819a3d7ba5fe25d069d6b51c8f0f3b2c2", commit.getName());
    assertEquals("Jean Aurambault", commit.getAuthorName());
    assertEquals("aurambaj@users.noreply.github.com", commit.getAuthorEmail());
    assertEquals(
        ZonedDateTime.parse("2022-09-20T10:55:39.000-07:00").toInstant(),
        commit.getSourceCreationDate().toInstant());
  }

  @Test
  public void testReadGitInfoNoHash() throws Exception {
    Repository repository = createTestRepoUsingRepoService();

    L10nJCommander l10nJCommanderFirstRun = getL10nJCommander();
    l10nJCommanderFirstRun.run("commit-create", "-r", repository.getName(), "--read-from-git");

    final Page<Commit> commits =
        commitService.getCommits(
            repository.getId(), null, null, null, null, null, Pageable.unpaged());

    assertEquals(1, commits.getTotalElements());
    final Commit commit = commits.get().findFirst().get();

    final GitRepository gitRepository = new GitRepository();
    gitRepository.init(getInputResourcesTestDir().toString());
    final RevCommit revCommit =
        Streams.stream(new Git(gitRepository.jgitRepository).log().setMaxCount(1).call())
            .findFirst()
            .orElseThrow(() -> new RuntimeException("test must be run on the mojito git repo"));

    assertEquals(revCommit.getName(), commit.getName());
  }
}

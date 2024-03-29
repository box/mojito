package com.box.l10n.mojito.cli.command;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

import com.box.l10n.mojito.cli.CLITestBase;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.rest.client.RepositoryClient;
import com.box.l10n.mojito.rest.entity.Branch;
import com.box.l10n.mojito.service.branch.BranchRepository;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class BranchDeleteCommandTest extends CLITestBase {

  @Autowired RepositoryClient repositoryClient;

  @Autowired BranchRepository branchRepository;

  @Test
  public void delete() throws Exception {
    Repository repository = createTestRepoUsingRepoService();

    getL10nJCommander()
        .run(
            "push",
            "-r",
            repository.getName(),
            "-s",
            getInputResourcesTestDir("source").getAbsolutePath());

    checkBranches("", repository, null, null, (String) null);

    getL10nJCommander().run("branch-view", "-r", repository.getName());

    Stream.of("master", "b1", "b2", "b3")
        .forEach(
            branchName -> {
              getL10nJCommander()
                  .run(
                      "push",
                      "-r",
                      repository.getName(),
                      "-s",
                      getInputResourcesTestDir("source").getAbsolutePath(),
                      "-b",
                      branchName);
            });

    getL10nJCommander()
        .run(
            "push",
            "-r",
            repository.getName(),
            "-s",
            getInputResourcesTestDir("b4Source").getAbsolutePath(),
            "-b",
            "b4");

    getL10nJCommander().run("branch-view", "-r", repository.getName());

    // master and null not processed for branch statistic see {@link
    // BranchStatisticService#getBranchesToProcess}
    waitForCondition(
        "b1, b2, b3, b4 should become untranslated when stats are computed",
        () -> {
          List<String> branches =
              repositoryClient
                  .getBranches(repository.getId(), null, null, null, false, true, null)
                  .stream()
                  .map(Branch::getName)
                  .sorted()
                  .collect(Collectors.toList());

          return branches.equals(asList("b1", "b2", "b3", "b4"));
        });

    getL10nJCommander()
        .run(
            "import",
            "-r",
            repository.getName(),
            "-s",
            getInputResourcesTestDir("b4Source").getAbsolutePath(),
            "-t",
            getInputResourcesTestDir("b4Translations").getAbsolutePath());

    waitForCondition(
        "All b4 translated other branches should still not be translated",
        () -> {
          List<String> branchesTranslated =
              repositoryClient
                  .getBranches(repository.getId(), null, null, null, true, false, null)
                  .stream()
                  .map(Branch::getName)
                  .sorted()
                  .collect(Collectors.toList());

          List<String> branchesUntransaslted =
              repositoryClient
                  .getBranches(repository.getId(), null, null, null, false, false, null)
                  .stream()
                  .map(Branch::getName)
                  .sorted()
                  .collect(Collectors.toList());
          return branchesTranslated.equals(asList("b4"))
              && branchesUntransaslted.equals(asList("b1", "b2", "b3"));
        });

    getL10nJCommander().run("branch-view", "-r", repository.getName());

    com.box.l10n.mojito.entity.Branch master =
        branchRepository.findByNameAndRepository("master", repository);
    master.setCreatedDate(ZonedDateTime.now().minusDays(14));
    branchRepository.save(master);

    checkBranches(
        "There should be only master that is older than a week",
        repository,
        ZonedDateTime.now().minusDays(7),
        null,
        "master");

    checkBranches(
        "This should show all the branches",
        repository,
        null,
        null,
        (String) null,
        "master",
        "b1",
        "b2",
        "b3",
        "b4");

    getL10nJCommander().run("branch-delete", "-r", repository.getName(), "-nr", "(b1|b2)", "-cblw");
    checkBranches(
        "Should not delete anything since b1 or b2 are not a week old",
        repository,
        null,
        null,
        (String) null,
        "master",
        "b1",
        "b2",
        "b3",
        "b4");

    getL10nJCommander().run("branch-delete", "-r", repository.getName(), "-nr", "(b1|b2)");
    checkBranches(
        "Should delete b1 and b2", repository, null, null, (String) null, "master", "b3", "b4");

    getL10nJCommander().run("branch-delete", "-r", repository.getName(), "-nr", "(b1|b2)", "-nb");
    checkBranches("", repository, null, null, "master", "b3", "b4");

    getL10nJCommander().run("branch-delete", "-r", repository.getName(), "-nr", "(?!^master$).*");
    checkBranches("", repository, null, null, "master");
  }

  void checkBranches(
      String message,
      Repository repository,
      ZonedDateTime createdBefore,
      Boolean translated,
      String... branchNames) {

    List<String> expectedBranchNames =
        branchNames == null
            ? Collections.emptyList()
            : Stream.of(branchNames)
                .sorted(Comparator.nullsFirst(Comparator.naturalOrder()))
                .collect(Collectors.toList());

    List<String> actual =
        repositoryClient
            .getBranches(repository.getId(), null, null, false, translated, true, createdBefore)
            .stream()
            .map(Branch::getName)
            .sorted(Comparator.nullsFirst(Comparator.naturalOrder()))
            .collect(Collectors.toList());

    assertEquals(message, expectedBranchNames, actual);
  }
}

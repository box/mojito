package com.box.l10n.mojito.cli.command;

import static com.box.l10n.mojito.cli.command.param.Param.BRANCH_CREATED_BEFORE;
import static com.box.l10n.mojito.cli.command.param.Param.BRANCH_CREATED_BEFORE_LAST_WEEK_LONG;
import static com.box.l10n.mojito.cli.command.param.Param.BRANCH_CREATED_BEFORE_OPTIONS_AND_EXAMPLE;
import static com.box.l10n.mojito.cli.command.param.Param.BRANCH_CREATED_BEFORE_SHORT;
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
import org.mockito.Mockito;
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

  @Test
  public void testCreatedBeforeParameter() throws Exception {
    final String masterBranchName = "master";
    final String pushCommandName = "push";
    final String branchDeleteCommandName = "branch-delete";
    Repository repository = createTestRepoUsingRepoService("repository-test", false);

    getL10nJCommander()
        .run(
            pushCommandName,
            "-r",
            repository.getName(),
            "-s",
            getInputResourcesTestDir("source").getAbsolutePath());

    final String oneMonthBranchName = "1-month-branch";
    final String tenDaysBranchName = "10-days-branch";
    Stream.of(masterBranchName, oneMonthBranchName, tenDaysBranchName)
        .forEach(
            branchName -> {
              getL10nJCommander()
                  .run(
                      pushCommandName,
                      "-r",
                      repository.getName(),
                      "-s",
                      getInputResourcesTestDir("source").getAbsolutePath(),
                      "-b",
                      branchName);
            });

    com.box.l10n.mojito.entity.Branch oneMonthBranch =
        branchRepository.findByNameAndRepository(oneMonthBranchName, repository);
    oneMonthBranch.setCreatedDate(ZonedDateTime.now().minusMonths(1));
    branchRepository.save(oneMonthBranch);

    com.box.l10n.mojito.entity.Branch tenDaysBranch =
        branchRepository.findByNameAndRepository(tenDaysBranchName, repository);
    tenDaysBranch.setCreatedDate(ZonedDateTime.now().minusDays(10));
    branchRepository.save(tenDaysBranch);

    L10nJCommander commander = getL10nJCommander();
    commander.consoleWriter = Mockito.spy(commander.consoleWriter);
    commander.run(
        branchDeleteCommandName,
        "-r",
        repository.getName(),
        "-nr",
        tenDaysBranchName,
        BRANCH_CREATED_BEFORE_SHORT,
        String.format(
            "3%c2%c",
            TimeframeType.DAYS.getAbbreviationInUpperCase(),
            TimeframeType.WEEKS.getAbbreviationInUpperCase()));
    Mockito.verify(commander.consoleWriter, Mockito.times(1))
        .a(
            String.format(
                "Please enter a single valid timeframe %s",
                BRANCH_CREATED_BEFORE_OPTIONS_AND_EXAMPLE));

    commander = getL10nJCommander();
    commander.consoleWriter = Mockito.spy(commander.consoleWriter);
    commander.run(
        branchDeleteCommandName,
        "-r",
        repository.getName(),
        "-nr",
        tenDaysBranchName,
        BRANCH_CREATED_BEFORE_SHORT,
        String.format("9991%c", TimeframeType.DAYS.getAbbreviationInUpperCase()));
    Mockito.verify(commander.consoleWriter, Mockito.times(1))
        .a(
            String.format(
                "Please enter a single valid timeframe %s",
                BRANCH_CREATED_BEFORE_OPTIONS_AND_EXAMPLE));

    commander = getL10nJCommander();
    commander.consoleWriter = Mockito.spy(commander.consoleWriter);
    commander.run(
        branchDeleteCommandName,
        "-r",
        repository.getName(),
        "-nr",
        tenDaysBranchName,
        BRANCH_CREATED_BEFORE_SHORT,
        "3R");
    Mockito.verify(commander.consoleWriter, Mockito.times(1))
        .a(
            String.format(
                "Please enter a single valid timeframe %s",
                BRANCH_CREATED_BEFORE_OPTIONS_AND_EXAMPLE));

    commander = getL10nJCommander();
    commander.consoleWriter = Mockito.spy(commander.consoleWriter);
    commander.run(
        branchDeleteCommandName,
        "-r",
        repository.getName(),
        "-nr",
        tenDaysBranchName,
        BRANCH_CREATED_BEFORE_SHORT,
        "3D",
        BRANCH_CREATED_BEFORE_LAST_WEEK_LONG);
    Mockito.verify(commander.consoleWriter, Mockito.times(1))
        .a(
            String.format(
                "Please, pick only one of these parameters: %s or %s",
                BRANCH_CREATED_BEFORE_LAST_WEEK_LONG, BRANCH_CREATED_BEFORE));

    checkBranches(
        "This should show all the branches",
        repository,
        null,
        null,
        null,
        masterBranchName,
        oneMonthBranchName,
        tenDaysBranchName);

    getL10nJCommander()
        .run(
            branchDeleteCommandName,
            "-r",
            repository.getName(),
            "-nr",
            String.format("(%s|%s)", oneMonthBranchName, tenDaysBranchName),
            BRANCH_CREATED_BEFORE_SHORT,
            String.format("3%c", TimeframeType.WEEKS.getAbbreviationInUpperCase()));

    checkBranches(
        "Should only delete oneMonthBranchName",
        repository,
        null,
        null,
        null,
        masterBranchName,
        tenDaysBranchName);

    getL10nJCommander()
        .run(
            branchDeleteCommandName,
            "-r",
            repository.getName(),
            "-nr",
            tenDaysBranchName,
            BRANCH_CREATED_BEFORE_SHORT,
            String.format("1%c", TimeframeType.WEEKS.getAbbreviationInLowerCase()));
    checkBranches("Should delete fiveDaysBranch", repository, null, null, null, masterBranchName);
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

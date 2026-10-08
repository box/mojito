package com.box.l10n.mojito.cli.command;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.box.l10n.mojito.cli.command.param.Param;
import com.box.l10n.mojito.cli.console.ConsoleWriter;
import com.box.l10n.mojito.rest.client.RepoTypeClient;
import com.box.l10n.mojito.rest.entity.RepoType;
import com.box.l10n.mojito.rest.entity.RepoTypeIntegrityChecker;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.fusesource.jansi.Ansi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Lists every repo type. Default prints id, name, description, {@code contains a value} when the AI
 * prompt is non-empty (body omitted), and {@code contains N values} for integrity checkers. {@code
 * --verbose} / {@code -vb} prints the prompt body and one checker pair per line.
 */
@Component
@Scope("prototype")
@Parameters(
    commandNames = {"repo-type-list"},
    commandDescription = "List all repo types")
public class RepoTypeListCommand extends Command {

  static Logger logger = LoggerFactory.getLogger(RepoTypeListCommand.class);

  @Autowired ConsoleWriter consoleWriter;

  @Autowired RepoTypeClient repoTypeClient;

  @Parameter(
      names = {Param.REPO_TYPE_LIST_VERBOSE_LONG, Param.REPO_TYPE_LIST_VERBOSE_SHORT},
      arity = 0,
      description = Param.REPO_TYPE_LIST_VERBOSE_DESCRIPTION)
  boolean verboseParam = false;

  @Override
  protected void execute() throws CommandException {
    consoleWriter.a("List repo types").println();

    List<RepoType> repoTypes = repoTypeClient.getRepoTypes(null);
    if (repoTypes.isEmpty()) {
      consoleWriter.newLine().a("No repo types found").println();
      return;
    }
    for (RepoType repoType : repoTypes) {
      printRepoType(repoType);
    }
  }

  private void printRepoType(RepoType repoType) {
    String description = repoType.getDescription() != null ? repoType.getDescription() : "";
    String aiPrompt = aiPromptLine(repoType.getAiPrompt());

    consoleWriter
        .newLine()
        .a("Repo type id --> ")
        .fg(Ansi.Color.MAGENTA)
        .a(repoType.getId())
        .println();
    consoleWriter.a("Name --> ").fg(Ansi.Color.MAGENTA).a(repoType.getName()).println();
    consoleWriter.a("Description --> ").fg(Ansi.Color.MAGENTA).a(description).println();
    consoleWriter.a("AI prompt --> ").fg(Ansi.Color.MAGENTA).a(aiPrompt).println();
    printIntegrityCheckers(repoType);
    consoleWriter.println();
  }

  /**
   * Default prints a count, or an empty value when there are no checkers. Verbose prints one {@code
   * extension:CHECKER_TYPE} line per checker, sorted by extension then type.
   */
  private void printIntegrityCheckers(RepoType repoType) {
    List<String> pairs = sortedCheckerPairs(repoType);
    if (!verboseParam || pairs.isEmpty()) {
      consoleWriter
          .a("Integrity checkers --> ")
          .fg(Ansi.Color.MAGENTA)
          .a(pairs.isEmpty() ? "" : containsValues(pairs.size()))
          .println();
      return;
    }

    consoleWriter.a("Integrity checkers --> ").fg(Ansi.Color.MAGENTA).a(pairs.get(0)).println();
    for (int i = 1; i < pairs.size(); i++) {
      consoleWriter.fg(Ansi.Color.MAGENTA).a(pairs.get(i)).println();
    }
  }

  private static List<String> sortedCheckerPairs(RepoType repoType) {
    if (repoType.getIntegrityCheckers() == null || repoType.getIntegrityCheckers().isEmpty()) {
      return List.of();
    }
    List<RepoTypeIntegrityChecker> checkers = new ArrayList<>(repoType.getIntegrityCheckers());
    checkers.sort(
        Comparator.comparing(
                RepoTypeIntegrityChecker::getAssetExtension,
                Comparator.nullsLast(String::compareTo))
            .thenComparing(
                checker ->
                    checker.getIntegrityCheckerType() == null
                        ? ""
                        : checker.getIntegrityCheckerType().name()));
    List<String> pairs = new ArrayList<>();
    for (RepoTypeIntegrityChecker checker : checkers) {
      pairs.add(checker.getAssetExtension() + ":" + checker.getIntegrityCheckerType());
    }
    return pairs;
  }

  private static String containsValues(int count) {
    return "contains " + count + (count == 1 ? " value" : " values");
  }

  String aiPromptLine(String aiPrompt) {
    if (verboseParam) {
      return aiPrompt != null ? aiPrompt : "";
    }
    return StringUtils.isNotEmpty(aiPrompt) ? "contains a value" : "";
  }
}

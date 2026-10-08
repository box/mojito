package com.box.l10n.mojito.cli.command;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.box.l10n.mojito.cli.command.param.Param;
import com.box.l10n.mojito.cli.console.ConsoleWriter;
import com.box.l10n.mojito.rest.entity.RepoType;
import com.box.l10n.mojito.rest.entity.RepoTypeIntegrityChecker;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.fusesource.jansi.Ansi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/** Views id, name, description, AI prompt, and integrity checkers of an existing repo type. */
@Component
@Scope("prototype")
@Parameters(
    commandNames = {"repo-type-view"},
    commandDescription = "View a repo type")
public class RepoTypeViewCommand extends Command {

  static Logger logger = LoggerFactory.getLogger(RepoTypeViewCommand.class);

  @Autowired ConsoleWriter consoleWriter;

  @Autowired CommandHelper commandHelper;

  @Parameter(
      names = {Param.REPO_TYPE_NAME_LONG, Param.REPO_TYPE_NAME_SHORT},
      arity = 1,
      required = true,
      description = Param.REPO_TYPE_NAME_DESCRIPTION)
  String nameParam;

  @Override
  protected void execute() throws CommandException {
    consoleWriter.a("View repo type: ").fg(Ansi.Color.CYAN).a(nameParam).println();

    RepoType repoType = commandHelper.findRepoTypeByName(nameParam);
    String description = repoType.getDescription() != null ? repoType.getDescription() : "";
    String aiPrompt = repoType.getAiPrompt() != null ? repoType.getAiPrompt() : "";

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
   * Prints each checker as {@code extension:CHECKER_TYPE}, sorted by extension then type. An empty
   * set prints nothing, matching {@code repo-view}.
   */
  private void printIntegrityCheckers(RepoType repoType) {
    if (repoType.getIntegrityCheckers() == null || repoType.getIntegrityCheckers().isEmpty()) {
      return;
    }

    List<RepoTypeIntegrityChecker> checkers = new ArrayList<>(repoType.getIntegrityCheckers());
    checkers.sort(
        Comparator.comparing(
                RepoTypeIntegrityChecker::getAssetExtension, Comparator.nullsLast(String::compareTo))
            .thenComparing(
                checker ->
                    checker.getIntegrityCheckerType() == null
                        ? ""
                        : checker.getIntegrityCheckerType().name()));

    consoleWriter.newLine().a("Integrity checkers --> ").fg(Ansi.Color.MAGENTA);
    for (int i = 0; i < checkers.size(); i++) {
      RepoTypeIntegrityChecker checker = checkers.get(i);
      consoleWriter.a(checker.getAssetExtension() + ":" + checker.getIntegrityCheckerType());
      if (i == checkers.size() - 1) {
        consoleWriter.println();
      } else {
        consoleWriter.a(",");
      }
    }
  }
}

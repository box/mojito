package com.box.l10n.mojito.cli.command;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.box.l10n.mojito.cli.command.param.Param;
import com.box.l10n.mojito.rest.client.exception.RepositoryNotFoundException;
import com.box.l10n.mojito.rest.entity.IntegrityChecker;
import com.box.l10n.mojito.rest.entity.Repository;
import com.box.l10n.mojito.rest.entity.RepositoryLocale;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.fusesource.jansi.Ansi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Command to view properties of existing repository
 *
 * @author jyi
 */
@Component
@Scope("prototype")
@Parameters(
    commandNames = {"repo-view"},
    commandDescription = "View a repository")
public class RepoViewCommand extends RepoCommand {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(RepoViewCommand.class);

  @Parameter(
      names = {Param.REPOSITORY_NAME_LONG, Param.REPOSITORY_NAME_SHORT},
      arity = 1,
      required = true,
      description = Param.REPOSITORY_NAME_DESCRIPTION)
  String nameParam;

  @Override
  public void execute() throws CommandException {
    if (!isJsonOutput()) {
      consoleWriter.a("View repository: ").fg(Ansi.Color.CYAN).a(nameParam).println();
    }

    try {
      Repository repository = repositoryClient.getRepositoryByName(nameParam);
      if (isJsonOutput()) {
        writeJsonSuccess(toJsonData(repository));
      } else {
        consoleWriter
            .newLine()
            .a("Repository id --> ")
            .fg(Ansi.Color.MAGENTA)
            .a(repository.getId())
            .println();
        printIntegrityChecker(repository);
        printLocales(repository);
        consoleWriter.println();
      }
    } catch (RepositoryNotFoundException ex) {
      throw new CommandException(ex.getMessage(), ex);
    }
  }

  /** Payload under the shared {@code data} key for {@code --json} output. */
  Map<String, Object> toJsonData(Repository repository) {
    Map<String, Object> data = new LinkedHashMap<>();
    data.put("repo", repository.getName());
    data.put("description", repository.getDescription());
    data.put("id", repository.getId());
    data.put("integrityCheckers", toIntegrityCheckersJson(repository));
    data.put("locales", toLocalesJson(repository));
    return data;
  }

  private List<Map<String, Object>> toIntegrityCheckersJson(Repository repository) {
    List<Map<String, Object>> result = new ArrayList<>();
    if (repository.getIntegrityCheckers() == null || repository.getIntegrityCheckers().isEmpty()) {
      return result;
    }
    List<IntegrityChecker> integrityCheckers = new ArrayList<>(repository.getIntegrityCheckers());
    Collections.sort(integrityCheckers, IntegrityChecker.getComparator());
    for (IntegrityChecker integrityChecker : integrityCheckers) {
      Map<String, Object> entry = new LinkedHashMap<>();
      entry.put("assetExtension", integrityChecker.getAssetExtension());
      entry.put("integrityCheckerType", integrityChecker.getIntegrityCheckerType().toString());
      result.add(entry);
    }
    return result;
  }

  private List<Map<String, Object>> toLocalesJson(Repository repository) {
    List<Map<String, Object>> result = new ArrayList<>();
    if (repository.getRepositoryLocales() == null || repository.getRepositoryLocales().isEmpty()) {
      return result;
    }
    List<RepositoryLocale> repositoryLocales = new ArrayList<>(repository.getRepositoryLocales());
    Collections.sort(repositoryLocales, RepositoryLocale.getComparator());
    for (RepositoryLocale repositoryLocale : repositoryLocales) {
      Map<String, Object> entry = new LinkedHashMap<>();
      entry.put("bcp47Tag", repositoryLocale.getLocale().getBcp47Tag());
      entry.put("toBeFullyTranslated", repositoryLocale.isToBeFullyTranslated());
      RepositoryLocale parentRepositoryLocale = repositoryLocale.getParentLocale();
      if (parentRepositoryLocale != null && parentRepositoryLocale.getLocale() != null) {
        entry.put("parentLocale", parentRepositoryLocale.getLocale().getBcp47Tag());
      }
      result.add(entry);
    }
    return result;
  }

  private void printIntegrityChecker(Repository repository) {
    if (repository.getIntegrityCheckers() != null && !repository.getIntegrityCheckers().isEmpty()) {
      List<IntegrityChecker> integrityCheckers = new ArrayList<>();
      integrityCheckers.addAll(repository.getIntegrityCheckers());
      Collections.sort(integrityCheckers, IntegrityChecker.getComparator());

      consoleWriter.newLine().a("Integrity checkers --> ").fg(Ansi.Color.MAGENTA);
      for (int i = 0; i < integrityCheckers.size(); i++) {
        IntegrityChecker integrityChecker = integrityCheckers.get(i);
        consoleWriter
            .a(integrityChecker.getAssetExtension())
            .a(":")
            .a(integrityChecker.getIntegrityCheckerType().toString());
        if (i == integrityCheckers.size() - 1) {
          consoleWriter.println();
        } else {
          consoleWriter.a(",");
        }
      }
    }
  }

  private void printLocales(Repository repository) {
    if (repository.getRepositoryLocales() != null && !repository.getRepositoryLocales().isEmpty()) {
      List<RepositoryLocale> repositoryLocales = new ArrayList<>();
      repositoryLocales.addAll(repository.getRepositoryLocales());
      Collections.sort(repositoryLocales, RepositoryLocale.getComparator());

      consoleWriter.newLine().a("Repository locales --> ").fg(Ansi.Color.MAGENTA);
      for (int j = 0; j < repositoryLocales.size(); j++) {
        RepositoryLocale repositoryLocale = repositoryLocales.get(j);
        String bcp47Tag = repositoryLocale.getLocale().getBcp47Tag();
        if (repositoryLocale.isToBeFullyTranslated()) {
          consoleWriter.a(bcp47Tag).a(" ");
        } else {
          RepositoryLocale parentRepositoryLocale = repositoryLocale.getParentLocale();
          if (parentRepositoryLocale != null) {
            String parentBcp47Tag = parentRepositoryLocale.getLocale().getBcp47Tag();

            if (parentRepositoryLocale.getParentLocale() == null) {
              consoleWriter.a("\"(").a(bcp47Tag).a(")\" ");
            } else {
              consoleWriter.a("\"(").a(bcp47Tag).a(")->").a(parentBcp47Tag).a("\" ");
            }
          }
        }
      }
      consoleWriter.println();
    }
  }
}

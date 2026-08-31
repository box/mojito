package com.box.l10n.mojito.cli.command;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.box.l10n.mojito.cli.command.param.Param;
import com.box.l10n.mojito.cli.console.ConsoleWriter;
import com.box.l10n.mojito.rest.client.RepoTypeClient;
import com.box.l10n.mojito.rest.entity.RepoType;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.fusesource.jansi.Ansi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Lists every repo type. Default prints id, name, description, and {@code set} when the AI prompt
 * is non-empty (body omitted). {@code --verbose} / {@code -vb} prints the prompt body (same as
 * {@code repo-type-view}).
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
    consoleWriter.println();
  }

  String aiPromptLine(String aiPrompt) {
    if (verboseParam) {
      return aiPrompt != null ? aiPrompt : "";
    }
    return StringUtils.isNotEmpty(aiPrompt) ? "set" : "";
  }
}

package com.box.l10n.mojito.cli.command;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Parameters;
import com.box.l10n.mojito.cli.command.param.Param;
import com.box.l10n.mojito.cli.console.ConsoleWriter;
import com.box.l10n.mojito.rest.client.RepoTypeClient;
import com.box.l10n.mojito.rest.entity.IntegrityChecker;
import com.box.l10n.mojito.rest.entity.RepoType;
import com.box.l10n.mojito.rest.entity.RepoTypeIntegrityChecker;
import java.util.Set;
import org.fusesource.jansi.Ansi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;

/** Updates name, description, AI prompt, and/or integrity checkers of an existing repo type. */
@Component
@Scope("prototype")
@Parameters(
    commandNames = {"repo-type-update"},
    commandDescription = "Updates a repo type")
public class RepoTypeUpdateCommand extends Command {

  static Logger logger = LoggerFactory.getLogger(RepoTypeUpdateCommand.class);

  @Autowired ConsoleWriter consoleWriter;

  @Autowired CommandHelper commandHelper;

  @Autowired RepoTypeClient repoTypeClient;

  @Parameter(
      names = {Param.REPO_TYPE_NAME_LONG, Param.REPO_TYPE_NAME_SHORT},
      arity = 1,
      required = true,
      description = Param.REPO_TYPE_NAME_DESCRIPTION)
  String nameParam;

  @Parameter(
      names = {Param.REPO_TYPE_NEW_NAME_LONG, Param.REPO_TYPE_NEW_NAME_SHORT},
      arity = 1,
      required = false,
      description = Param.REPO_TYPE_NEW_NAME_DESCRIPTION)
  String newNameParam;

  @Parameter(
      names = {Param.REPO_TYPE_DESCRIPTION_LONG, Param.REPO_TYPE_DESCRIPTION_SHORT},
      arity = 1,
      required = false,
      description = Param.REPO_TYPE_DESCRIPTION_DESCRIPTION)
  String descriptionParam;

  @Parameter(
      names = {Param.REPO_TYPE_AI_PROMPT_LONG, Param.REPO_TYPE_AI_PROMPT_SHORT},
      arity = 1,
      required = false,
      description = Param.REPO_TYPE_AI_PROMPT_DESCRIPTION)
  String aiPromptParam;

  @Parameter(
      names = {Param.REPO_TYPE_AI_PROMPT_FILE_LONG, Param.REPO_TYPE_AI_PROMPT_FILE_SHORT},
      arity = 1,
      required = false,
      description = Param.REPO_TYPE_AI_PROMPT_FILE_DESCRIPTION)
  String aiPromptFileParam;

  /**
   * Same {@code FILE_EXTENSION:CHECKER_TYPE} input as repo update. Omitted flag leaves {@code
   * integrityCheckers} null so the JSON property is not sent. An empty value sends {@code []} and
   * clears the type's checkers.
   */
  static final String INTEGRITY_CHECK_DESCRIPTION =
      "Replaces integrity checkers for this repo type, comma separated format: \"FILE_EXTENSION_1:CHECKER_TYPE_1,FILE_EXTENSION_2:CHECKER_TYPE_2\"\n       "
          + "Omitting the flag leaves existing checkers unchanged. An empty value clears them.\n       "
          + "Available Checker types: "
          + IntegrityCheckerCli.AVAILABLE_CHECKER_TYPES_TOKEN
          + "\n       "
          + "For example: \"properties:MESSAGE_FORMAT,properties:TRAILING_WHITESPACE\"";

  @Parameter(
      names = {RepoCommand.INTEGRITY_CHECK_LONG_PARAM, RepoCommand.INTEGRITY_CHECK_SHORT_PARAM},
      arity = 1,
      required = false,
      description = INTEGRITY_CHECK_DESCRIPTION)
  String integrityCheckParam;

  @Override
  protected void execute() throws CommandException {
    consoleWriter.a("Update repo type: ").fg(Ansi.Color.CYAN).a(nameParam).println();

    if (newNameParam == null
        && descriptionParam == null
        && aiPromptParam == null
        && aiPromptFileParam == null
        && integrityCheckParam == null) {
      throw new CommandException(
          "Must provide at least one of the following options: --new-name, --description,"
              + " --ai-prompt, --ai-prompt-file, --integrity-check");
    }

    RepoType existing = commandHelper.findRepoTypeByName(nameParam);

    try {
      String aiPrompt = CommandHelper.resolveRepoTypeAiPrompt(aiPromptParam, aiPromptFileParam);
      Set<IntegrityChecker> integrityCheckers =
          IntegrityCheckerCli.parse(integrityCheckParam, consoleWriter);
      RepoType updated =
          repoTypeClient.updateRepoType(
              existing.getId(),
              repoTypePatch(
                  newNameParam,
                  descriptionParam,
                  aiPrompt,
                  IntegrityCheckerCli.toRepoTypeCheckers(integrityCheckers)));
      consoleWriter
          .newLine()
          .a("updated --> repo type id: ")
          .fg(Ansi.Color.MAGENTA)
          .a(updated.getId())
          .println();
    } catch (ParameterException ex) {
      throw new CommandException(ex.getMessage(), ex);
    } catch (HttpClientErrorException ex) {
      throw CommandHelper.repoTypeClientError(ex);
    }
  }

  /**
   * PATCH body that leaves checkers unchanged. {@code null} checkers are omitted by the CLI
   * RestTemplate {@code NON_NULL} mapper.
   */
  static RepoType repoTypePatch(String newName, String description, String aiPrompt) {
    return repoTypePatch(newName, description, aiPrompt, null);
  }

  /**
   * PATCH body for name, description, AI prompt, and/or integrity checkers. {@code
   * integrityCheckers} {@code null} is omitted (leave unchanged). A non-null empty set serializes
   * as {@code []} and clears checkers. {@code aiPrompt} {@code null} is omitted the same way; empty
   * string is included and clears the prompt.
   */
  static RepoType repoTypePatch(
      String newName,
      String description,
      String aiPrompt,
      Set<RepoTypeIntegrityChecker> integrityCheckers) {
    RepoType patch = new RepoType();
    patch.setName(newName);
    patch.setDescription(description);
    patch.setAiPrompt(aiPrompt);
    patch.setIntegrityCheckers(integrityCheckers);
    return patch;
  }
}

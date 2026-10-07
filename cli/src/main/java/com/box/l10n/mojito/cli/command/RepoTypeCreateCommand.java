package com.box.l10n.mojito.cli.command;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Parameters;
import com.box.l10n.mojito.cli.command.param.Param;
import com.box.l10n.mojito.cli.console.ConsoleWriter;
import com.box.l10n.mojito.rest.client.RepoTypeClient;
import com.box.l10n.mojito.rest.entity.IntegrityChecker;
import com.box.l10n.mojito.rest.entity.RepoType;
import java.util.Set;
import org.fusesource.jansi.Ansi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;

/**
 * Creates a repo type with a required name and optional description, AI prompt, and integrity
 * checkers.
 */
@Component
@Scope("prototype")
@Parameters(
    commandNames = {"repo-type-create"},
    commandDescription = "Creates a repo type")
public class RepoTypeCreateCommand extends Command {

  static Logger logger = LoggerFactory.getLogger(RepoTypeCreateCommand.class);

  @Autowired ConsoleWriter consoleWriter;

  @Autowired RepoTypeClient repoTypeClient;

  @Parameter(
      names = {Param.REPO_TYPE_NAME_LONG, Param.REPO_TYPE_NAME_SHORT},
      arity = 1,
      required = true,
      description = Param.REPO_TYPE_NAME_DESCRIPTION)
  String nameParam;

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
   * Same {@code FILE_EXTENSION:CHECKER_TYPE} input as repository create. Omitted flag leaves {@code
   * integrityCheckers} null so the JSON property is not sent.
   */
  @Parameter(
      names = {RepoCommand.INTEGRITY_CHECK_LONG_PARAM, RepoCommand.INTEGRITY_CHECK_SHORT_PARAM},
      arity = 1,
      required = false,
      description =
          "Integrity checkers for this repo type, comma separated format: \"FILE_EXTENSION_1:CHECKER_TYPE_1,FILE_EXTENSION_2:CHECKER_TYPE_2\"\n       "
              + "Available Checker types: "
              + IntegrityCheckerCli.AVAILABLE_CHECKER_TYPES_TOKEN
              + "\n       "
              + "For example: \"properties:MESSAGE_FORMAT,properties:TRAILING_WHITESPACE\"")
  String integrityCheckParam;

  @Override
  protected void execute() throws CommandException {
    consoleWriter.a("Create repo type: ").fg(Ansi.Color.CYAN).a(nameParam).println();

    try {
      RepoType toCreate = new RepoType();
      toCreate.setName(nameParam);
      toCreate.setDescription(descriptionParam);
      toCreate.setAiPrompt(CommandHelper.resolveRepoTypeAiPrompt(aiPromptParam, aiPromptFileParam));
      Set<IntegrityChecker> integrityCheckers =
          IntegrityCheckerCli.parse(integrityCheckParam, consoleWriter);
      toCreate.setIntegrityCheckers(IntegrityCheckerCli.toRepoTypeCheckers(integrityCheckers));

      RepoType created = repoTypeClient.createRepoType(toCreate);
      consoleWriter
          .newLine()
          .a("created --> repo type id: ")
          .fg(Ansi.Color.MAGENTA)
          .a(created.getId())
          .println();
    } catch (ParameterException ex) {
      throw new CommandException(ex.getMessage(), ex);
    } catch (HttpClientErrorException ex) {
      throw CommandHelper.repoTypeClientError(ex);
    }
  }
}

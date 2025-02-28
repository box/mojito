package com.box.l10n.mojito.cli.command;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.box.l10n.mojito.apiclient.AIServiceClient;
import com.box.l10n.mojito.cli.console.ConsoleWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
@Parameters(
    commandNames = {"add-ai-prompt-to-repository"},
    commandDescription = "Add an AI prompt for a given repository")
public class AddPromptToRepositoryCommand extends Command {

  static Logger logger = LoggerFactory.getLogger(AddPromptToRepositoryCommand.class);

  @Autowired AIServiceClient aiServiceClient;

  @Parameter(
      names = {"--repository-name", "-r"},
      required = true,
      description = "Repository name")
  String repository;

  @Parameter(
      names = {"--prompt-id", "-pid"},
      required = true,
      description = "The prompt id")
  Long promptId;

  @Parameter(
      names = {"--prompt-type", "-pty"},
      required = true,
      description = "The type of prompt to create")
  String promptType;

  @Autowired ConsoleWriter consoleWriter;

  public void execute() {
    logger.debug("Add prompt to {} repository with id: {}", repository, promptId);
    aiServiceClient.addPromptToRepository(promptId, repository, promptType);
    consoleWriter
        .newLine()
        .a("Prompt with id: " + promptId + ", added to repository: " + repository)
        .println();
  }
}

package com.box.l10n.mojito.cli.command;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.box.l10n.mojito.cli.console.ConsoleWriter;
import com.box.l10n.mojito.rest.client.AIServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
@Parameters(
    commandNames = {"delete-ai-prompt-context-message"},
    commandDescription = "Delete an AI prompt context message.")
public class DeleteAIPromptContextMessageCommand extends Command {

  static Logger logger = LoggerFactory.getLogger(DeleteAIPromptContextMessageCommand.class);

  @Autowired AIServiceClient aiServiceClient;

  @Parameter(
      names = {"--id", "-i"},
      required = true,
      description = "The id of the context message to delete.")
  Long id;

  @Autowired private ConsoleWriter consoleWriter;

  @Override
  protected void execute() throws CommandException {
    deletePromptContextMessage();
  }

  private void deletePromptContextMessage() {
    logger.debug("Received request to create prompt content message");
    aiServiceClient.deletePromptContextMessage(id);
    consoleWriter.newLine().a("Prompt context message deleted").println();
  }
}

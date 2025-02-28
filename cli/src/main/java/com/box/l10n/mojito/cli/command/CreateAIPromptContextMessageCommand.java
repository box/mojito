package com.box.l10n.mojito.cli.command;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.box.l10n.mojito.apiclient.AIServiceClient;
import com.box.l10n.mojito.apiclient.model.AIPromptContextMessageCreateRequest;
import com.box.l10n.mojito.cli.console.ConsoleWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
@Parameters(
    commandNames = {"create-ai-prompt-context-message"},
    commandDescription = "Create an AI prompt context message.")
public class CreateAIPromptContextMessageCommand extends Command {

  static Logger logger = LoggerFactory.getLogger(CreateAIPromptContextMessageCommand.class);

  @Autowired AIServiceClient aiServiceClient;

  @Parameter(
      names = {"--content", "-c"},
      required = true,
      description = "The system prompt text")
  String content;

  @Parameter(
      names = {"--message-type", "-mt"},
      required = true,
      description = "The type of message to create")
  String messageType;

  @Parameter(
      names = {"--prompt-id", "-p"},
      required = true,
      description = "The id of the associated AI prompt.")
  Long promptId;

  @Parameter(
      names = {"--order-index", "-i"},
      required = true,
      description = "The index of the message in the prompt context.")
  int orderIndex;

  @Autowired private ConsoleWriter consoleWriter;

  @Override
  protected void execute() throws CommandException {
    createPromptContextMessage();
  }

  private void createPromptContextMessage() {
    logger.debug("Received request to create prompt content message");
    AIPromptContextMessageCreateRequest aiPromptContextMessageCreateRequest =
        new AIPromptContextMessageCreateRequest();
    aiPromptContextMessageCreateRequest.setContent(content);
    aiPromptContextMessageCreateRequest.setMessageType(messageType);
    aiPromptContextMessageCreateRequest.setAiPromptId(promptId);
    aiPromptContextMessageCreateRequest.setOrderIndex(orderIndex);
    long contextMessageId =
        aiServiceClient.createPromptMessage(aiPromptContextMessageCreateRequest);
    consoleWriter
        .newLine()
        .a("Prompt context message created with id: " + contextMessageId)
        .println();
  }
}

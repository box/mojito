package com.box.l10n.mojito.cli.command;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.box.l10n.mojito.cli.console.ConsoleWriter;
import com.box.l10n.mojito.rest.client.AIServiceClient;
import com.box.l10n.mojito.rest.entity.AIPromptCreateRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
@Parameters(
    commandNames = {"create-ai-prompt"},
    commandDescription = "Create an AI prompt for a given repository")
public class CreateAIPromptCommand extends Command {

  static Logger logger = LoggerFactory.getLogger(CreateAIPromptCommand.class);

  @Autowired AIServiceClient AIServiceClient;

  @Parameter(
      names = {"--repository-name", "-r"},
      required = true,
      description = "Repository name")
  String repository;

  @Parameter(
      names = {"--user-prompt-text", "-upt"},
      required = true,
      description = "The user prompt text")
  String userPromptText;

  @Parameter(
      names = {"--system-prompt-text", "-spt"},
      required = true,
      description = "The system prompt text")
  String systemPromptText;

  @Parameter(
      names = {"--model-name", "-mn"},
      required = true,
      description = "The model name to used for the prompt")
  String modelName;

  @Parameter(
      names = {"--prompt-type", "-pty"},
      required = true,
      description = "The type of prompt to create")
  String promptType;

  @Parameter(
      names = {"--prompt-temperature", "-pt"},
      required = false,
      description = "The temperature to use for the prompt")
  float promptTemperature = 0.0F;

  @Autowired private ConsoleWriter consoleWriter;

  @Override
  protected void execute() throws CommandException {
    createPrompt();
  }

  private void createPrompt() {
    logger.debug("Received request to create prompt");
    AIPromptCreateRequest AIPromptCreateRequest = new AIPromptCreateRequest();
    AIPromptCreateRequest.setRepositoryName(repository);
    AIPromptCreateRequest.setSystemPrompt(systemPromptText);
    AIPromptCreateRequest.setUserPrompt(userPromptText);
    AIPromptCreateRequest.setModelName(modelName);
    AIPromptCreateRequest.setPromptType(promptType);
    AIPromptCreateRequest.setPromptTemperature(promptTemperature);
    long promptId = AIServiceClient.createPrompt(AIPromptCreateRequest);
    consoleWriter.newLine().a("Prompt created with id: " + promptId).println();
  }
}

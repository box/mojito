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

  @Autowired AIServiceClient aiServiceClient;

  @Parameter(
      names = {"--repository-name", "-r"},
      required = false,
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

  @Parameter(
      names = {"--is-json-response", "-ijr"},
      required = false,
      description = "The prompt response is expected to be in JSON format from the LLM")
  boolean isJsonResponse = false;

  @Parameter(
      names = {"--json-response-key", "-jrk"},
      required = false,
      description = "The key to use to extract the translation from the JSON response")
  String jsonResponseKey;

  @Autowired private ConsoleWriter consoleWriter;

  @Override
  protected void execute() throws CommandException {
    createPrompt();
  }

  private void createPrompt() {
    logger.debug("Received request to create prompt");
    AIPromptCreateRequest aiPromptCreateRequest = new AIPromptCreateRequest();
    aiPromptCreateRequest.setRepositoryName(repository);
    aiPromptCreateRequest.setSystemPrompt(systemPromptText);
    aiPromptCreateRequest.setUserPrompt(userPromptText);
    aiPromptCreateRequest.setModelName(modelName);
    aiPromptCreateRequest.setPromptType(promptType);
    aiPromptCreateRequest.setPromptTemperature(promptTemperature);
    aiPromptCreateRequest.setJsonResponse(isJsonResponse);
    if (isJsonResponse && jsonResponseKey == null) {
      throw new CommandException("jsonResponseKey is required when isJsonResponse is true");
    }
    aiPromptCreateRequest.setJsonResponseKey(jsonResponseKey);
    long promptId = aiServiceClient.createPrompt(aiPromptCreateRequest);
    consoleWriter.newLine().a("Prompt created with id: " + promptId).println();
  }
}

package com.box.l10n.mojito.cli.command;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.box.l10n.mojito.cli.apiclient.AiPromptWsApiProxy;
import com.box.l10n.mojito.cli.model.AITranslationLocalePromptOverridesRequest;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@Scope("prototype")
@Parameters(
    commandNames = {"ai-repository-locale-prompt-override"},
    commandDescription =
        "Create/Update/Delete locale translation AI prompt overrides for a given repository")
public class AIRepositoryLocaleOverrideCommand extends Command {

  static Logger logger = LoggerFactory.getLogger(CreateAIPromptCommand.class);

  @Autowired AiPromptWsApiProxy aiServiceClient;

  @Parameter(
      names = {"--repository-name", "-r"},
      required = true,
      description = "Repository name")
  String repository;

  @Parameter(
      names = {"--ai-prompt-id", "-aid"},
      required = true,
      description = "AI prompt id")
  Long aiPromptId;

  @Parameter(
      names = {"--locales", "-l"},
      required = true,
      description = "Locale BCP-47 tags provided in a comma separated list")
  String locales;

  @Parameter(
      names = {"--disabled"},
      required = false,
      description =
          "Indicates if the locales are disabled for AI translation. Setting to true means AI translation will be skipped for the relevant locales. Default is false")
  boolean disabled = false;

  @Parameter(
      names = {"--delete"},
      required = false,
      description = "Delete the AI prompt overrides for the given locales")
  boolean isDelete = false;

  @Override
  protected void execute() throws CommandException {
    AITranslationLocalePromptOverridesRequest aiTranslationLocalePromptOverridesRequest =
        new AITranslationLocalePromptOverridesRequest();
    aiTranslationLocalePromptOverridesRequest.setRepositoryName(repository);
    aiTranslationLocalePromptOverridesRequest.setLocales(
        Arrays.asList(StringUtils.commaDelimitedListToStringArray(locales)));
    aiTranslationLocalePromptOverridesRequest.setAiPromptId(aiPromptId);
    aiTranslationLocalePromptOverridesRequest.setDisabled(disabled);

    if (isDelete) {
      aiServiceClient.deleteRepositoryLocalePromptOverrides(
          aiTranslationLocalePromptOverridesRequest);
    } else {
      aiServiceClient.createOrUpdateRepositoryLocalePromptOverrides(
          aiTranslationLocalePromptOverridesRequest);
    }
  }
}

package com.box.l10n.mojito.service.ai;

import com.box.l10n.mojito.entity.AIPrompt;
import com.box.l10n.mojito.rest.ai.AIPromptContextMessageCreateRequest;
import com.box.l10n.mojito.rest.ai.AIPromptCreateRequest;
import java.util.List;
import java.util.Set;

public interface PromptService {

  Long createPrompt(AIPromptCreateRequest AIPromptCreateRequest);

  void deletePrompt(Long promptId);

  AIPrompt getPrompt(Long promptId);

  List<AIPrompt> getAllActivePromptsForRepository(String repositoryName);

  List<AIPrompt> getAllActivePrompts();

  Long createPromptContextMessage(
      AIPromptContextMessageCreateRequest aiPromptContextMessageCreateRequest);

  void deletePromptContextMessage(Long promptContextMessageId);

  void addPromptToRepository(Long promptId, String repositoryName, String promptType);

  void createOrUpdateRepositoryLocaleTranslationPromptOverrides(
      String repositoryName, Set<String> bcp47Tags, Long aiPromptId, boolean disabled);

  void deleteRepositoryLocaleTranslationPromptOverride(
      String repositoryName, Set<String> bcp47Tags);
}

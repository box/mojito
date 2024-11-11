package com.box.l10n.mojito.service.ai;

import com.box.l10n.mojito.JSR310Migration;
import com.box.l10n.mojito.entity.AIPrompt;
import com.box.l10n.mojito.entity.AIPromptContextMessage;
import com.box.l10n.mojito.entity.AIPromptType;
import com.box.l10n.mojito.entity.Locale;
import com.box.l10n.mojito.entity.PromptType;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.RepositoryLocale;
import com.box.l10n.mojito.entity.RepositoryLocaleAIPrompt;
import com.box.l10n.mojito.rest.ai.AIException;
import com.box.l10n.mojito.rest.ai.AIPromptContextMessageCreateRequest;
import com.box.l10n.mojito.rest.ai.AIPromptCreateRequest;
import com.box.l10n.mojito.service.ai.openai.OpenAIPromptContextMessageType;
import com.box.l10n.mojito.service.locale.LocaleRepository;
import com.box.l10n.mojito.service.repository.RepositoryRepository;
import io.micrometer.core.annotation.Timed;
import jakarta.transaction.Transactional;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(value = "l10n.ai.service.type", havingValue = "OpenAI")
public class LLMPromptService implements PromptService {

  static Logger logger = LoggerFactory.getLogger(LLMPromptService.class);

  @Autowired RepositoryRepository repositoryRepository;

  @Autowired AIPromptRepository aiPromptRepository;

  @Autowired AIPromptTypeRepository aiPromptTypeRepository;

  @Autowired RepositoryLocaleAIPromptRepository repositoryLocaleAIPromptRepository;

  @Autowired AIPromptContextMessageRepository aiPromptContextMessageRepository;

  @Autowired LocaleRepository localeRepository;

  @Timed("LLMPromptService.createPrompt")
  @Transactional
  public Long createPrompt(AIPromptCreateRequest AIPromptCreateRequest) {

    Repository repository = getRepository(AIPromptCreateRequest.getRepositoryName());

    AIPromptType aiPromptType =
        aiPromptTypeRepository.findByName(AIPromptCreateRequest.getPromptType());
    if (aiPromptType == null) {
      logger.error("Prompt type not found: {}", AIPromptCreateRequest.getPromptType());
      throw new AIException("Prompt type not found: " + AIPromptCreateRequest.getPromptType());
    }

    AIPrompt aiPrompt = new AIPrompt();
    aiPrompt.setSystemPrompt(AIPromptCreateRequest.getSystemPrompt());
    aiPrompt.setUserPrompt(AIPromptCreateRequest.getUserPrompt());
    aiPrompt.setPromptTemperature(AIPromptCreateRequest.getPromptTemperature());
    aiPrompt.setModelName(AIPromptCreateRequest.getModelName());
    aiPrompt.setPromptType(aiPromptType);
    ZonedDateTime now = JSR310Migration.dateTimeNow();
    aiPrompt.setCreatedDate(now);
    aiPrompt.setLastModifiedDate(now);
    aiPrompt.setJsonResponse(AIPromptCreateRequest.isJsonResponse());
    aiPrompt.setJsonResponseKey(AIPromptCreateRequest.getJsonResponseKey());
    aiPromptRepository.save(aiPrompt);
    logger.debug("Created prompt with id: {}", aiPrompt.getId());

    RepositoryLocaleAIPrompt repositoryLocaleAIPrompt = new RepositoryLocaleAIPrompt();
    repositoryLocaleAIPrompt.setRepository(repository);
    repositoryLocaleAIPrompt.setAiPrompt(aiPrompt);
    repositoryLocaleAIPromptRepository.save(repositoryLocaleAIPrompt);
    logger.debug("Created repository prompt with id: {}", repositoryLocaleAIPrompt.getId());

    return aiPrompt.getId();
  }

  @Timed("LLMPromptService.addPromptToRepository")
  public void addPromptToRepository(Long promptId, String repositoryName, String promptType) {
    Repository repository = getRepository(repositoryName);

    AIPromptType aiPromptType = aiPromptTypeRepository.findByName(promptType);
    if (aiPromptType == null) {
      logger.error("Prompt type not found: {}", promptType);
      throw new AIException("Prompt type not found: " + promptType);
    }

    AIPrompt aiPrompt =
        aiPromptRepository
            .findById(promptId)
            .orElseThrow(() -> new AIException("Prompt not found: " + promptId));

    RepositoryLocaleAIPrompt repositoryLocaleAIPrompt = new RepositoryLocaleAIPrompt();
    repositoryLocaleAIPrompt.setRepository(repository);
    repositoryLocaleAIPrompt.setAiPrompt(aiPrompt);
    repositoryLocaleAIPromptRepository.save(repositoryLocaleAIPrompt);
    logger.debug("Created repository prompt with id: {}", repositoryLocaleAIPrompt.getId());
  }

  @Timed("LLMPromptService.getPromptsByRepositoryAndPromptType")
  public List<AIPrompt> getPromptsByRepositoryAndPromptType(
      Repository repository, PromptType promptType) {
    return aiPromptRepository.findByRepositoryIdAndPromptTypeName(
        repository.getId(), promptType.name());
  }

  @Timed("LLMPromptService.deletePrompt")
  public void deletePrompt(Long promptId) {
    AIPrompt aiPrompt =
        aiPromptRepository
            .findById(promptId)
            .orElseThrow(() -> new AIException("Prompt not found: " + promptId));
    aiPrompt.setDeleted(true);
    aiPrompt.setLastModifiedDate(JSR310Migration.dateTimeNow());
    aiPromptRepository.save(aiPrompt);
  }

  @Timed("LLMPromptService.getPrompt")
  public AIPrompt getPrompt(Long promptId) {
    return aiPromptRepository
        .findById(promptId)
        .orElseThrow(() -> new AIException("Prompt not found: " + promptId));
  }

  @Timed("LLMPromptService.getAllActivePrompts")
  public List<AIPrompt> getAllActivePrompts() {
    return aiPromptRepository.findByDeletedFalse();
  }

  @Override
  @Transactional
  @Timed("LLMPromptService.createPromptContextMessage")
  public Long createPromptContextMessage(
      AIPromptContextMessageCreateRequest aiPromptContextMessageCreateRequest) {
    AIPromptContextMessage aiPromptContextMessage = new AIPromptContextMessage();
    OpenAIPromptContextMessageType messageType =
        OpenAIPromptContextMessageType.valueOf(
            aiPromptContextMessageCreateRequest.getMessageType().toUpperCase());
    aiPromptContextMessage.setContent(aiPromptContextMessageCreateRequest.getContent());
    aiPromptContextMessage.setAiPrompt(
        aiPromptRepository
            .findById(aiPromptContextMessageCreateRequest.getAiPromptId())
            .orElseThrow(
                () ->
                    new AIException(
                        "Prompt not found: "
                            + aiPromptContextMessageCreateRequest.getAiPromptId())));
    aiPromptContextMessageRepository
        .findByAiPromptIdAndOrderIndexAndDeleted(
            aiPromptContextMessageCreateRequest.getAiPromptId(),
            aiPromptContextMessageCreateRequest.getOrderIndex(),
            false)
        .ifPresent(
            existingMessage -> {
              throw new AIException(
                  "Prompt context message already exists for order index: "
                      + aiPromptContextMessageCreateRequest.getOrderIndex());
            });
    aiPromptContextMessage.setOrderIndex(aiPromptContextMessageCreateRequest.getOrderIndex());
    aiPromptContextMessage.setMessageType(messageType.getType());
    ZonedDateTime now = JSR310Migration.dateTimeNow();
    aiPromptContextMessage.setCreatedDate(now);
    aiPromptContextMessage.setLastModifiedDate(now);
    return aiPromptContextMessageRepository.save(aiPromptContextMessage).getId();
  }

  @Timed("LLMPromptService.deletePromptContextMessage")
  public void deletePromptContextMessage(Long promptMessageId) {
    AIPromptContextMessage aiPromptContextMessage =
        aiPromptContextMessageRepository
            .findById(promptMessageId)
            .orElseThrow(
                () -> new AIException("Prompt context message not found: " + promptMessageId));
    aiPromptContextMessage.setDeleted(true);
    aiPromptContextMessage.setLastModifiedDate(JSR310Migration.dateTimeNow());
    aiPromptContextMessageRepository.save(aiPromptContextMessage);
  }

  @Timed("LLMPromptService.getAllActivePromptsForRepository")
  public List<AIPrompt> getAllActivePromptsForRepository(String repositoryName) {
    Repository repository = getRepository(repositoryName);
    return aiPromptRepository.findByRepositoryIdAndDeletedFalse(repository.getId());
  }

  @Override
  @Timed("LLMPromptService.handleRepositoryLocalePromptOverrides")
  @Transactional
  public void createOrUpdateRepositoryLocaleTranslationPromptOverrides(
      String repositoryName, Set<String> bcp47Tags, Long aiPromptId, boolean disabled) {
    Repository repository = getRepository(repositoryName);
    AIPrompt aiPrompt =
        aiPromptRepository
            .findById(aiPromptId)
            .orElseThrow(() -> new AIException("Prompt not found: " + aiPromptId));

    validateProvidedLocaleTags(repositoryName, bcp47Tags, getLocalesForRepository(repository));
    bcp47Tags.forEach(
        bcp47Tag ->
            createOrUpdateRepositoryLocaleAiPrompt(
                bcp47Tag,
                getBcp47TagRepositoryLocaleAIPromptOverridesMap(bcp47Tags, repository),
                repository,
                aiPrompt,
                disabled));
  }

  @Override
  @Transactional
  @Timed("LLMPromptService.deleteRepositoryLocalePromptOverride")
  public void deleteRepositoryLocaleTranslationPromptOverride(
      String repositoryName, Set<String> bcp47Tags) {
    Repository repository = getRepository(repositoryName);
    validateProvidedLocaleTags(repositoryName, bcp47Tags, getLocalesForRepository(repository));
    repositoryLocaleAIPromptRepository.deleteAll(
        getBcp47TagRepositoryLocaleAIPromptOverridesMap(bcp47Tags, repository).values());
  }

  private Repository getRepository(String repositoryName) {
    Repository repository = repositoryRepository.findByName(repositoryName);
    if (repository == null) {
      logger.error("Repository not found: {}", repositoryName);
      throw new AIException("Repository not found: " + repositoryName);
    }
    return repository;
  }

  private Set<Locale> getLocalesForRepository(Repository repository) {
    return repository.getRepositoryLocales().stream()
        .map(RepositoryLocale::getLocale)
        .collect(Collectors.toSet());
  }

  private void validateProvidedLocaleTags(
      String repositoryName, Set<String> bcp47Tags, Set<Locale> locales) {
    List<String> invalidTags =
        bcp47Tags.stream()
            .filter(
                bcp47Tag ->
                    locales.stream().noneMatch(locale -> locale.getBcp47Tag().equals(bcp47Tag)))
            .toList();
    if (!invalidTags.isEmpty()) {
      throw new AIException(
          String.format(
              "Repository %s is not configured for the following BCP-47 tags: %s",
              repositoryName, invalidTags));
    }
  }

  private Map<String, RepositoryLocaleAIPrompt> getBcp47TagRepositoryLocaleAIPromptOverridesMap(
      Set<String> bcp47Tags, Repository repository) {
    return repositoryLocaleAIPromptRepository
        .getRepositoryLocaleTranslationPromptOverrides(repository)
        .orElseGet(List::of)
        .stream()
        .filter(
            repositoryLocaleAIPrompt ->
                bcp47Tags.contains(repositoryLocaleAIPrompt.getLocale().getBcp47Tag()))
        .collect(
            Collectors.toMap(
                repoLocaleAiPrompt -> repoLocaleAiPrompt.getLocale().getBcp47Tag(),
                Function.identity()));
  }

  private void createOrUpdateRepositoryLocaleAiPrompt(
      String bcp47Tag,
      Map<String, RepositoryLocaleAIPrompt> existingLocaleOverridesMap,
      Repository repository,
      AIPrompt aiPrompt,
      boolean disabled) {
    RepositoryLocaleAIPrompt repositoryLocaleAIPrompt = existingLocaleOverridesMap.get(bcp47Tag);
    if (repositoryLocaleAIPrompt == null) {
      repositoryLocaleAIPrompt = new RepositoryLocaleAIPrompt();
      repositoryLocaleAIPrompt.setRepository(repository);
      repositoryLocaleAIPrompt.setLocale(localeRepository.findByBcp47Tag(bcp47Tag));
    }
    repositoryLocaleAIPrompt.setAiPrompt(aiPrompt);
    repositoryLocaleAIPrompt.setDisabled(disabled);
    repositoryLocaleAIPromptRepository.save(repositoryLocaleAIPrompt);
  }
}

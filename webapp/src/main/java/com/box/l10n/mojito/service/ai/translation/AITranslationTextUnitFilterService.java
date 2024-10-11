package com.box.l10n.mojito.service.ai.translation;

import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.TMTextUnit;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AITranslationTextUnitFilterService {

  private static final Logger logger =
      LoggerFactory.getLogger(AITranslationTextUnitFilterService.class);
  private static final String HTML_TAG_REGEX = "<[^>]*>";
  private static final Pattern HTML_TAG_PATTERN = Pattern.compile(HTML_TAG_REGEX);

  protected Map<String, Pattern> excludePlaceholdersPatternMap;

  @Autowired AITranslationFilterConfiguration aiTranslationFilterConfiguration;

  @Autowired MeterRegistry meterRegistry;

  public boolean isTranslatable(TMTextUnit tmTextUnit, Repository repository) {
    boolean isTranslatable = true;

    if (repository == null) {
      logger.warn(
          "Repository is null for text unit with id: {}, filtering will be skipped",
          tmTextUnit.getId());
      return isTranslatable;
    }

    if (aiTranslationFilterConfiguration.getRepositoryConfig() == null
        || aiTranslationFilterConfiguration.getRepositoryConfig().get(repository.getName())
            == null) {
      logger.debug(
          "No configuration found for repository: {}, filtering will be skipped",
          repository.getName());
      return isTranslatable;
    }

    AITranslationFilterConfiguration.RepositoryConfig repositoryConfig =
        aiTranslationFilterConfiguration.getRepositoryConfig().get(repository.getName());

    if (repositoryConfig.shouldExcludePlurals()) {
      isTranslatable = !isPlural(tmTextUnit);
    }

    if (repositoryConfig.shouldExcludePlaceholders()) {
      isTranslatable = isTranslatable && !containsPlaceholder(repository.getName(), tmTextUnit);
    }

    if (repositoryConfig.shouldExcludeHtmlTags()) {
      isTranslatable = isTranslatable && !containsHtmlTag(tmTextUnit);
    }

    logger.debug(
        "Text unit with name: {}, should be translated: {}", tmTextUnit.getName(), isTranslatable);
    return isTranslatable;
  }

  private boolean containsPlaceholder(String repositoryName, TMTextUnit tmTextUnit) {
    Pattern pattern = excludePlaceholdersPatternMap.get(repositoryName);
    if (pattern != null) {
      Matcher matcher =
          excludePlaceholdersPatternMap.get(repositoryName).matcher(tmTextUnit.getContent());
      return matcher.find();
    } else {
      logger.debug("No exclude placeholders pattern found for repository: {}", repositoryName);
      return false;
    }
  }

  private boolean isPlural(TMTextUnit tmTextUnit) {
    return tmTextUnit.getPluralForm() != null;
  }

  private boolean containsHtmlTag(TMTextUnit tmTextUnit) {
    Matcher matcher = HTML_TAG_PATTERN.matcher(tmTextUnit.getContent());
    return matcher.find();
  }

  @PostConstruct
  public void init() {
    excludePlaceholdersPatternMap = Map.of();
    if (aiTranslationFilterConfiguration.getRepositoryConfig() != null) {
      for (Map.Entry<String, AITranslationFilterConfiguration.RepositoryConfig> entry :
          aiTranslationFilterConfiguration.getRepositoryConfig().entrySet()) {
        AITranslationFilterConfiguration.RepositoryConfig repositoryConfig = entry.getValue();
        if (repositoryConfig.shouldExcludePlaceholders()) {
          excludePlaceholdersPatternMap.put(
              entry.getKey(), Pattern.compile(repositoryConfig.getExcludePlaceholdersRegex()));
        }
      }
    }
  }
}

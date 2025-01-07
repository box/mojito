package com.box.l10n.mojito.service.ai.translation;

import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.TMTextUnit;
import com.google.common.collect.Maps;
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

    if (repository == null) {
      logger.warn(
          "Repository is null for text unit with id: {}, filtering will be skipped",
          tmTextUnit.getId());
      return true;
    }

    if (aiTranslationFilterConfiguration.getRepositoryConfig() == null
        || aiTranslationFilterConfiguration.getRepositoryConfig().get(repository.getName())
            == null) {
      logger.debug(
          "No configuration found for repository: {}, filtering will be skipped",
          repository.getName());
      return true;
    }

    AITranslationFilterConfiguration.RepositoryConfig repositoryConfig =
        aiTranslationFilterConfiguration.getRepositoryConfig().get(repository.getName());

    if (repositoryConfig.shouldExcludePlurals()) {
      if (isPlural(tmTextUnit)) {
        logger.debug(
            "Text unit with name: {}, is a plural, AI translation will be skipped",
            tmTextUnit.getName());
        return false;
      }
    }

    if (repositoryConfig.shouldExcludePlaceholders()) {
      if (containsPlaceholder(repository.getName(), tmTextUnit)) {
        logger.debug(
            "Text unit with name: {}, contains a placeholder, AI translation will be skipped",
            tmTextUnit.getName());
        return false;
      }
    }

    if (repositoryConfig.shouldExcludeHtmlTags()) {
      if (containsHtmlTag(tmTextUnit)) {
        logger.debug(
            "Text unit with name: {}, contains HTML tag, AI translation will be skipped",
            tmTextUnit.getName());
        return false;
      }
    }

    return true;
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
    excludePlaceholdersPatternMap = Maps.newHashMap();
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

package com.box.l10n.mojito.rest.script;

import com.box.l10n.mojito.rest.client.PollableTaskClient;
import com.box.l10n.mojito.rest.client.RepositoryClient;
import com.box.l10n.mojito.rest.client.TextUnitClient;
import com.box.l10n.mojito.rest.client.TextUnitClient.TextUnit;
import com.box.l10n.mojito.rest.client.TextUnitClient.TextUnitSearchBody;
import com.box.l10n.mojito.rest.client.exception.RepositoryNotFoundException;
import com.box.l10n.mojito.rest.entity.PollableTask;
import com.box.l10n.mojito.rest.resttemplate.AuthenticatedRestTemplateTest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@Configuration
@SpringBootTest(
    classes = {
      AuthenticatedRestTemplateTest.class,
      RepositoryClient.class,
      TextUnitClient.class,
      PollableTaskClient.class
    })
@TestPropertySource(locations = "file:/Users/ja/.l10n/config/script/application.properties")
@EnableConfigurationProperties
// @Ignore
public class LeverageScript {

  static Logger logger = LoggerFactory.getLogger(LeverageScript.class);

  @Autowired TextUnitClient textUnitClient;

  @Autowired PollableTaskClient pollableTaskClient;

  @Autowired RepositoryClient repositoryClient;

  @Test
  public void script() throws RepositoryNotFoundException {

    var targetRepository = "ios";
    var sourceRepository = "zzzzzzzzz_ios";
    var skipSourceEqTarget = true;
    var locales = List.of("fr");

    List<TextUnit> textUnitsToSave = new ArrayList<>();

    for (String locale : locales) {
      logger.info("Processing locale: {}", locale);
      List<TextUnit> untranslatedTextUnitsForLocale =
          getUntranslatedTextUnitsForLocale(targetRepository, locale);

      for (TextUnit textUnit : untranslatedTextUnitsForLocale) {
        List<TextUnit> existingTranslationWithSameSource =
            getTranslationsWithExactSource(sourceRepository, locale, textUnit.source());
        Optional<TextUnit> match =
            getMatchByNameAndComment(
                textUnit.source(), textUnit.comment(), existingTranslationWithSameSource);

        if (match.isPresent()) {
          logger.info(
              "Found a match (tuvid: {}) by name and comment for:\n {}\ntranslation is: {}",
              match.get().tmTextUnitVariantId(),
              textUnit.source(),
              match.get().target());
        } else {
          match = getMatchByNewest(existingTranslationWithSameSource);
          if (match.isPresent()) {
            logger.info(
                "Found match (tuvid: {}) by newest date for:\n {}\ntranslation is: {}",
                match.get().tmTextUnitVariantId(),
                textUnit.source(),
                match.get().target());
          }
        }

        if (match.isPresent()) {
          logger.info("found match for source: {}", textUnit.source());
          var translation = match.get();
          if (skipSourceEqTarget && translation.source().equals(translation.target())) {
            logger.info("skipping because source and target are the same");
          }
          textUnitsToSave.add(textUnit.withTarget(translation.target(), translation.status()));
        } else {
          logger.info("No match found for source: {}", textUnit.source());
        }
      }
    }

    TextUnitClient.ImportTextUnitsBatch importTextUnitsBatch =
        new TextUnitClient.ImportTextUnitsBatch(false, true, textUnitsToSave);
    PollableTask pollableTask = textUnitClient.importTextUnitBatch(importTextUnitsBatch);
    pollableTaskClient.waitForPollableTask(pollableTask.getId());
  }

  private List<TextUnit> getTranslationsWithExactSource(
      String repositoryName, String locale, String source) {
    TextUnitSearchBody textUnitSearchBody = new TextUnitSearchBody();
    textUnitSearchBody.setRepositoryNames(List.of(repositoryName));
    textUnitSearchBody.setLocaleTags(List.of(locale));
    textUnitSearchBody.setStatusFilter(TextUnitClient.StatusFilter.TRANSLATED);
    textUnitSearchBody.setSource(source);
    textUnitSearchBody.setLimit(20);
    return textUnitClient.searchTextUnits(textUnitSearchBody);
  }

  private List<TextUnit> getUntranslatedTextUnitsForLocale(String repositoryName, String locale) {
    TextUnitSearchBody textUnitSearchBody = new TextUnitSearchBody();
    textUnitSearchBody.setRepositoryNames(List.of(repositoryName));
    textUnitSearchBody.setLocaleTags(List.of(locale));
    textUnitSearchBody.setUsedFilter(TextUnitClient.UsedFilter.USED);
    textUnitSearchBody.setStatusFilter(TextUnitClient.StatusFilter.UNTRANSLATED);
    textUnitSearchBody.setLimit(1000);
    return textUnitClient.searchTextUnits(textUnitSearchBody);
  }

  private Optional<TextUnit> getMatchByNewest(List<TextUnit> candidates) {
    return candidates.stream().max(Comparator.comparingLong(TextUnit::createdDate));
  }

  private static Optional<TextUnit> getMatchByNameAndComment(
      String name, String comment, List<TextUnit> candidates) {
    return candidates.stream()
        .filter(m -> Objects.equals(name, m.name()) && Objects.equals(comment, m.comment()))
        .max(Comparator.comparingLong(TextUnit::createdDate));
  }
}

package com.box.l10n.mojito.cli.command;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.box.l10n.mojito.cli.command.param.Param;
import com.box.l10n.mojito.cli.console.ConsoleWriter;
import com.box.l10n.mojito.rest.client.RepositoryClient;
import com.box.l10n.mojito.rest.client.TextUnitClient;
import com.box.l10n.mojito.rest.entity.Locale;
import com.box.l10n.mojito.rest.entity.PollableTask;
import com.box.l10n.mojito.rest.entity.Repository;
import com.box.l10n.mojito.rest.entity.RepositoryLocale;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.Ansi.Color;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Command to populate untranslated string with translation from another repository translation
 * memory. This meant to process small volumes but can be helpful to combine the with the import
 * localized files command.
 *
 * <p>Matches the text units by the source. Use text unit name and comment match as priority (moved
 * assets), else just take the newest translation match. Has an option to not import string that
 * have same source and target to avoid copy translation that might be from importing partly
 * translated files. The drawback is that it could skip string that should be imported but the
 * benefits is that the localized files will be same regardless.
 *
 * <p>Limited to translating at most 1000 strings in a run
 *
 * @author jaurambault
 */
@Component
@Scope("prototype")
@Parameters(
    commandNames = {"repository-translate"},
    commandDescription =
        "Translate a repository using another repository TM. Only untranslated text units"
            + " will be processed, no overriding. Priorities text unit that have same and comment, else just look"
            + "at the newest match")
public class RepositoryTmTranslateCommand extends Command {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(RepositoryTmTranslateCommand.class);

  @Autowired ConsoleWriter consoleWriter;

  @Parameter(
      names = {Param.SOURCE_REPOSITORY_LONG, Param.SOURCE_REPOSITORY_SHORT},
      arity = 1,
      required = false,
      description = Param.SOURCE_REPOSITORY_DESCRIPTION)
  String sourceRepositoryParam;

  @Parameter(
      names = {Param.TARGET_REPOSITORY_LONG, Param.TARGET_REPOSITORY_SHORT},
      arity = 1,
      required = true,
      description = Param.TARGET_REPOSITORY_DESCRIPTION)
  String targetRepositoryParam;

  @Parameter(
      names = {"--import-source-equals-target"},
      arity = 0,
      description = "Import the translation if the source is equal to the target")
  boolean importSourceEqualsTargetParam = false;

  @Parameter(
      names = {Param.REPOSITORY_LOCALES_LONG, Param.REPOSITORY_LOCALES_SHORT},
      variableArity = true,
      required = false,
      description = "List of locales (bcp47 tags) to translate")
  List<String> localesParam = null;

  @Autowired CommandHelper commandHelper;

  @Autowired TextUnitClient textUnitClient;

  @Autowired RepositoryClient repositoryClient;

  @Override
  public void execute() throws CommandException {

    if (sourceRepositoryParam == null) {
      sourceRepositoryParam = targetRepositoryParam;
    }

    consoleWriter
        .newLine()
        .a("Translate repository: ")
        .fg(Color.CYAN)
        .a(targetRepositoryParam)
        .reset()
        .a(" with text units from repository: ")
        .fg(Color.CYAN)
        .a(sourceRepositoryParam)
        .println(2);

    Repository sourceRepository = commandHelper.findRepositoryByName(sourceRepositoryParam);
    Repository targetRepository = commandHelper.findRepositoryByName(targetRepositoryParam);

    List<String> locales = localesParam;
    if (locales == null) {
      locales =
          targetRepository.getRepositoryLocales().stream()
              .filter(rl -> rl.getParentLocale() != null)
              .map(RepositoryLocale::getLocale)
              .map(Locale::getBcp47Tag)
              .toList();
    }

    for (String locale : locales) {
      consoleWriter.a("Processing locale: ").fg(Color.MAGENTA).a(locale).println();
      List<TextUnitClient.TextUnit> textUnitsToSave = new ArrayList<>();

      List<TextUnitClient.TextUnit> untranslatedTextUnitsForLocale =
          getUntranslatedTextUnitsForLocale(targetRepository.getName(), locale);

      for (TextUnitClient.TextUnit textUnit : untranslatedTextUnitsForLocale) {
        consoleWriter.a("Processing source: ").fg(Color.CYAN).a(textUnit.source()).println();
        List<TextUnitClient.TextUnit> existingTranslationWithSameSource =
            getTranslationsWithExactSource(sourceRepository.getName(), locale, textUnit.source());

        Optional<TextUnitClient.TextUnit> match =
            getMatchByNameAndComment(
                textUnit.source(), textUnit.comment(), existingTranslationWithSameSource);
        if (match.isPresent()) {
          match = getMatchByNewest(existingTranslationWithSameSource);
        }

        if (match.isPresent()) {
          var translation = match.get();
          consoleWriter.a("Found match: ").fg(Color.CYAN).a(translation.target()).println();

          if (translation.source().equals(translation.target())) {
            if (importSourceEqualsTargetParam) {
              textUnitsToSave.add(textUnit.withTarget(translation.target(), translation.status()));
            } else {
              consoleWriter.a("Skip import, source = target").println();
            }
          } else {
            textUnitsToSave.add(textUnit.withTarget(translation.target(), translation.status()));
          }
        } else {
          consoleWriter.a("No match found").println();
        }
        consoleWriter.println();
      }

      consoleWriter
          .a("Saving translations for locale: ")
          .fg(Color.MAGENTA)
          .a(locale)
          .reset()
          .a("Text unit count: ")
          .fg(Color.CYAN)
          .a(textUnitsToSave.size())
          .println();
      TextUnitClient.ImportTextUnitsBatch importTextUnitsBatch =
          new TextUnitClient.ImportTextUnitsBatch(false, true, textUnitsToSave);
      PollableTask pollableTask = textUnitClient.importTextUnitBatch(importTextUnitsBatch);
      commandHelper.waitForPollableTask(pollableTask.getId());
    }
    consoleWriter.fg(Ansi.Color.GREEN).newLine().a("Finished").println(2);
  }

  private List<TextUnitClient.TextUnit> getTranslationsWithExactSource(
      String repositoryName, String locale, String source) {
    TextUnitClient.TextUnitSearchBody textUnitSearchBody = new TextUnitClient.TextUnitSearchBody();
    textUnitSearchBody.setRepositoryNames(List.of(repositoryName));
    textUnitSearchBody.setLocaleTags(List.of(locale));
    textUnitSearchBody.setStatusFilter(TextUnitClient.StatusFilter.TRANSLATED);
    textUnitSearchBody.setSource(source);
    textUnitSearchBody.setLimit(20);
    return textUnitClient.searchTextUnits(textUnitSearchBody);
  }

  private List<TextUnitClient.TextUnit> getUntranslatedTextUnitsForLocale(
      String repositoryName, String locale) {
    TextUnitClient.TextUnitSearchBody textUnitSearchBody = new TextUnitClient.TextUnitSearchBody();
    textUnitSearchBody.setRepositoryNames(List.of(repositoryName));
    textUnitSearchBody.setLocaleTags(List.of(locale));
    textUnitSearchBody.setUsedFilter(TextUnitClient.UsedFilter.USED);
    textUnitSearchBody.setStatusFilter(TextUnitClient.StatusFilter.UNTRANSLATED);
    textUnitSearchBody.setLimit(1000);
    return textUnitClient.searchTextUnits(textUnitSearchBody);
  }

  private Optional<TextUnitClient.TextUnit> getMatchByNewest(
      List<TextUnitClient.TextUnit> candidates) {
    return candidates.stream().max(Comparator.comparingLong(TextUnitClient.TextUnit::createdDate));
  }

  private static Optional<TextUnitClient.TextUnit> getMatchByNameAndComment(
      String name, String comment, List<TextUnitClient.TextUnit> candidates) {
    return candidates.stream()
        .filter(m -> Objects.equals(name, m.name()) && Objects.equals(comment, m.comment()))
        .max(Comparator.comparingLong(TextUnitClient.TextUnit::createdDate));
  }
}

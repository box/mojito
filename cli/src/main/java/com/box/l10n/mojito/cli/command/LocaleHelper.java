package com.box.l10n.mojito.cli.command;

import com.beust.jcommander.internal.Lists;
import com.box.l10n.mojito.cli.console.ConsoleWriter;
import com.box.l10n.mojito.rest.client.LocaleClient;
import com.box.l10n.mojito.rest.entity.Locale;
import com.box.l10n.mojito.rest.entity.Repository;
import com.box.l10n.mojito.rest.entity.RepositoryLocale;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.fusesource.jansi.Ansi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Common methods for locale management.
 *
 * @author jyi
 */
@Component
public class LocaleHelper {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(LocaleHelper.class);

  protected static final Pattern BCP47_TAG_BRACKET_PATTERN =
      Pattern.compile("\\((?<bcp47Tag>.*?)\\)");

  @Autowired ConsoleWriter consoleWriter;

  @Autowired LocaleClient localeClient;

  /**
   * Extract {@link RepositoryLocale} set from {@link RepoCreateCommand#encodedBcp47Tags} to prep
   * for {@link Repository} creation
   *
   * @return
   * @throws CommandException
   */
  protected Set<RepositoryLocale> extractRepositoryLocalesFromInput(
      List<String> encodedBcp47Tags, boolean doPrint) throws CommandException {
    Set<RepositoryLocale> repositoryLocales = new LinkedHashSet<>();

    if (encodedBcp47Tags != null) {
      List<Locale> locales = localeClient.getLocales();
      Map<String, Locale> localeMapByBcp47Tag = getLocaleMapByBcp47Tag(locales);

      for (String encodedBcp47Tag : encodedBcp47Tags) {
        RepositoryLocale repositoryLocale =
            getRepositoryLocaleFromEncodedBcp47Tag(localeMapByBcp47Tag, encodedBcp47Tag, doPrint);
        repositoryLocales.add(repositoryLocale);
      }
    }
    return repositoryLocales;
  }

  /**
   * Convert encoded Bcp47 Tag into {@link RepositoryLocale} with all the parent relationships and
   * {@link RepositoryLocale#toBeFullyTranslated} set
   *
   * @param encodedBcp47Tag
   * @return
   * @throws CommandException
   */
  protected RepositoryLocale getRepositoryLocaleFromEncodedBcp47Tag(
      Map<String, Locale> localeMapByBcp47Tag, String encodedBcp47Tag, boolean doPrint)
      throws CommandException {

    if (doPrint) {
      consoleWriter.a("Extracting locale: ").fg(Ansi.Color.MAGENTA).a(encodedBcp47Tag).println();
    }

    RepositoryLocale repositoryLocale = new RepositoryLocale();

    List<String> bcp47Tags = Lists.newArrayList(encodedBcp47Tag.split("->"));

    for (String bcp47Tag : bcp47Tags) {
      Matcher matcher = BCP47_TAG_BRACKET_PATTERN.matcher(bcp47Tag);
      if (matcher.find()) {
        repositoryLocale.setToBeFullyTranslated(false);
        bcp47Tag = matcher.group("bcp47Tag");
      }

      Locale locale = localeMapByBcp47Tag.get(bcp47Tag);

      if (locale == null) {
        throw new CommandException("Locale [" + bcp47Tag + "] does not exist in the system");
      }

      if (repositoryLocale.getLocale() == null) {
        repositoryLocale.setLocale(locale);
      } else {
        addLocaleAsTheLastParent(repositoryLocale, locale);
      }
    }

    if (doPrint) {
      printRepositoryLocale(repositoryLocale);
    }

    return repositoryLocale;
  }

  /**
   * Add locale to the end (last) of the parent hierarchy ({@link RepositoryLocale#parentLocale}) in
   * this {@link RepositoryLocale}
   *
   * @param repositoryLocale
   * @param parentLocale
   */
  protected void addLocaleAsTheLastParent(RepositoryLocale repositoryLocale, Locale parentLocale) {
    while (repositoryLocale.getParentLocale() != null) {
      repositoryLocale = repositoryLocale.getParentLocale();
    }

    RepositoryLocale parentRepoLocale = new RepositoryLocale();
    parentRepoLocale.setLocale(parentLocale);

    repositoryLocale.setParentLocale(parentRepoLocale);
  }

  protected void printRepositoryLocale(RepositoryLocale repositoryLocale) {
    consoleWriter
        .a("Extracted RepositoryLocale: ")
        .newLine()
        .fg(Ansi.Color.BLUE)
        .a("-- bcp47Tag = ")
        .fg(Ansi.Color.GREEN)
        .a(repositoryLocale.getLocale().getBcp47Tag())
        .newLine()
        .fg(Ansi.Color.BLUE)
        .a("-- isFullyTranslated = ")
        .fg(Ansi.Color.GREEN)
        .a(String.valueOf(repositoryLocale.isToBeFullyTranslated()))
        .newLine();

    while (repositoryLocale.getParentLocale() != null) {
      repositoryLocale = repositoryLocale.getParentLocale();
      consoleWriter
          .fg(Ansi.Color.BLUE)
          .a("-- parentLocale = ")
          .fg(Ansi.Color.GREEN)
          .a(repositoryLocale.getLocale().getBcp47Tag())
          .newLine();
    }

    consoleWriter.print();
  }

  /**
   * Transform locale list to map by Bcp47 Tag
   *
   * @param locales
   * @return
   */
  protected Map<String, Locale> getLocaleMapByBcp47Tag(List<Locale> locales) {
    Map<String, Locale> bcp47LocaleMap = new LinkedHashMap<>(locales.size());

    for (Locale locale : locales) {
      bcp47LocaleMap.put(locale.getBcp47Tag(), locale);
    }

    return bcp47LocaleMap;
  }
}

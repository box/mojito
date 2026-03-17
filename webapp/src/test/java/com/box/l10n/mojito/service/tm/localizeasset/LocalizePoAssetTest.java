package com.box.l10n.mojito.service.tm.localizeasset;

import static org.junit.Assert.assertEquals;

import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.RepositoryLocale;
import com.box.l10n.mojito.okapi.ImportTranslationsFromLocalizedAssetStep.StatusForEqualTarget;
import com.box.l10n.mojito.service.tm.search.StatusFilter;
import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcherParameters;
import java.util.List;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LocalizePoAssetTest extends LocalizeAssetTestBase {

  static Logger logger = LoggerFactory.getLogger(LocalizePoAssetTest.class);

  @Test
  public void testLocalizePoPluralJp() throws Exception {

    Repository repo = createRepository();
    RepositoryLocale repoLocale = addLocale(repo, "ja-JP");

    String assetContent =
        """
        msgid ""
        msgstr ""
        "Project-Id-Version: PACKAGE VERSION\\n"
        "Report-Msgid-Bugs-To: \\n"
        "POT-Creation-Date: 2017-09-15 11:53-0500\\n"
        "PO-Revision-Date: YEAR-MO-DA HO:MI+ZONE\\n"
        "Last-Translator: FULL NAME <EMAIL@ADDRESS>\\n"
        "Language-Team: LANGUAGE <LL@li.org>\\n"
        "MIME-Version: 1.0\\n"
        "Plural-Forms: nplurals=2; plural=(n != 1);\\n"
        "Content-Type: text/plain; charset=utf-8\\n"
        "Content-Transfer-Encoding: 8bit\\n"
        #. Comments
        #: core/logic/week_in_review_email_logic.py:49
        msgid "repin"
        msgid_plural "repins"
        msgstr[0] ""
        msgstr[1] ""
        """;

    String expectedLocalizedAsset =
        """
        msgid ""
        msgstr ""
        "Project-Id-Version: PACKAGE VERSION\\n"
        "Report-Msgid-Bugs-To: \\n"
        "POT-Creation-Date: 2017-09-15 11:53-0500\\n"
        "PO-Revision-Date: YEAR-MO-DA HO:MI+ZONE\\n"
        "Last-Translator: FULL NAME <EMAIL@ADDRESS>\\n"
        "Language-Team: LANGUAGE <LL@li.org>\\n"
        "MIME-Version: 1.0\\n"
        "Plural-Forms: nplurals=1; plural=0;\\n"
        "Content-Type: text/plain; charset=utf-8\\n"
        "Content-Transfer-Encoding: 8bit\\n"
        #. Comments
        #: core/logic/week_in_review_email_logic.py:49
        msgid "repin"
        msgid_plural "repins"
        msgstr[0] "repins"
        """;

    createAsset(repo, "messages.pot", assetContent);
    processAsset(repo, assetContent);

    List<TextUnitDTO> textUnitDTOs = searchTextUnits(repo);

    for (TextUnitDTO textUnitDTO : textUnitDTOs) {
      logger.debug("source=[{}]", textUnitDTO.getSource());
    }

    String localizedAsset = generateLocalized(assetContent, repoLocale, "ja-JP");
    logger.debug("localized=\n{}", localizedAsset);
    assertEquals(expectedLocalizedAsset, localizedAsset);

    String forImport =
        """
        msgid ""
        msgstr ""
        "Project-Id-Version: PACKAGE VERSION\\n"
        "Report-Msgid-Bugs-To: \\n"
        "POT-Creation-Date: 2017-09-15 11:53-0500\\n"
        "PO-Revision-Date: YEAR-MO-DA HO:MI+ZONE\\n"
        "Last-Translator: FULL NAME <EMAIL@ADDRESS>\\n"
        "Language-Team: LANGUAGE <LL@li.org>\\n"
        "MIME-Version: 1.0\\n"
        "Plural-Forms: nplurals=1; plural=0;\\n"
        "Content-Type: text/plain; charset=utf-8\\n"
        "Content-Transfer-Encoding: 8bit\\n"
        #. Comments
        #: core/logic/week_in_review_email_logic.py:49
        msgid "repin"
        msgid_plural "repins"
        msgstr[0] "repin-jp"
        """;

    importTranslations(repoLocale, forImport, StatusForEqualTarget.TRANSLATION_NEEDED);

    localizedAsset = generateLocalized(assetContent, repoLocale, "ja-JP");
    logger.debug("localized after import=\n{}", localizedAsset);
    assertEquals(forImport, localizedAsset);
  }

  @Test
  public void testLocalizePoRemoveUntranslated() throws Exception {

    Repository repo = createRepository();
    RepositoryLocale repoLocale = addLocale(repo, "ja-JP");

    String assetContent =
        """
        msgid ""
        msgstr ""
        "Project-Id-Version: PACKAGE VERSION\\n"
        "Report-Msgid-Bugs-To: \\n"
        "POT-Creation-Date: 2017-09-15 11:53-0500\\n"
        "PO-Revision-Date: YEAR-MO-DA HO:MI+ZONE\\n"
        "Last-Translator: FULL NAME <EMAIL@ADDRESS>\\n"
        "Language-Team: LANGUAGE <LL@li.org>\\n"
        "MIME-Version: 1.0\\n"
        "Plural-Forms: nplurals=2; plural=(n != 1);\\n"
        "Content-Type: text/plain; charset=utf-8\\n"
        "Content-Transfer-Encoding: 8bit\\n"


        #. Comments
        #: core/logic/week_in_review_email_logic.py:49
        msgid "repin"
        msgstr ""
        #. Description
        #: core/logic/week_in_review_email_logic.py:50
        msgid "description"
        msgstr ""
        """;

    String expectedLocalizedAsset =
        """
        msgid ""
        msgstr ""
        "Project-Id-Version: PACKAGE VERSION\\n"
        "Report-Msgid-Bugs-To: \\n"
        "POT-Creation-Date: 2017-09-15 11:53-0500\\n"
        "PO-Revision-Date: YEAR-MO-DA HO:MI+ZONE\\n"
        "Last-Translator: FULL NAME <EMAIL@ADDRESS>\\n"
        "Language-Team: LANGUAGE <LL@li.org>\\n"
        "MIME-Version: 1.0\\n"
        "Plural-Forms: nplurals=1; plural=0;\\n"
        "Content-Type: text/plain; charset=utf-8\\n"
        "Content-Transfer-Encoding: 8bit\\n"


        """;

    createAsset(repo, "messages.pot", assetContent);
    processAsset(repo, assetContent);

    String localizedAsset = generateLocalizedRemoveUntranslated(assetContent, repoLocale, "ja-JP");
    logger.debug("localized=\n{}\nEOL", localizedAsset);
    assertEquals(expectedLocalizedAsset, localizedAsset);

    String forImport =
        """
        msgid ""
        msgstr ""
        "Project-Id-Version: PACKAGE VERSION\\n"
        "Report-Msgid-Bugs-To: \\n"
        "POT-Creation-Date: 2017-09-15 11:53-0500\\n"
        "PO-Revision-Date: YEAR-MO-DA HO:MI+ZONE\\n"
        "Last-Translator: FULL NAME <EMAIL@ADDRESS>\\n"
        "Language-Team: LANGUAGE <LL@li.org>\\n"
        "MIME-Version: 1.0\\n"
        "Plural-Forms: nplurals=1; plural=0;\\n"
        "Content-Type: text/plain; charset=utf-8\\n"
        "Content-Transfer-Encoding: 8bit\\n"


        #. Comments
        #: core/logic/week_in_review_email_logic.py:49
        msgid "repin"
        msgstr "repin-jp"
        """;

    logger.debug("formimport=\n{}", forImport);

    importTranslations(repoLocale, forImport, StatusForEqualTarget.TRANSLATION_NEEDED);

    localizedAsset = generateLocalizedRemoveUntranslated(assetContent, repoLocale, "ja-JP");
    logger.info("localized after import=\n{}", localizedAsset);
    assertEquals(forImport, localizedAsset);
  }

  @Test
  public void testLocalizePoEscaping() throws Exception {

    Repository repo = createRepository();
    RepositoryLocale repoLocale = addLocale(repo, "ja-JP");

    String assetContent =
        """
        msgstr ""
        "Project-Id-Version: PACKAGE VERSION\\n"
        "Report-Msgid-Bugs-To: \\n"
        "POT-Creation-Date: 2017-09-15 11:53-0500\\n"
        "PO-Revision-Date: YEAR-MO-DA HO:MI+ZONE\\n"
        "Last-Translator: FULL NAME <EMAIL@ADDRESS>\\n"
        "Language-Team: LANGUAGE <LL@li.org>\\n"
        "MIME-Version: 1.0\\n"
        "Plural-Forms: nplurals=2; plural=(n != 1);\\n"
        "Content-Type: text/plain; charset=utf-8\\n"
        "Content-Transfer-Encoding: 8bit\\n"
        #. Comments
        #: core/logic/week_in_review_email_logic.py:49
        msgid "repin \\"{}\\""
        msgstr ""
        """;

    String expectedLocalizedAsset =
        """
        msgstr ""
        "Project-Id-Version: PACKAGE VERSION\\n"
        "Report-Msgid-Bugs-To: \\n"
        "POT-Creation-Date: 2017-09-15 11:53-0500\\n"
        "PO-Revision-Date: YEAR-MO-DA HO:MI+ZONE\\n"
        "Last-Translator: FULL NAME <EMAIL@ADDRESS>\\n"
        "Language-Team: LANGUAGE <LL@li.org>\\n"
        "MIME-Version: 1.0\\n"
        "Plural-Forms: nplurals=1; plural=0;\\n"
        "Content-Type: text/plain; charset=utf-8\\n"
        "Content-Transfer-Encoding: 8bit\\n"
        #. Comments
        #: core/logic/week_in_review_email_logic.py:49
        msgid "repin \\"{}\\""
        msgstr "repin \\"{}\\""
        """;

    createAsset(repo, "messages.pot", assetContent);
    processAsset(repo, assetContent);

    List<TextUnitDTO> textUnitDTOs = searchTextUnits(repo);

    assertEquals(1, textUnitDTOs.size());
    TextUnitDTO textUnitDTO = textUnitDTOs.get(0);
    assertEquals("repin \"{}\"", textUnitDTO.getName());
    assertEquals("repin \"{}\"", textUnitDTO.getSource());

    String localizedAsset = generateLocalized(assetContent, repoLocale, "ja-JP");
    logger.debug("localized=\n{}", localizedAsset);
    assertEquals(expectedLocalizedAsset, localizedAsset);

    String forImport =
        """
        msgstr ""
        "Project-Id-Version: PACKAGE VERSION\\n"
        "Report-Msgid-Bugs-To: \\n"
        "POT-Creation-Date: 2017-09-15 11:53-0500\\n"
        "PO-Revision-Date: YEAR-MO-DA HO:MI+ZONE\\n"
        "Last-Translator: FULL NAME <EMAIL@ADDRESS>\\n"
        "Language-Team: LANGUAGE <LL@li.org>\\n"
        "MIME-Version: 1.0\\n"
        "Plural-Forms: nplurals=1; plural=0;\\n"
        "Content-Type: text/plain; charset=utf-8\\n"
        "Content-Transfer-Encoding: 8bit\\n"
        #. Comments
        #: core/logic/week_in_review_email_logic.py:49
        msgid "repin \\"{}\\""
        msgstr "repin \\"{}\\" jp"
        """;

    importTranslations(repoLocale, forImport, StatusForEqualTarget.TRANSLATION_NEEDED);

    localizedAsset = generateLocalizedRemoveUntranslated(assetContent, repoLocale, "ja-JP");
    logger.debug("localized after import=\n{}", localizedAsset);
    assertEquals(forImport, localizedAsset);

    TextUnitSearcherParameters textUnitSearcherParameters = new TextUnitSearcherParameters();
    textUnitSearcherParameters.setRepositoryIds(repo.getId());
    textUnitSearcherParameters.setStatusFilter(StatusFilter.TRANSLATED);
    textUnitSearcherParameters.setLocaleId(repoLocale.getLocale().getId());
    textUnitDTOs = textUnitSearcher.search(textUnitSearcherParameters);

    assertEquals(1, textUnitDTOs.size());
    textUnitDTO = textUnitDTOs.get(0);
    assertEquals("repin \"{}\"", textUnitDTO.getName());
    assertEquals("repin \"{}\"", textUnitDTO.getSource());
    assertEquals("repin \"{}\" jp", textUnitDTO.getTarget());
  }

  @Test
  public void testLocalizePoPluralRu() throws Exception {

    Repository repo = createRepository();
    RepositoryLocale repoLocale = addLocale(repo, "ru-RU");

    String assetContent =
        """
        msgid ""
        msgstr ""
        "Project-Id-Version: PACKAGE VERSION\\n"
        "Report-Msgid-Bugs-To: \\n"
        "POT-Creation-Date: 2017-09-15 11:53-0500\\n"
        "PO-Revision-Date: YEAR-MO-DA HO:MI+ZONE\\n"
        "Last-Translator: FULL NAME <EMAIL@ADDRESS>\\n"
        "Language-Team: LANGUAGE <LL@li.org>\\n"
        "MIME-Version: 1.0\\n"
        "Plural-Forms: nplurals=2; plural=(n != 1);\\n"
        "Content-Type: text/plain; charset=utf-8\\n"
        "Content-Transfer-Encoding: 8bit\\n"
        #. Comments
        #: core/logic/week_in_review_email_logic.py:49
        msgid "repin"
        msgid_plural "repins"
        msgstr[0] ""
        msgstr[1] ""
        """;

    String expectedLocalizedAsset =
        """
        msgid ""
        msgstr ""
        "Project-Id-Version: PACKAGE VERSION\\n"
        "Report-Msgid-Bugs-To: \\n"
        "POT-Creation-Date: 2017-09-15 11:53-0500\\n"
        "PO-Revision-Date: YEAR-MO-DA HO:MI+ZONE\\n"
        "Last-Translator: FULL NAME <EMAIL@ADDRESS>\\n"
        "Language-Team: LANGUAGE <LL@li.org>\\n"
        "MIME-Version: 1.0\\n"
        "Plural-Forms: nplurals=3; plural=n%10==1 && n%100!=11 ? 0 : n%10>=2 && n%10<=4 && (n%100<10 || n%100>=20) ? 1 : 2;\\n"
        "Content-Type: text/plain; charset=utf-8\\n"
        "Content-Transfer-Encoding: 8bit\\n"
        #. Comments
        #: core/logic/week_in_review_email_logic.py:49
        msgid "repin"
        msgid_plural "repins"
        msgstr[0] "repin"
        msgstr[1] "repins"
        msgstr[2] "repins"
        """;

    createAsset(repo, "messages.pot", assetContent);
    processAsset(repo, assetContent);

    List<TextUnitDTO> textUnitDTOs = searchTextUnits(repo);

    for (TextUnitDTO textUnitDTO : textUnitDTOs) {
      logger.debug("source=[{}]", textUnitDTO.getSource());
    }

    //        assertEquals("Hello, %1$s! You have <b>%2$d new messages</b>.",
    // textUnitDTO.getSource());
    String localizedAsset = generateLocalized(assetContent, repoLocale, "ru-RU");
    logger.debug("localized=\n{}", localizedAsset);
    assertEquals(expectedLocalizedAsset, localizedAsset);

    String forImport =
        """
        msgid ""
        msgstr ""
        "Project-Id-Version: PACKAGE VERSION\\n"
        "Report-Msgid-Bugs-To: \\n"
        "POT-Creation-Date: 2017-09-15 11:53-0500\\n"
        "PO-Revision-Date: YEAR-MO-DA HO:MI+ZONE\\n"
        "Last-Translator: FULL NAME <EMAIL@ADDRESS>\\n"
        "Language-Team: LANGUAGE <LL@li.org>\\n"
        "MIME-Version: 1.0\\n"
        "Plural-Forms: nplurals=3; plural=n%10==1 && n%100!=11 ? 0 : n%10>=2 && n%10<=4 && (n%100<10 || n%100>=20) ? 1 : 2;\\n"
        "Content-Type: text/plain; charset=utf-8\\n"
        "Content-Transfer-Encoding: 8bit\\n"
        #. Comments
        #: core/logic/week_in_review_email_logic.py:49
        msgid "repin"
        msgid_plural "repins"
        msgstr[0] "repin-ru"
        msgstr[1] "repins-ru-1"
        msgstr[2] "repins-ru-2"
        """;

    importTranslations(repoLocale, forImport, StatusForEqualTarget.TRANSLATION_NEEDED);

    localizedAsset = generateLocalized(assetContent, repoLocale, "ru-RU");
    logger.debug("localized after import=\n{}", localizedAsset);

    assertEquals(forImport, localizedAsset);
  }

  @Test
  public void testLocalizePoPluralCs() throws Exception {

    Repository repo = createRepository();
    RepositoryLocale repoLocale = addLocale(repo, "cs-CZ");

    String assetContent =
        """
        msgid ""
        msgstr ""
        "Project-Id-Version: PACKAGE VERSION\\n"
        "Report-Msgid-Bugs-To: \\n"
        "POT-Creation-Date: 2017-09-15 11:53-0500\\n"
        "PO-Revision-Date: YEAR-MO-DA HO:MI+ZONE\\n"
        "Last-Translator: FULL NAME <EMAIL@ADDRESS>\\n"
        "Language-Team: LANGUAGE <LL@li.org>\\n"
        "MIME-Version: 1.0\\n"
        "Plural-Forms: nplurals=2; plural=(n != 1);\\n"
        "Content-Type: text/plain; charset=utf-8\\n"
        "Content-Transfer-Encoding: 8bit\\n"
        #. Comments
        #: core/logic/week_in_review_email_logic.py:49
        msgid "repin"
        msgid_plural "repins"
        msgstr[0] ""
        msgstr[1] ""
        """;

    String expectedLocalizedAsset =
        """
        msgid ""
        msgstr ""
        "Project-Id-Version: PACKAGE VERSION\\n"
        "Report-Msgid-Bugs-To: \\n"
        "POT-Creation-Date: 2017-09-15 11:53-0500\\n"
        "PO-Revision-Date: YEAR-MO-DA HO:MI+ZONE\\n"
        "Last-Translator: FULL NAME <EMAIL@ADDRESS>\\n"
        "Language-Team: LANGUAGE <LL@li.org>\\n"
        "MIME-Version: 1.0\\n"
        "Plural-Forms: nplurals=3; plural=(n==1) ? 0 : (n>=2 && n<=4) ? 1 : 2;\\n"
        "Content-Type: text/plain; charset=utf-8\\n"
        "Content-Transfer-Encoding: 8bit\\n"
        #. Comments
        #: core/logic/week_in_review_email_logic.py:49
        msgid "repin"
        msgid_plural "repins"
        msgstr[0] "repin"
        msgstr[1] "repins"
        msgstr[2] "repins"
        """;

    createAsset(repo, "messages.pot", assetContent);
    processAsset(repo, assetContent);

    List<TextUnitDTO> textUnitDTOs = searchTextUnits(repo);

    for (TextUnitDTO textUnitDTO : textUnitDTOs) {
      logger.debug("source=[{}]", textUnitDTO.getSource());
    }

    String localizedAsset = generateLocalized(assetContent, repoLocale, "cs-CZ");
    logger.debug("localized=\n{}", localizedAsset);
    assertEquals(expectedLocalizedAsset, localizedAsset);

    String forImport =
        """
        msgid ""
        msgstr ""
        "Project-Id-Version: PACKAGE VERSION\\n"
        "Report-Msgid-Bugs-To: \\n"
        "POT-Creation-Date: 2017-09-15 11:53-0500\\n"
        "PO-Revision-Date: YEAR-MO-DA HO:MI+ZONE\\n"
        "Last-Translator: FULL NAME <EMAIL@ADDRESS>\\n"
        "Language-Team: LANGUAGE <LL@li.org>\\n"
        "MIME-Version: 1.0\\n"
        "Plural-Forms: nplurals=3; plural=(n==1) ? 0 : (n>=2 && n<=4) ? 1 : 2;\\n"
        "Content-Type: text/plain; charset=utf-8\\n"
        "Content-Transfer-Encoding: 8bit\\n"
        #. Comments
        #: core/logic/week_in_review_email_logic.py:49
        msgid "repin"
        msgid_plural "repins"
        msgstr[0] "repin-cs"
        msgstr[1] "repins-cz-1"
        msgstr[2] "repins-cz-2"
        """;

    importTranslations(repoLocale, forImport, StatusForEqualTarget.TRANSLATION_NEEDED);

    localizedAsset = generateLocalized(assetContent, repoLocale, "cs-CZ");
    logger.debug("localized after import=\n{}", localizedAsset);

    assertEquals(forImport, localizedAsset);
  }

  @Test
  public void testLocalizePoPluralAr() throws Exception {

    Repository repo = createRepository();
    RepositoryLocale repoLocale = addLocale(repo, "ar-SA");

    String assetContent =
        """
        msgid ""
        msgstr ""
        "Project-Id-Version: PACKAGE VERSION\\n"
        "Report-Msgid-Bugs-To: \\n"
        "POT-Creation-Date: 2017-09-15 11:53-0500\\n"
        "PO-Revision-Date: YEAR-MO-DA HO:MI+ZONE\\n"
        "Last-Translator: FULL NAME <EMAIL@ADDRESS>\\n"
        "Language-Team: LANGUAGE <LL@li.org>\\n"
        "MIME-Version: 1.0\\n"
        "Plural-Forms: nplurals=2; plural=(n != 1);\\n"
        "Content-Type: text/plain; charset=utf-8\\n"
        "Content-Transfer-Encoding: 8bit\\n"
        #. Comments
        #: core/logic/week_in_review_email_logic.py:49
        msgid "repin \\"{placeholder}\\""
        msgid_plural "repins \\"{placeholder}\\""
        msgstr[0] ""
        msgstr[1] ""
        """;

    String expectedLocalizedAsset =
        """
        msgid ""
        msgstr ""
        "Project-Id-Version: PACKAGE VERSION\\n"
        "Report-Msgid-Bugs-To: \\n"
        "POT-Creation-Date: 2017-09-15 11:53-0500\\n"
        "PO-Revision-Date: YEAR-MO-DA HO:MI+ZONE\\n"
        "Last-Translator: FULL NAME <EMAIL@ADDRESS>\\n"
        "Language-Team: LANGUAGE <LL@li.org>\\n"
        "MIME-Version: 1.0\\n"
        "Plural-Forms: nplurals=6; plural=n==0 ? 0 : n==1 ? 1 : n==2 ? 2 : n%100>=3 && n%100<=10 ? 3 : n%100>=11 ? 4 : 5;\\n"
        "Content-Type: text/plain; charset=utf-8\\n"
        "Content-Transfer-Encoding: 8bit\\n"
        #. Comments
        #: core/logic/week_in_review_email_logic.py:49
        msgid "repin \\"{placeholder}\\""
        msgid_plural "repins \\"{placeholder}\\""
        msgstr[0] "repins \\"{placeholder}\\""
        msgstr[1] "repin \\"{placeholder}\\""
        msgstr[2] "repins \\"{placeholder}\\""
        msgstr[3] "repins \\"{placeholder}\\""
        msgstr[4] "repins \\"{placeholder}\\""
        msgstr[5] "repins \\"{placeholder}\\""
        """;

    createAsset(repo, "messages.pot", assetContent);
    processAsset(repo, assetContent);

    List<TextUnitDTO> textUnitDTOs = searchTextUnits(repo);

    for (TextUnitDTO textUnitDTO : textUnitDTOs) {
      logger.debug("source=[{}]", textUnitDTO.getSource());
    }

    String localizedAsset = generateLocalized(assetContent, repoLocale, "ar-SA");
    logger.debug("localized=\n{}", localizedAsset);
    assertEquals(expectedLocalizedAsset, localizedAsset);

    String forImport =
        """
        msgid ""
        msgstr ""
        "Project-Id-Version: PACKAGE VERSION\\n"
        "Report-Msgid-Bugs-To: \\n"
        "POT-Creation-Date: 2017-09-15 11:53-0500\\n"
        "PO-Revision-Date: YEAR-MO-DA HO:MI+ZONE\\n"
        "Last-Translator: FULL NAME <EMAIL@ADDRESS>\\n"
        "Language-Team: LANGUAGE <LL@li.org>\\n"
        "MIME-Version: 1.0\\n"
        "Plural-Forms: nplurals=6; plural=n==0 ? 0 : n==1 ? 1 : n==2 ? 2 : n%100>=3 && n%100<=10 ? 3 : n%100>=11 ? 4 : 5;\\n"
        "Content-Type: text/plain; charset=utf-8\\n"
        "Content-Transfer-Encoding: 8bit\\n"
        #. Comments
        #: core/logic/week_in_review_email_logic.py:49
        msgid "repin \\"{placeholder}\\""
        msgid_plural "repins \\"{placeholder}\\""
        msgstr[0] "repins \\"{placeholder}\\"-ar-0"
        msgstr[1] "repins \\"{placeholder}\\"-ar-1"
        msgstr[2] "repins \\"{placeholder}\\"-ar-2"
        msgstr[3] "repins \\"{placeholder}\\"-ar-3"
        msgstr[4] "repins \\"{placeholder}\\"-ar-4"
        msgstr[5] "repins \\"{placeholder}\\"-ar-5"
        """;

    importTranslations(repoLocale, forImport, StatusForEqualTarget.TRANSLATION_NEEDED);

    localizedAsset = generateLocalized(assetContent, repoLocale, "ar-AR");
    logger.debug("localized after import=\n{}", localizedAsset);

    assertEquals(forImport, localizedAsset);
  }

  @Test
  public void testImportLocalizedAssetPoPluralSinglePlural() throws Exception {
    repository = createRepository();
    RepositoryLocale repoLocale = addLocale(repository, "ja-JP");

    String assetContent =
        """
        # SOME DESCRIPTIVE TITLE.
        # Copyright (C) YEAR THE PACKAGE'S COPYRIGHT HOLDER
        # This file is distributed under the same license as the PACKAGE package.
        # FIRST AUTHOR <EMAIL@ADDRESS>, YEAR.
        #
        #, fuzzy
        msgid ""
        msgstr ""
        "Project-Id-Version: PACKAGE VERSION\\n"
        "Report-Msgid-Bugs-To: \\n"
        "POT-Creation-Date: 2017-02-24 11:50-0800\\n"
        "PO-Revision-Date: YEAR-MO-DA HO:MI+ZONE\\n"
        "Last-Translator: FULL NAME <EMAIL@ADDRESS>\\n"
        "Language-Team: LANGUAGE <LL@li.org>\\n"
        "Language: \\n"
        "MIME-Version: 1.0\\n"
        "Plural-Forms: nplurals=2; plural=(n != 1);\\n"
        "Content-Type: text/plain; charset=utf-8\\n"
        "Content-Transfer-Encoding: 8bit\\n"
        \s
        \s\s\s
        #. Test plural
        #: file.js:20
        msgctxt "car"
        msgid "There is {number} car"
        msgid_plural "There are {number} cars"
        msgstr[0] ""
        msgstr[1] ""

        #. Test plural okapi bug
        #: file.js:24
        msgctxt "testpluralokapibug"
        msgid "test okapi bug"
        msgstr ""
        """;
    createAsset(repository, "messages.pot", assetContent);
    processAsset(repository, assetContent);

    String localizedAssetContent =
        """
        # SOME DESCRIPTIVE TITLE.
        # Copyright (C) YEAR THE PACKAGE'S COPYRIGHT HOLDER
        # This file is distributed under the same license as the PACKAGE package.
        # FIRST AUTHOR <EMAIL@ADDRESS>, YEAR.
        #
        #, fuzzy
        msgid ""
        msgstr ""
        "Project-Id-Version: PACKAGE VERSION\\n"
        "Report-Msgid-Bugs-To: \\n"
        "POT-Creation-Date: 2017-02-24 11:50-0800\\n"
        "PO-Revision-Date: YEAR-MO-DA HO:MI+ZONE\\n"
        "Last-Translator: FULL NAME <EMAIL@ADDRESS>\\n"
        "Language-Team: LANGUAGE <LL@li.org>\\n"
        "Language: \\n"
        "MIME-Version: 1.0\\n"
        "Plural-Forms: nplurals=1; plural=0;\\n"
        "Content-Type: text/plain; charset=utf-8\\n"
        "Content-Transfer-Encoding: 8bit\\n"
        \s\s
        \s\s\s
        #. Test plural
        #: file.js:20
        msgctxt "car"
        msgid "There is {number} car"
        msgid_plural "There are {number} cars"
        msgstr[0] "There is {number} car"

        #. Test plural okapi bug
        #: file.js:24
        msgctxt "testpluralokapibug"
        msgid "test okapi bug"
        msgstr "jp test okapi bug"

        """;
    importTranslations(repoLocale, localizedAssetContent, StatusForEqualTarget.APPROVED);
  }

  @Test
  public void testLocalizePoEscapedBackslashNewlineTab() throws Exception {
    // based on:
    // https://gitlab.com/okapiframework/Okapi/-/blob/v1.45.0/okapi/filters/po/src/test/java/net/sf/okapi/filters/po/POFilterTest.java#L241-251

    Repository repo = createRepository();
    RepositoryLocale repoLocale = addLocale(repo, "ja-JP");

    String assetContent =
        """
        msgid ""
        msgstr ""
        "Project-Id-Version: PACKAGE VERSION\\n"
        "Report-Msgid-Bugs-To: \\n"
        "POT-Creation-Date: 2017-09-15 11:53-0500\\n"
        "PO-Revision-Date: YEAR-MO-DA HO:MI+ZONE\\n"
        "Last-Translator: FULL NAME <EMAIL@ADDRESS>\\n"
        "Language-Team: LANGUAGE <LL@li.org>\\n"
        "MIME-Version: 1.0\\n"
        "Plural-Forms: nplurals=2; plural=(n != 1);\\n"
        "Content-Type: text/plain; charset=utf-8\\n"
        "Content-Transfer-Encoding: 8bit\\n"
        #. Comments
        #: src/main.py:10
        msgid "Text \\\\ and \\" and \\n and \\t"
        msgstr ""
        """;

    String expectedLocalizedAsset =
        """
        msgid ""
        msgstr ""
        "Project-Id-Version: PACKAGE VERSION\\n"
        "Report-Msgid-Bugs-To: \\n"
        "POT-Creation-Date: 2017-09-15 11:53-0500\\n"
        "PO-Revision-Date: YEAR-MO-DA HO:MI+ZONE\\n"
        "Last-Translator: FULL NAME <EMAIL@ADDRESS>\\n"
        "Language-Team: LANGUAGE <LL@li.org>\\n"
        "MIME-Version: 1.0\\n"
        "Plural-Forms: nplurals=1; plural=0;\\n"
        "Content-Type: text/plain; charset=utf-8\\n"
        "Content-Transfer-Encoding: 8bit\\n"
        #. Comments
        #: src/main.py:10
        msgid "Text \\\\ and \\" and \\n and \\t"
        msgstr "Text \\\\ and \\" and \\n and \\t"
        """;

    createAsset(repo, "messages.pot", assetContent);
    processAsset(repo, assetContent);

    List<TextUnitDTO> textUnitDTOs = searchTextUnits(repo);

    assertEquals(1, textUnitDTOs.size());
    TextUnitDTO textUnitDTO = textUnitDTOs.get(0);
    assertEquals("Text \\ and \" and \n and \t", textUnitDTO.getName());
    assertEquals("Text \\ and \" and \n and \t", textUnitDTO.getSource());

    String localizedAsset = generateLocalized(assetContent, repoLocale, "ja-JP");
    logger.debug("localized=\n{}", localizedAsset);
    assertEquals(expectedLocalizedAsset, localizedAsset);

    String forImport =
        """
        msgid ""
        msgstr ""
        "Project-Id-Version: PACKAGE VERSION\\n"
        "Report-Msgid-Bugs-To: \\n"
        "POT-Creation-Date: 2017-09-15 11:53-0500\\n"
        "PO-Revision-Date: YEAR-MO-DA HO:MI+ZONE\\n"
        "Last-Translator: FULL NAME <EMAIL@ADDRESS>\\n"
        "Language-Team: LANGUAGE <LL@li.org>\\n"
        "MIME-Version: 1.0\\n"
        "Plural-Forms: nplurals=1; plural=0;\\n"
        "Content-Type: text/plain; charset=utf-8\\n"
        "Content-Transfer-Encoding: 8bit\\n"
        #. Comments
        #: src/main.py:10
        msgid "Text \\\\ and \\" and \\n and \\t"
        msgstr "Text \\\\ and \\" and \\n and \\t jp"
        """;

    importTranslations(repoLocale, forImport, StatusForEqualTarget.TRANSLATION_NEEDED);

    localizedAsset = generateLocalized(assetContent, repoLocale, "ja-JP");
    logger.debug("localized after import=\n{}", localizedAsset);
    assertEquals(forImport, localizedAsset);

    TextUnitSearcherParameters textUnitSearcherParameters = new TextUnitSearcherParameters();
    textUnitSearcherParameters.setRepositoryIds(repo.getId());
    textUnitSearcherParameters.setStatusFilter(StatusFilter.TRANSLATED);
    textUnitSearcherParameters.setLocaleId(repoLocale.getLocale().getId());
    textUnitDTOs = textUnitSearcher.search(textUnitSearcherParameters);

    assertEquals(1, textUnitDTOs.size());
    textUnitDTO = textUnitDTOs.get(0);
    assertEquals("Text \\ and \" and \n and \t", textUnitDTO.getName());
    assertEquals("Text \\ and \" and \n and \t", textUnitDTO.getSource());
    assertEquals("Text \\ and \" and \n and \t jp", textUnitDTO.getTarget());
  }

  @Test
  public void testLocalizePoUnescapedRewrite() throws Exception {
    // based on:
    // https://gitlab.com/okapiframework/Okapi/-/blob/v1.45.0/okapi/filters/po/src/test/java/net/sf/okapi/filters/po/POFilterTest.java#L308-312

    // Verifies parity with how Okapi handles malformed escaping in PO files

    Repository repo = createRepository();
    RepositoryLocale repoLocale = addLocale(repo, "ja-JP");

    // Malformed PO: unescaped quote in middle of msgid string.
    // PO Parser uses lastIndexOf('"') to find the closing delimiter,
    // so given a PO snippet:
    // `msgid "A " and a \"`
    // the parsed string content after unescaping is:
    // `A " and a \`
    // (both an unescaped inner quote and the single trailing backslash are treated as literals)
    String assetContent =
        """
        msgid ""
        msgstr ""
        "Project-Id-Version: PACKAGE VERSION\\n"
        "Report-Msgid-Bugs-To: \\n"
        "POT-Creation-Date: 2017-09-15 11:53-0500\\n"
        "PO-Revision-Date: YEAR-MO-DA HO:MI+ZONE\\n"
        "Last-Translator: FULL NAME <EMAIL@ADDRESS>\\n"
        "Language-Team: LANGUAGE <LL@li.org>\\n"
        "MIME-Version: 1.0\\n"
        "Plural-Forms: nplurals=2; plural=(n != 1);\\n"
        "Content-Type: text/plain; charset=utf-8\\n"
        "Content-Transfer-Encoding: 8bit\\n"
        msgid "A " and a \\"
        msgstr ""
        """;

    // On localization (source copied to target), encoder should properly re-escape:
    // `msgstr "A \" and a \\"
    String expectedLocalizedAsset =
        """
        msgid ""
        msgstr ""
        "Project-Id-Version: PACKAGE VERSION\\n"
        "Report-Msgid-Bugs-To: \\n"
        "POT-Creation-Date: 2017-09-15 11:53-0500\\n"
        "PO-Revision-Date: YEAR-MO-DA HO:MI+ZONE\\n"
        "Last-Translator: FULL NAME <EMAIL@ADDRESS>\\n"
        "Language-Team: LANGUAGE <LL@li.org>\\n"
        "MIME-Version: 1.0\\n"
        "Plural-Forms: nplurals=1; plural=0;\\n"
        "Content-Type: text/plain; charset=utf-8\\n"
        "Content-Transfer-Encoding: 8bit\\n"
        msgid "A " and a \\"
        msgstr "A \\" and a \\\\"
        """;

    createAsset(repo, "messages.pot", assetContent);
    processAsset(repo, assetContent);

    List<TextUnitDTO> textUnitDTOs = searchTextUnits(repo);

    assertEquals(1, textUnitDTOs.size());
    TextUnitDTO textUnitDTO = textUnitDTOs.get(0);
    assertEquals(
        """
            A " and a \\""",
        textUnitDTO.getName());
    assertEquals(
        """
            A " and a \\""",
        textUnitDTO.getSource());

    String localizedAsset = generateLocalized(assetContent, repoLocale, "ja-JP");
    logger.debug("localized=\n{}", localizedAsset);
    assertEquals(expectedLocalizedAsset, localizedAsset);
  }
}

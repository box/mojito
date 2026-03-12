package com.box.l10n.mojito.service.tm.filetype;

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

public class TMServicePoTest extends TMServiceFileTypeTestBase {

  static Logger logger = LoggerFactory.getLogger(TMServicePoTest.class);

  @Test
  public void testLocalizePoPluralJp() throws Exception {

    Repository repo = createRepository();
    RepositoryLocale repoLocale = addLocale(repo, "ja-JP");

    String assetContent =
        "msgid \"\"\n"
            + "msgstr \"\"\n"
            + "\"Project-Id-Version: PACKAGE VERSION\\n\"\n"
            + "\"Report-Msgid-Bugs-To: \\n\"\n"
            + "\"POT-Creation-Date: 2017-09-15 11:53-0500\\n\"\n"
            + "\"PO-Revision-Date: YEAR-MO-DA HO:MI+ZONE\\n\"\n"
            + "\"Last-Translator: FULL NAME <EMAIL@ADDRESS>\\n\"\n"
            + "\"Language-Team: LANGUAGE <LL@li.org>\\n\"\n"
            + "\"MIME-Version: 1.0\\n\"\n"
            + "\"Plural-Forms: nplurals=2; plural=(n != 1);\\n\"\n"
            + "\"Content-Type: text/plain; charset=utf-8\\n\"\n"
            + "\"Content-Transfer-Encoding: 8bit\\n\"\n"
            + "#. Comments\n"
            + "#: core/logic/week_in_review_email_logic.py:49\n"
            + "msgid \"repin\"\n"
            + "msgid_plural \"repins\"\n"
            + "msgstr[0] \"\"\n"
            + "msgstr[1] \"\"";

    String expectedLocalizedAsset =
        "msgid \"\"\n"
            + "msgstr \"\"\n"
            + "\"Project-Id-Version: PACKAGE VERSION\\n\"\n"
            + "\"Report-Msgid-Bugs-To: \\n\"\n"
            + "\"POT-Creation-Date: 2017-09-15 11:53-0500\\n\"\n"
            + "\"PO-Revision-Date: YEAR-MO-DA HO:MI+ZONE\\n\"\n"
            + "\"Last-Translator: FULL NAME <EMAIL@ADDRESS>\\n\"\n"
            + "\"Language-Team: LANGUAGE <LL@li.org>\\n\"\n"
            + "\"MIME-Version: 1.0\\n\"\n"
            + "\"Plural-Forms: nplurals=1; plural=0;\\n\"\n"
            + "\"Content-Type: text/plain; charset=utf-8\\n\"\n"
            + "\"Content-Transfer-Encoding: 8bit\\n\"\n"
            + "#. Comments\n"
            + "#: core/logic/week_in_review_email_logic.py:49\n"
            + "msgid \"repin\"\n"
            + "msgid_plural \"repins\"\n"
            + "msgstr[0] \"repins\"\n";

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
        "msgid \"\"\n"
            + "msgstr \"\"\n"
            + "\"Project-Id-Version: PACKAGE VERSION\\n\"\n"
            + "\"Report-Msgid-Bugs-To: \\n\"\n"
            + "\"POT-Creation-Date: 2017-09-15 11:53-0500\\n\"\n"
            + "\"PO-Revision-Date: YEAR-MO-DA HO:MI+ZONE\\n\"\n"
            + "\"Last-Translator: FULL NAME <EMAIL@ADDRESS>\\n\"\n"
            + "\"Language-Team: LANGUAGE <LL@li.org>\\n\"\n"
            + "\"MIME-Version: 1.0\\n\"\n"
            + "\"Plural-Forms: nplurals=1; plural=0;\\n\"\n"
            + "\"Content-Type: text/plain; charset=utf-8\\n\"\n"
            + "\"Content-Transfer-Encoding: 8bit\\n\"\n"
            + "#. Comments\n"
            + "#: core/logic/week_in_review_email_logic.py:49\n"
            + "msgid \"repin\"\n"
            + "msgid_plural \"repins\"\n"
            + "msgstr[0] \"repin-jp\"\n";

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
        "msgid \"\"\n"
            + "msgstr \"\"\n"
            + "\"Project-Id-Version: PACKAGE VERSION\\n\"\n"
            + "\"Report-Msgid-Bugs-To: \\n\"\n"
            + "\"POT-Creation-Date: 2017-09-15 11:53-0500\\n\"\n"
            + "\"PO-Revision-Date: YEAR-MO-DA HO:MI+ZONE\\n\"\n"
            + "\"Last-Translator: FULL NAME <EMAIL@ADDRESS>\\n\"\n"
            + "\"Language-Team: LANGUAGE <LL@li.org>\\n\"\n"
            + "\"MIME-Version: 1.0\\n\"\n"
            + "\"Plural-Forms: nplurals=2; plural=(n != 1);\\n\"\n"
            + "\"Content-Type: text/plain; charset=utf-8\\n\"\n"
            + "\"Content-Transfer-Encoding: 8bit\\n\"\n\n\n"
            + "#. Comments\n"
            + "#: core/logic/week_in_review_email_logic.py:49\n"
            + "msgid \"repin\"\n"
            + "msgstr \"\"\n"
            + "#. Description\n"
            + "#: core/logic/week_in_review_email_logic.py:50\n"
            + "msgid \"description\"\n"
            + "msgstr \"\"\n";

    String expectedLocalizedAsset =
        "msgid \"\"\n"
            + "msgstr \"\"\n"
            + "\"Project-Id-Version: PACKAGE VERSION\\n\"\n"
            + "\"Report-Msgid-Bugs-To: \\n\"\n"
            + "\"POT-Creation-Date: 2017-09-15 11:53-0500\\n\"\n"
            + "\"PO-Revision-Date: YEAR-MO-DA HO:MI+ZONE\\n\"\n"
            + "\"Last-Translator: FULL NAME <EMAIL@ADDRESS>\\n\"\n"
            + "\"Language-Team: LANGUAGE <LL@li.org>\\n\"\n"
            + "\"MIME-Version: 1.0\\n\"\n"
            + "\"Plural-Forms: nplurals=1; plural=0;\\n\"\n"
            + "\"Content-Type: text/plain; charset=utf-8\\n\"\n"
            + "\"Content-Transfer-Encoding: 8bit\\n\"\n"
            + "\n"
            + "\n";

    createAsset(repo, "messages.pot", assetContent);
    processAsset(repo, assetContent);

    String localizedAsset = generateLocalizedRemoveUntranslated(assetContent, repoLocale, "ja-JP");
    logger.debug("localized=\n{}\nEOL", localizedAsset);
    assertEquals(expectedLocalizedAsset, localizedAsset);

    String forImport =
        "msgid \"\"\n"
            + "msgstr \"\"\n"
            + "\"Project-Id-Version: PACKAGE VERSION\\n\"\n"
            + "\"Report-Msgid-Bugs-To: \\n\"\n"
            + "\"POT-Creation-Date: 2017-09-15 11:53-0500\\n\"\n"
            + "\"PO-Revision-Date: YEAR-MO-DA HO:MI+ZONE\\n\"\n"
            + "\"Last-Translator: FULL NAME <EMAIL@ADDRESS>\\n\"\n"
            + "\"Language-Team: LANGUAGE <LL@li.org>\\n\"\n"
            + "\"MIME-Version: 1.0\\n\"\n"
            + "\"Plural-Forms: nplurals=1; plural=0;\\n\"\n"
            + "\"Content-Type: text/plain; charset=utf-8\\n\"\n"
            + "\"Content-Transfer-Encoding: 8bit\\n\"\n\n\n"
            + "#. Comments\n"
            + "#: core/logic/week_in_review_email_logic.py:49\n"
            + "msgid \"repin\"\n"
            + "msgstr \"repin-jp\"\n";

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
        "msgstr \"\"\n"
            + "\"Project-Id-Version: PACKAGE VERSION\\n\"\n"
            + "\"Report-Msgid-Bugs-To: \\n\"\n"
            + "\"POT-Creation-Date: 2017-09-15 11:53-0500\\n\"\n"
            + "\"PO-Revision-Date: YEAR-MO-DA HO:MI+ZONE\\n\"\n"
            + "\"Last-Translator: FULL NAME <EMAIL@ADDRESS>\\n\"\n"
            + "\"Language-Team: LANGUAGE <LL@li.org>\\n\"\n"
            + "\"MIME-Version: 1.0\\n\"\n"
            + "\"Plural-Forms: nplurals=2; plural=(n != 1);\\n\"\n"
            + "\"Content-Type: text/plain; charset=utf-8\\n\"\n"
            + "\"Content-Transfer-Encoding: 8bit\\n\"\n"
            + "#. Comments\n"
            + "#: core/logic/week_in_review_email_logic.py:49\n"
            + "msgid \"repin \\\"{}\\\"\"\n"
            + "msgstr \"\"";

    String expectedLocalizedAsset =
        "msgstr \"\"\n"
            + "\"Project-Id-Version: PACKAGE VERSION\\n\"\n"
            + "\"Report-Msgid-Bugs-To: \\n\"\n"
            + "\"POT-Creation-Date: 2017-09-15 11:53-0500\\n\"\n"
            + "\"PO-Revision-Date: YEAR-MO-DA HO:MI+ZONE\\n\"\n"
            + "\"Last-Translator: FULL NAME <EMAIL@ADDRESS>\\n\"\n"
            + "\"Language-Team: LANGUAGE <LL@li.org>\\n\"\n"
            + "\"MIME-Version: 1.0\\n\"\n"
            + "\"Plural-Forms: nplurals=1; plural=0;\\n\"\n"
            + "\"Content-Type: text/plain; charset=utf-8\\n\"\n"
            + "\"Content-Transfer-Encoding: 8bit\\n\"\n"
            + "#. Comments\n"
            + "#: core/logic/week_in_review_email_logic.py:49\n"
            + "msgid \"repin \\\"{}\\\"\"\n"
            + "msgstr \"repin \\\"{}\\\"\"\n";

    createAsset(repo, "messages.pot", assetContent);
    processAsset(repo, assetContent);

    List<TextUnitDTO> textUnitDTOs = searchTextUnits(repo);

    assertEquals(1, textUnitDTOs.size());
    TextUnitDTO textUnitDTO = textUnitDTOs.get(0);
    assertEquals("repin \\\"{}\\\"", textUnitDTO.getName());
    assertEquals("repin \"{}\"", textUnitDTO.getSource());

    String localizedAsset = generateLocalized(assetContent, repoLocale, "ja-JP");
    logger.debug("localized=\n{}", localizedAsset);
    assertEquals(expectedLocalizedAsset, localizedAsset);

    String forImport =
        "msgstr \"\"\n"
            + "\"Project-Id-Version: PACKAGE VERSION\\n\"\n"
            + "\"Report-Msgid-Bugs-To: \\n\"\n"
            + "\"POT-Creation-Date: 2017-09-15 11:53-0500\\n\"\n"
            + "\"PO-Revision-Date: YEAR-MO-DA HO:MI+ZONE\\n\"\n"
            + "\"Last-Translator: FULL NAME <EMAIL@ADDRESS>\\n\"\n"
            + "\"Language-Team: LANGUAGE <LL@li.org>\\n\"\n"
            + "\"MIME-Version: 1.0\\n\"\n"
            + "\"Plural-Forms: nplurals=1; plural=0;\\n\"\n"
            + "\"Content-Type: text/plain; charset=utf-8\\n\"\n"
            + "\"Content-Transfer-Encoding: 8bit\\n\"\n"
            + "#. Comments\n"
            + "#: core/logic/week_in_review_email_logic.py:49\n"
            + "msgid \"repin \\\"{}\\\"\"\n"
            + "msgstr \"repin \\\"{}\\\" jp\"\n";

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
    assertEquals("repin \\\"{}\\\"", textUnitDTO.getName());
    assertEquals("repin \"{}\"", textUnitDTO.getSource());
    assertEquals("repin \"{}\" jp", textUnitDTO.getTarget());
  }

  @Test
  public void testLocalizePoPluralRu() throws Exception {

    Repository repo = createRepository();
    RepositoryLocale repoLocale = addLocale(repo, "ru-RU");

    String assetContent =
        "msgid \"\"\n"
            + "msgstr \"\"\n"
            + "\"Project-Id-Version: PACKAGE VERSION\\n\"\n"
            + "\"Report-Msgid-Bugs-To: \\n\"\n"
            + "\"POT-Creation-Date: 2017-09-15 11:53-0500\\n\"\n"
            + "\"PO-Revision-Date: YEAR-MO-DA HO:MI+ZONE\\n\"\n"
            + "\"Last-Translator: FULL NAME <EMAIL@ADDRESS>\\n\"\n"
            + "\"Language-Team: LANGUAGE <LL@li.org>\\n\"\n"
            + "\"MIME-Version: 1.0\\n\"\n"
            + "\"Plural-Forms: nplurals=2; plural=(n != 1);\\n\"\n"
            + "\"Content-Type: text/plain; charset=utf-8\\n\"\n"
            + "\"Content-Transfer-Encoding: 8bit\\n\"\n"
            + "#. Comments\n"
            + "#: core/logic/week_in_review_email_logic.py:49\n"
            + "msgid \"repin\"\n"
            + "msgid_plural \"repins\"\n"
            + "msgstr[0] \"\"\n"
            + "msgstr[1] \"\"";

    String expectedLocalizedAsset =
        "msgid \"\"\n"
            + "msgstr \"\"\n"
            + "\"Project-Id-Version: PACKAGE VERSION\\n\"\n"
            + "\"Report-Msgid-Bugs-To: \\n\"\n"
            + "\"POT-Creation-Date: 2017-09-15 11:53-0500\\n\"\n"
            + "\"PO-Revision-Date: YEAR-MO-DA HO:MI+ZONE\\n\"\n"
            + "\"Last-Translator: FULL NAME <EMAIL@ADDRESS>\\n\"\n"
            + "\"Language-Team: LANGUAGE <LL@li.org>\\n\"\n"
            + "\"MIME-Version: 1.0\\n\"\n"
            + "\"Plural-Forms: nplurals=3; plural=n%10==1 && n%100!=11 ? 0 : n%10>=2 && n%10<=4 && (n%100<10 || n%100>=20) ? 1 : 2;\\n\"\n"
            + "\"Content-Type: text/plain; charset=utf-8\\n\"\n"
            + "\"Content-Transfer-Encoding: 8bit\\n\"\n"
            + "#. Comments\n"
            + "#: core/logic/week_in_review_email_logic.py:49\n"
            + "msgid \"repin\"\n"
            + "msgid_plural \"repins\"\n"
            + "msgstr[0] \"repin\"\n"
            + "msgstr[1] \"repins\"\n"
            + "msgstr[2] \"repins\"\n";

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
        "msgid \"\"\n"
            + "msgstr \"\"\n"
            + "\"Project-Id-Version: PACKAGE VERSION\\n\"\n"
            + "\"Report-Msgid-Bugs-To: \\n\"\n"
            + "\"POT-Creation-Date: 2017-09-15 11:53-0500\\n\"\n"
            + "\"PO-Revision-Date: YEAR-MO-DA HO:MI+ZONE\\n\"\n"
            + "\"Last-Translator: FULL NAME <EMAIL@ADDRESS>\\n\"\n"
            + "\"Language-Team: LANGUAGE <LL@li.org>\\n\"\n"
            + "\"MIME-Version: 1.0\\n\"\n"
            + "\"Plural-Forms: nplurals=3; plural=n%10==1 && n%100!=11 ? 0 : n%10>=2 && n%10<=4 && (n%100<10 || n%100>=20) ? 1 : 2;\\n\"\n"
            + "\"Content-Type: text/plain; charset=utf-8\\n\"\n"
            + "\"Content-Transfer-Encoding: 8bit\\n\"\n"
            + "#. Comments\n"
            + "#: core/logic/week_in_review_email_logic.py:49\n"
            + "msgid \"repin\"\n"
            + "msgid_plural \"repins\"\n"
            + "msgstr[0] \"repin-ru\"\n"
            + "msgstr[1] \"repins-ru-1\"\n"
            + "msgstr[2] \"repins-ru-2\"\n";

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
        "msgid \"\"\n"
            + "msgstr \"\"\n"
            + "\"Project-Id-Version: PACKAGE VERSION\\n\"\n"
            + "\"Report-Msgid-Bugs-To: \\n\"\n"
            + "\"POT-Creation-Date: 2017-09-15 11:53-0500\\n\"\n"
            + "\"PO-Revision-Date: YEAR-MO-DA HO:MI+ZONE\\n\"\n"
            + "\"Last-Translator: FULL NAME <EMAIL@ADDRESS>\\n\"\n"
            + "\"Language-Team: LANGUAGE <LL@li.org>\\n\"\n"
            + "\"MIME-Version: 1.0\\n\"\n"
            + "\"Plural-Forms: nplurals=2; plural=(n != 1);\\n\"\n"
            + "\"Content-Type: text/plain; charset=utf-8\\n\"\n"
            + "\"Content-Transfer-Encoding: 8bit\\n\"\n"
            + "#. Comments\n"
            + "#: core/logic/week_in_review_email_logic.py:49\n"
            + "msgid \"repin\"\n"
            + "msgid_plural \"repins\"\n"
            + "msgstr[0] \"\"\n"
            + "msgstr[1] \"\"";

    String expectedLocalizedAsset =
        "msgid \"\"\n"
            + "msgstr \"\"\n"
            + "\"Project-Id-Version: PACKAGE VERSION\\n\"\n"
            + "\"Report-Msgid-Bugs-To: \\n\"\n"
            + "\"POT-Creation-Date: 2017-09-15 11:53-0500\\n\"\n"
            + "\"PO-Revision-Date: YEAR-MO-DA HO:MI+ZONE\\n\"\n"
            + "\"Last-Translator: FULL NAME <EMAIL@ADDRESS>\\n\"\n"
            + "\"Language-Team: LANGUAGE <LL@li.org>\\n\"\n"
            + "\"MIME-Version: 1.0\\n\"\n"
            + "\"Plural-Forms: nplurals=3; plural=(n==1) ? 0 : (n>=2 && n<=4) ? 1 : 2;\\n\"\n"
            + "\"Content-Type: text/plain; charset=utf-8\\n\"\n"
            + "\"Content-Transfer-Encoding: 8bit\\n\"\n"
            + "#. Comments\n"
            + "#: core/logic/week_in_review_email_logic.py:49\n"
            + "msgid \"repin\"\n"
            + "msgid_plural \"repins\"\n"
            + "msgstr[0] \"repin\"\n"
            + "msgstr[1] \"repins\"\n"
            + "msgstr[2] \"repins\"\n";

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
        "msgid \"\"\n"
            + "msgstr \"\"\n"
            + "\"Project-Id-Version: PACKAGE VERSION\\n\"\n"
            + "\"Report-Msgid-Bugs-To: \\n\"\n"
            + "\"POT-Creation-Date: 2017-09-15 11:53-0500\\n\"\n"
            + "\"PO-Revision-Date: YEAR-MO-DA HO:MI+ZONE\\n\"\n"
            + "\"Last-Translator: FULL NAME <EMAIL@ADDRESS>\\n\"\n"
            + "\"Language-Team: LANGUAGE <LL@li.org>\\n\"\n"
            + "\"MIME-Version: 1.0\\n\"\n"
            + "\"Plural-Forms: nplurals=3; plural=(n==1) ? 0 : (n>=2 && n<=4) ? 1 : 2;\\n\"\n"
            + "\"Content-Type: text/plain; charset=utf-8\\n\"\n"
            + "\"Content-Transfer-Encoding: 8bit\\n\"\n"
            + "#. Comments\n"
            + "#: core/logic/week_in_review_email_logic.py:49\n"
            + "msgid \"repin\"\n"
            + "msgid_plural \"repins\"\n"
            + "msgstr[0] \"repin-cs\"\n"
            + "msgstr[1] \"repins-cz-1\"\n"
            + "msgstr[2] \"repins-cz-2\"\n";

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
        "msgid \"\"\n"
            + "msgstr \"\"\n"
            + "\"Project-Id-Version: PACKAGE VERSION\\n\"\n"
            + "\"Report-Msgid-Bugs-To: \\n\"\n"
            + "\"POT-Creation-Date: 2017-09-15 11:53-0500\\n\"\n"
            + "\"PO-Revision-Date: YEAR-MO-DA HO:MI+ZONE\\n\"\n"
            + "\"Last-Translator: FULL NAME <EMAIL@ADDRESS>\\n\"\n"
            + "\"Language-Team: LANGUAGE <LL@li.org>\\n\"\n"
            + "\"MIME-Version: 1.0\\n\"\n"
            + "\"Plural-Forms: nplurals=2; plural=(n != 1);\\n\"\n"
            + "\"Content-Type: text/plain; charset=utf-8\\n\"\n"
            + "\"Content-Transfer-Encoding: 8bit\\n\"\n"
            + "#. Comments\n"
            + "#: core/logic/week_in_review_email_logic.py:49\n"
            + "msgid \"repin \\\"{placeholder}\\\"\"\n"
            + "msgid_plural \"repins \\\"{placeholder}\\\"\"\n"
            + "msgstr[0] \"\"\n"
            + "msgstr[1] \"\"";

    String expectedLocalizedAsset =
        "msgid \"\"\n"
            + "msgstr \"\"\n"
            + "\"Project-Id-Version: PACKAGE VERSION\\n\"\n"
            + "\"Report-Msgid-Bugs-To: \\n\"\n"
            + "\"POT-Creation-Date: 2017-09-15 11:53-0500\\n\"\n"
            + "\"PO-Revision-Date: YEAR-MO-DA HO:MI+ZONE\\n\"\n"
            + "\"Last-Translator: FULL NAME <EMAIL@ADDRESS>\\n\"\n"
            + "\"Language-Team: LANGUAGE <LL@li.org>\\n\"\n"
            + "\"MIME-Version: 1.0\\n\"\n"
            + "\"Plural-Forms: nplurals=6; plural=n==0 ? 0 : n==1 ? 1 : n==2 ? 2 : n%100>=3 && n%100<=10 ? 3 : n%100>=11 ? 4 : 5;\\n\"\n"
            + "\"Content-Type: text/plain; charset=utf-8\\n\"\n"
            + "\"Content-Transfer-Encoding: 8bit\\n\"\n"
            + "#. Comments\n"
            + "#: core/logic/week_in_review_email_logic.py:49\n"
            + "msgid \"repin \\\"{placeholder}\\\"\"\n"
            + "msgid_plural \"repins \\\"{placeholder}\\\"\"\n"
            + "msgstr[0] \"repins \\\"{placeholder}\\\"\"\n"
            + "msgstr[1] \"repin \\\"{placeholder}\\\"\"\n"
            + "msgstr[2] \"repins \\\"{placeholder}\\\"\"\n"
            + "msgstr[3] \"repins \\\"{placeholder}\\\"\"\n"
            + "msgstr[4] \"repins \\\"{placeholder}\\\"\"\n"
            + "msgstr[5] \"repins \\\"{placeholder}\\\"\"\n";

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
        "msgid \"\"\n"
            + "msgstr \"\"\n"
            + "\"Project-Id-Version: PACKAGE VERSION\\n\"\n"
            + "\"Report-Msgid-Bugs-To: \\n\"\n"
            + "\"POT-Creation-Date: 2017-09-15 11:53-0500\\n\"\n"
            + "\"PO-Revision-Date: YEAR-MO-DA HO:MI+ZONE\\n\"\n"
            + "\"Last-Translator: FULL NAME <EMAIL@ADDRESS>\\n\"\n"
            + "\"Language-Team: LANGUAGE <LL@li.org>\\n\"\n"
            + "\"MIME-Version: 1.0\\n\"\n"
            + "\"Plural-Forms: nplurals=6; plural=n==0 ? 0 : n==1 ? 1 : n==2 ? 2 : n%100>=3 && n%100<=10 ? 3 : n%100>=11 ? 4 : 5;\\n\"\n"
            + "\"Content-Type: text/plain; charset=utf-8\\n\"\n"
            + "\"Content-Transfer-Encoding: 8bit\\n\"\n"
            + "#. Comments\n"
            + "#: core/logic/week_in_review_email_logic.py:49\n"
            + "msgid \"repin \\\"{placeholder}\\\"\"\n"
            + "msgid_plural \"repins \\\"{placeholder}\\\"\"\n"
            + "msgstr[0] \"repins \\\"{placeholder}\\\"-ar-0\"\n"
            + "msgstr[1] \"repins \\\"{placeholder}\\\"-ar-1\"\n"
            + "msgstr[2] \"repins \\\"{placeholder}\\\"-ar-2\"\n"
            + "msgstr[3] \"repins \\\"{placeholder}\\\"-ar-3\"\n"
            + "msgstr[4] \"repins \\\"{placeholder}\\\"-ar-4\"\n"
            + "msgstr[5] \"repins \\\"{placeholder}\\\"-ar-5\"\n";

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
        "# SOME DESCRIPTIVE TITLE.\n"
            + "# Copyright (C) YEAR THE PACKAGE'S COPYRIGHT HOLDER\n"
            + "# This file is distributed under the same license as the PACKAGE package.\n"
            + "# FIRST AUTHOR <EMAIL@ADDRESS>, YEAR.\n"
            + "#\n"
            + "#, fuzzy\n"
            + "msgid \"\"\n"
            + "msgstr \"\"\n"
            + "\"Project-Id-Version: PACKAGE VERSION\\n\"\n"
            + "\"Report-Msgid-Bugs-To: \\n\"\n"
            + "\"POT-Creation-Date: 2017-02-24 11:50-0800\\n\"\n"
            + "\"PO-Revision-Date: YEAR-MO-DA HO:MI+ZONE\\n\"\n"
            + "\"Last-Translator: FULL NAME <EMAIL@ADDRESS>\\n\"\n"
            + "\"Language-Team: LANGUAGE <LL@li.org>\\n\"\n"
            + "\"Language: \\n\"\n"
            + "\"MIME-Version: 1.0\\n\"\n"
            + "\"Plural-Forms: nplurals=2; plural=(n != 1);\\n\"\n"
            + "\"Content-Type: text/plain; charset=utf-8\\n\"\n"
            + "\"Content-Transfer-Encoding: 8bit\\n\"\n"
            + " \n"
            + "   \n"
            + "#. Test plural\n"
            + "#: file.js:20\n"
            + "msgctxt \"car\"\n"
            + "msgid \"There is {number} car\"\n"
            + "msgid_plural \"There are {number} cars\"\n"
            + "msgstr[0] \"\"\n"
            + "msgstr[1] \"\"\n"
            + "\n"
            + "#. Test plural okapi bug\n"
            + "#: file.js:24\n"
            + "msgctxt \"testpluralokapibug\"\n"
            + "msgid \"test okapi bug\"\n"
            + "msgstr \"\"\n"
            + "";
    createAsset(repository, "messages.pot", assetContent);
    processAsset(repository, assetContent);

    String localizedAssetContent =
        "# SOME DESCRIPTIVE TITLE.\n"
            + "# Copyright (C) YEAR THE PACKAGE'S COPYRIGHT HOLDER\n"
            + "# This file is distributed under the same license as the PACKAGE package.\n"
            + "# FIRST AUTHOR <EMAIL@ADDRESS>, YEAR.\n"
            + "#\n"
            + "#, fuzzy\n"
            + "msgid \"\"\n"
            + "msgstr \"\"\n"
            + "\"Project-Id-Version: PACKAGE VERSION\\n\"\n"
            + "\"Report-Msgid-Bugs-To: \\n\"\n"
            + "\"POT-Creation-Date: 2017-02-24 11:50-0800\\n\"\n"
            + "\"PO-Revision-Date: YEAR-MO-DA HO:MI+ZONE\\n\"\n"
            + "\"Last-Translator: FULL NAME <EMAIL@ADDRESS>\\n\"\n"
            + "\"Language-Team: LANGUAGE <LL@li.org>\\n\"\n"
            + "\"Language: \\n\"\n"
            + "\"MIME-Version: 1.0\\n\"\n"
            + "\"Plural-Forms: nplurals=1; plural=0;\\n\"\n"
            + "\"Content-Type: text/plain; charset=utf-8\\n\"\n"
            + "\"Content-Transfer-Encoding: 8bit\\n\"\n"
            + "  \n"
            + "   \n"
            + "#. Test plural\n"
            + "#: file.js:20\n"
            + "msgctxt \"car\"\n"
            + "msgid \"There is {number} car\"\n"
            + "msgid_plural \"There are {number} cars\"\n"
            + "msgstr[0] \"There is {number} car\"\n"
            + "\n"
            + "#. Test plural okapi bug\n"
            + "#: file.js:24\n"
            + "msgctxt \"testpluralokapibug\"\n"
            + "msgid \"test okapi bug\"\n"
            + "msgstr \"jp test okapi bug\"\n"
            + "\n"
            + "";
    importTranslations(repoLocale, localizedAssetContent, StatusForEqualTarget.APPROVED);
  }
}

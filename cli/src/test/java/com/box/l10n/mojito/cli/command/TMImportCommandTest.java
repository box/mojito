package com.box.l10n.mojito.cli.command;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.slf4j.LoggerFactory.getLogger;

import com.box.l10n.mojito.bootstrap.BootstrapConfig;
import com.box.l10n.mojito.cli.CLITestBase;
import com.box.l10n.mojito.entity.Locale;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.RepositoryLocale;
import com.box.l10n.mojito.entity.TMTextUnit;
import com.box.l10n.mojito.entity.TMTextUnitVariant;
import com.box.l10n.mojito.entity.TMTextUnitVariantComment;
import com.box.l10n.mojito.rest.client.AssetClient;
import com.box.l10n.mojito.service.asset.AssetService;
import com.box.l10n.mojito.service.locale.LocaleService;
import com.box.l10n.mojito.service.tm.TMService;
import com.box.l10n.mojito.service.tm.TMTextUnitRepository;
import com.box.l10n.mojito.service.tm.TMTextUnitVariantRepository;
import java.io.File;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

/** @author wyau */
public class TMImportCommandTest extends CLITestBase {

  /** logger */
  static Logger logger = getLogger(TMImportCommandTest.class);

  @Autowired AssetClient assetClient;

  @Autowired TMService tmService;

  @Autowired TMTextUnitVariantRepository tmTextUnitVariantRepository;

  @Autowired LocaleService localeService;

  @Autowired TMTextUnitRepository tmTextUnitRepository;

  @Autowired AssetService assetService;

  @Autowired BootstrapConfig bootstrapConfig;

  Repository testRepository;

  File sourceDirectory;

  @Before
  public void before() throws Exception {
    testRepository = createTestRepoUsingRepoService();
    sourceDirectory = getInputResourcesTestDir("import");
  }

  @Test
  public void testImport() throws Exception {
    logger.debug("Source directory is [{}]", sourceDirectory.getAbsoluteFile());
    getL10nJCommander()
        .run("tm-import", "-r", testRepository.getName(), "-s", sourceDirectory.getAbsolutePath());

    String outputString = outputCapture.toString();

    Matcher matcher = Pattern.compile("- Importing file:\\s*(.*?)\\s").matcher(outputString);
    assertTrue(matcher.find());

    Long tmId = testRepository.getTm().getId();

    List<TMTextUnit> tmTextUnits = tmTextUnitRepository.findByTm_id(tmId);
    assertEquals(5, tmTextUnits.size());

    Locale locale = getLocaleFromRepositoryLocales(testRepository.getRepositoryLocales(), "ja-JP");

    List<TMTextUnitVariant> tmTextUnitsVariantsJapanese =
        tmTextUnitVariantRepository.findAllByLocale_IdAndTmTextUnit_Tm_id(locale.getId(), tmId);
    assertEquals(5, tmTextUnitsVariantsJapanese.size());

    TMTextUnitVariant tmTextUnitVariant;
    Iterator<TMTextUnitVariant> iterator = tmTextUnitsVariantsJapanese.iterator();

    tmTextUnitVariant = iterator.next();
    assertEquals("100文字の説明:", tmTextUnitVariant.getContent());

    tmTextUnitVariant = iterator.next();
    assertEquals("15分", tmTextUnitVariant.getContent());

    tmTextUnitVariant = iterator.next();
    assertEquals("1日", tmTextUnitVariant.getContent());

    tmTextUnitVariant = iterator.next();
    assertEquals("1時間", tmTextUnitVariant.getContent());

    tmTextUnitVariant = iterator.next();
    assertEquals("1か月", tmTextUnitVariant.getContent());
  }

  @Test
  public void testImportWithImportExportNote() throws Exception {
    logger.debug("Source directory is [{}]", sourceDirectory.getAbsoluteFile());
    getL10nJCommander()
        .run("tm-import", "-r", testRepository.getName(), "-s", sourceDirectory.getAbsolutePath());

    String outputString = outputCapture.toString();

    Matcher matcher = Pattern.compile("- Importing file:\\s*(.*?)\\s").matcher(outputString);
    assertTrue(matcher.find());

    Long tmId = testRepository.getTm().getId();

    List<TMTextUnit> tmTextUnits = tmTextUnitRepository.findByTm_id(tmId);
    assertEquals(10, tmTextUnits.size());

    Locale locale = getLocaleFromRepositoryLocales(testRepository.getRepositoryLocales(), "ja-JP");

    List<TMTextUnitVariant> tmTextUnitsVariantsJapanese =
        tmTextUnitVariantRepository.findAllByLocale_IdAndTmTextUnit_Tm_id(locale.getId(), tmId);
    sortTMTextUnitVariantByContent(tmTextUnitsVariantsJapanese);
    assertEquals(7, tmTextUnitsVariantsJapanese.size());

    TMTextUnitVariant tmTextUnitVariant;
    Iterator<TMTextUnitVariant> iterator = tmTextUnitsVariantsJapanese.iterator();

    tmTextUnitVariant = iterator.next();
    assertEquals("100文字の説明:", tmTextUnitVariant.getContent());

    tmTextUnitVariant = iterator.next();
    assertEquals("1時間", tmTextUnitVariant.getContent());
    assertEquals(new DateTime(1447198865000L), tmTextUnitVariant.getCreatedDate());

    tmTextUnitVariant = iterator.next();
    assertEquals("1か月", tmTextUnitVariant.getContent());
    assertEquals(new DateTime(1447198865000L), tmTextUnitVariant.getCreatedDate());

    Set<TMTextUnitVariantComment> tmTextUnitVariantComments =
        tmTextUnitVariant.getTmTextUnitVariantComments();
    for (TMTextUnitVariantComment tmTextUnitVariantComment : tmTextUnitVariantComments) {
      assertEquals(
          "User should be set to the current user doing the import",
          bootstrapConfig.getDefaultUser().getUsername(),
          tmTextUnitVariantComment.getCreatedByUser().getUsername());
    }

    tmTextUnitVariant = iterator.next();
    assertEquals("1日", tmTextUnitVariant.getContent());

    tmTextUnitVariant = iterator.next();
    assertEquals("1時間 (in source2)", tmTextUnitVariant.getContent());

    tmTextUnitVariant = iterator.next();
    assertEquals("15分", tmTextUnitVariant.getContent());

    tmTextUnitVariant = iterator.next();
    assertEquals("1か月 (in source2)", tmTextUnitVariant.getContent());

    assertFalse(iterator.hasNext());
  }

  private void sortTMTextUnitVariantByContent(List<TMTextUnitVariant> textUnitVariants) {
    Collections.sort(
        textUnitVariants,
        new Comparator<TMTextUnitVariant>() {

          @Override
          public int compare(TMTextUnitVariant o1, TMTextUnitVariant o2) {
            return o1.getContentMD5().compareTo(o2.getContentMD5());
          }
        });
  }

  private Locale getLocaleFromRepositoryLocales(
      Set<RepositoryLocale> repositoryLocales, String bcp47Tag) {
    for (RepositoryLocale repositoryLocale : repositoryLocales) {
      Locale locale = repositoryLocale.getLocale();
      if (locale.getBcp47Tag().equals(bcp47Tag)) {
        return locale;
      }
    }

    return null;
  }

  @Test
  public void testReimportShouldFail() throws Exception {
    assetService.createAssetWithContent(testRepository.getId(), "asset-path.xliff", "test content");

    getL10nJCommander()
        .run("tm-import", "-r", testRepository.getName(), "-s", sourceDirectory.getAbsolutePath());

    String output = outputCapture.toString();
    assertTrue(output.contains("Error importing "));
  }

  @Test
  public void testImportShouldFailIfImportingJustTargetFiles() throws Exception {
    getL10nJCommander()
        .run("tm-import", "-r", testRepository.getName(), "-s", sourceDirectory.getAbsolutePath());

    String output = outputCapture.toString();
    assertTrue(output.contains("Error importing "));
  }
}

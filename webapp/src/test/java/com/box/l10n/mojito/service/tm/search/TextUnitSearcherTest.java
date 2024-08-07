package com.box.l10n.mojito.service.tm.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import com.box.l10n.mojito.entity.Asset;
import com.box.l10n.mojito.entity.AssetExtraction;
import com.box.l10n.mojito.entity.AssetTextUnit;
import com.box.l10n.mojito.entity.AssetTextUnitToTMTextUnit;
import com.box.l10n.mojito.entity.TMTextUnit;
import com.box.l10n.mojito.entity.TMTextUnitCurrentVariant;
import com.box.l10n.mojito.entity.TMTextUnitVariant;
import com.box.l10n.mojito.service.asset.AssetService;
import com.box.l10n.mojito.service.assetExtraction.ServiceTestBase;
import com.box.l10n.mojito.service.locale.LocaleService;
import com.box.l10n.mojito.service.tm.TMService;
import com.box.l10n.mojito.service.tm.TMTestData;
import com.box.l10n.mojito.service.tm.TMTextUnitRepository;
import com.box.l10n.mojito.service.tm.TMTextUnitVariantRepository;
import com.box.l10n.mojito.test.TestIdWatcher;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * TODO(P1) complete with test for the whole flow, document update and extraction to check for
 * used/unused documents, etc.
 *
 * @author jaurambault
 */
public class TextUnitSearcherTest extends ServiceTestBase {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(TextUnitSearcherTest.class);

  @Autowired TextUnitSearcher textUnitSearcher;

  @Autowired TMService tmService;

  @Autowired TMTextUnitVariantRepository tmTextUnitVariantRepository;

  @Autowired LocaleService localeService;

  @Autowired TMTextUnitRepository tmTextUnitRepository;

  @Autowired AssetService assetService;

  @Autowired EntityManager entityManager;

  @Rule public TestIdWatcher testIdWatcher = new TestIdWatcher();

  @Transactional
  @Test
  public void testAllFilters() {

    TMTestData tmTestData = new TMTestData(testIdWatcher);

    List<String> localeTags = new ArrayList<>();
    localeTags.add("ko-KR");

    TextUnitSearcherParameters textUnitSearcherParameters =
        new TextUnitSearcherParametersForTesting();

    textUnitSearcherParameters.setName("zuora_error_message_verify_state_province");
    textUnitSearcherParameters.setLocaleTags(localeTags);
    textUnitSearcherParameters.setTarget("올바른 국가, 지역 또는 시/도를 입력하십시오.");
    textUnitSearcherParameters.setRepositoryIds(tmTestData.repository.getId());
    textUnitSearcherParameters.setSource("Please enter a valid state, region or province");
    textUnitSearcherParameters.setUsedFilter(UsedFilter.USED);
    textUnitSearcherParameters.setStatusFilter(StatusFilter.TRANSLATED);
    textUnitSearcherParameters.setLocationUsage("fake_for_test");

    List<TextUnitDTO> textUnitDTOs = textUnitSearcher.search(textUnitSearcherParameters);

    for (TextUnitDTO textUnitDTO : textUnitDTOs) {
      logger.debug(ToStringBuilder.reflectionToString(textUnitDTO, ToStringStyle.MULTI_LINE_STYLE));
    }

    Iterator<TextUnitDTO> iterator = textUnitDTOs.iterator();

    TextUnitDTO next = iterator.next();
    assertEquals(tmTestData.asset.getId(), next.getAssetId());
    assertEquals("Comment1", next.getComment());
    assertEquals(tmTestData.koKR.getId(), next.getLocaleId());
    assertEquals("zuora_error_message_verify_state_province", next.getName());
    assertEquals("Please enter a valid state, region or province", next.getSource());
    assertEquals("올바른 국가, 지역 또는 시/도를 입력하십시오.", next.getTarget());
    assertEquals("ko-KR", next.getTargetLocale());
    assertEquals(tmTestData.addTMTextUnit1.getId(), next.getTmTextUnitId());
    assertEquals(
        tmTestData.addCurrentTMTextUnitVariant1KoKR.getId(), next.getTmTextUnitVariantId());
    assertTrue(next.isTranslated());
    assertTrue(next.isUsed());
    assertEquals("fake_for_test", next.getAssetPath());

    assertFalse(iterator.hasNext());
  }

  @Transactional
  @Test
  public void testMultipleLocalesTranslatedAndUntranslated() {

    TMTestData tmTestData = new TMTestData(testIdWatcher);

    List<String> localeTags = new ArrayList<>();
    localeTags.add("ko-KR");
    localeTags.add("fr-FR");
    localeTags.add("fr-CA");

    TextUnitSearcherParameters textUnitSearcherParameters =
        new TextUnitSearcherParametersForTesting();

    textUnitSearcherParameters.setRepositoryIds(tmTestData.repository.getId());
    textUnitSearcherParameters.setName("zuora_error_message_verify_state_province");
    textUnitSearcherParameters.setLocaleTags(localeTags);
    textUnitSearcherParameters.setSource("Please enter a valid state, region or province");

    List<TextUnitDTO> textUnitDTOs = textUnitSearcher.search(textUnitSearcherParameters);

    for (TextUnitDTO textUnitDTO : textUnitDTOs) {
      logger.debug(ToStringBuilder.reflectionToString(textUnitDTO, ToStringStyle.MULTI_LINE_STYLE));
    }

    Iterator<TextUnitDTO> iterator = textUnitDTOs.iterator();

    TextUnitDTO next = iterator.next();
    assertEquals(tmTestData.asset.getId(), next.getAssetId());
    assertEquals("Comment1", next.getComment());
    assertEquals(tmTestData.frFR.getId(), next.getLocaleId());
    assertEquals("zuora_error_message_verify_state_province", next.getName());
    assertEquals("Please enter a valid state, region or province", next.getSource());
    assertEquals("Veuillez indiquer un état, une région ou une province valide.", next.getTarget());
    assertEquals("fr-FR", next.getTargetLocale());
    assertEquals(tmTestData.addTMTextUnit1.getId(), next.getTmTextUnitId());
    assertEquals(
        tmTestData.addCurrentTMTextUnitVariant1FrFR.getId(), next.getTmTextUnitVariantId());
    assertTrue(next.isTranslated());
    assertTrue(next.isUsed());

    next = iterator.next();
    assertEquals(tmTestData.asset.getId(), next.getAssetId());
    assertEquals("Comment1", next.getComment());
    assertEquals(tmTestData.frCA.getId(), next.getLocaleId());
    assertEquals("zuora_error_message_verify_state_province", next.getName());
    assertEquals("Please enter a valid state, region or province", next.getSource());
    assertEquals(null, next.getTarget());
    assertEquals("fr-CA", next.getTargetLocale());
    assertEquals(tmTestData.addTMTextUnit1.getId(), next.getTmTextUnitId());
    assertEquals(null, next.getTmTextUnitVariantId());
    assertFalse(next.isTranslated());
    assertTrue(next.isUsed());

    next = iterator.next();
    assertEquals(tmTestData.asset.getId(), next.getAssetId());
    assertEquals("Comment1", next.getComment());
    assertEquals(tmTestData.koKR.getId(), next.getLocaleId());
    assertEquals("zuora_error_message_verify_state_province", next.getName());
    assertEquals("Please enter a valid state, region or province", next.getSource());
    assertEquals("올바른 국가, 지역 또는 시/도를 입력하십시오.", next.getTarget());
    assertEquals("ko-KR", next.getTargetLocale());
    assertEquals(tmTestData.addTMTextUnit1.getId(), next.getTmTextUnitId());
    assertEquals(
        tmTestData.addCurrentTMTextUnitVariant1KoKR.getId(), next.getTmTextUnitVariantId());
    assertTrue(next.isTranslated());
    assertTrue(next.isUsed());

    assertFalse(iterator.hasNext());
  }

  @Transactional
  @Test
  public void testUnusedAndTranslated() {

    TMTestData tmTestData = new TMTestData(testIdWatcher);

    List<String> localeTags = new ArrayList<>();
    localeTags.add("ko-KR");
    localeTags.add("fr-FR");

    TextUnitSearcherParameters textUnitSearcherParameters =
        new TextUnitSearcherParametersForTesting();

    textUnitSearcherParameters.setRepositoryIds(tmTestData.repository.getId());
    textUnitSearcherParameters.setLocaleTags(localeTags);
    textUnitSearcherParameters.setStatusFilter(StatusFilter.TRANSLATED);
    textUnitSearcherParameters.setUsedFilter(UsedFilter.UNUSED);

    List<TextUnitDTO> textUnitDTOs = textUnitSearcher.search(textUnitSearcherParameters);

    for (TextUnitDTO textUnitDTO : textUnitDTOs) {
      logger.debug(ToStringBuilder.reflectionToString(textUnitDTO, ToStringStyle.MULTI_LINE_STYLE));
    }

    Iterator<TextUnitDTO> iterator = textUnitDTOs.iterator();

    TextUnitDTO next = iterator.next();
    assertEquals("Comment3", next.getComment());
    assertEquals(tmTestData.frFR.getId(), next.getLocaleId());
    assertEquals("TEST3", next.getName());
    assertEquals("Content3", next.getSource());
    assertEquals("Content3 fr-FR", next.getTarget());
    assertEquals("fr-FR", next.getTargetLocale());
    assertEquals(tmTestData.addTMTextUnit3.getId(), next.getTmTextUnitId());
    assertEquals(
        tmTestData.addCurrentTMTextUnitVariant3FrFR.getId(), next.getTmTextUnitVariantId());
    assertTrue(next.isTranslated());
    assertFalse(next.isUsed());

    assertFalse(iterator.hasNext());
  }

  @Transactional
  @Test
  public void testPagination() {

    TMTestData tmTestData = new TMTestData(testIdWatcher);

    List<String> localeTags = new ArrayList<>();
    localeTags.add("ko-KR");
    localeTags.add("fr-FR");
    localeTags.add("fr-CA");

    TextUnitSearcherParameters textUnitSearcherParameters =
        new TextUnitSearcherParametersForTesting();

    textUnitSearcherParameters.setRepositoryIds(tmTestData.repository.getId());
    textUnitSearcherParameters.setLocaleTags(localeTags);

    textUnitSearcherParameters.setLimit(2);
    textUnitSearcherParameters.setOffset(2);

    List<TextUnitDTO> textUnitDTOs = textUnitSearcher.search(textUnitSearcherParameters);

    for (TextUnitDTO textUnitDTO : textUnitDTOs) {
      logger.debug(ToStringBuilder.reflectionToString(textUnitDTO, ToStringStyle.MULTI_LINE_STYLE));
    }

    Iterator<TextUnitDTO> iterator = textUnitDTOs.iterator();

    TextUnitDTO next = iterator.next();
    assertEquals("Comment1", next.getComment());
    assertEquals(tmTestData.koKR.getId(), next.getLocaleId());
    assertEquals("zuora_error_message_verify_state_province", next.getName());
    assertEquals("Please enter a valid state, region or province", next.getSource());
    assertEquals("올바른 국가, 지역 또는 시/도를 입력하십시오.", next.getTarget());
    assertEquals("ko-KR", next.getTargetLocale());
    assertEquals(tmTestData.addTMTextUnit1.getId(), next.getTmTextUnitId());
    assertEquals(
        tmTestData.addCurrentTMTextUnitVariant1KoKR.getId(), next.getTmTextUnitVariantId());
    assertTrue(next.isTranslated());
    assertTrue(next.isUsed());

    next = iterator.next();
    assertEquals("Comment2", next.getComment());
    assertEquals(tmTestData.frFR.getId(), next.getLocaleId());
    assertEquals("TEST2", next.getName());
    assertEquals("Content2", next.getSource());
    assertEquals("fr-FR", next.getTargetLocale());
    assertEquals(tmTestData.addTMTextUnit2.getId(), next.getTmTextUnitId());
    assertFalse(next.isTranslated());
    assertTrue(next.isUsed());

    assertFalse(iterator.hasNext());
  }

  @Transactional(noRollbackFor = {Throwable.class})
  @Test
  public void testUntranslatedOrTranslationNeeded() {

    TMTestData tmTestData = new TMTestData(testIdWatcher);

    logger.debug("Mark on translated string as need review");
    TMTextUnitCurrentVariant addTMTextUnitCurrentVariantReviewNeeded =
        tmService.addTMTextUnitCurrentVariant(
            tmTestData.addCurrentTMTextUnitVariant1FrFR.getTmTextUnit().getId(),
            tmTestData.addCurrentTMTextUnitVariant1FrFR.getLocale().getId(),
            tmTestData.addCurrentTMTextUnitVariant1FrFR.getContent(),
            "this translation is bad in context",
            TMTextUnitVariant.Status.TRANSLATION_NEEDED,
            true);

    List<String> localeTags = new ArrayList<>();
    localeTags.add("fr-FR");

    TextUnitSearcherParameters textUnitSearcherParameters =
        new TextUnitSearcherParametersForTesting();
    textUnitSearcherParameters.setStatusFilter(StatusFilter.FOR_TRANSLATION);

    textUnitSearcherParameters.setRepositoryIds(tmTestData.repository.getId());
    textUnitSearcherParameters.setLocaleTags(localeTags);

    List<TextUnitDTO> textUnitDTOs = textUnitSearcher.search(textUnitSearcherParameters);

    for (TextUnitDTO textUnitDTO : textUnitDTOs) {
      logger.debug(ToStringBuilder.reflectionToString(textUnitDTO, ToStringStyle.MULTI_LINE_STYLE));
    }

    Iterator<TextUnitDTO> iterator = textUnitDTOs.iterator();

    logger.debug("Check the entry that needs review");
    TextUnitDTO next = iterator.next();
    assertEquals("Comment1", next.getComment());
    assertEquals(tmTestData.frFR.getId(), next.getLocaleId());
    assertEquals("zuora_error_message_verify_state_province", next.getName());
    assertEquals("Please enter a valid state, region or province", next.getSource());
    assertEquals("Veuillez indiquer un état, une région ou une province valide.", next.getTarget());
    assertEquals("fr-FR", next.getTargetLocale());
    assertEquals(tmTestData.addTMTextUnit1.getId(), next.getTmTextUnitId());
    assertEquals(
        addTMTextUnitCurrentVariantReviewNeeded.getTmTextUnitVariant().getId(),
        next.getTmTextUnitVariantId());
    assertTrue(next.isTranslated());
    assertTrue(next.isUsed());
    assertEquals(TMTextUnitVariant.Status.TRANSLATION_NEEDED, next.getStatus());
    assertTrue(next.isIncludedInLocalizedFile());

    logger.debug(
        "Check the entry that is untranslated (no need for review, not included in the file)");
    next = iterator.next();
    assertEquals("Comment2", next.getComment());
    assertEquals(tmTestData.frFR.getId(), next.getLocaleId());
    assertEquals("TEST2", next.getName());
    assertEquals("Content2", next.getSource());
    assertEquals("fr-FR", next.getTargetLocale());
    assertEquals(tmTestData.addTMTextUnit2.getId(), next.getTmTextUnitId());
    assertFalse(next.isTranslated());
    assertTrue(next.isUsed());
    assertEquals(TMTextUnitVariant.Status.TRANSLATION_NEEDED, next.getStatus());
    assertFalse(next.isIncludedInLocalizedFile());

    assertFalse(iterator.hasNext());
  }

  @Transactional
  @Test
  public void testIncludedInLocalizedFile() {
    TMTestData tmTestData = new TMTestData(testIdWatcher);

    List<TMTextUnitVariant> variants =
        tmTextUnitVariantRepository.findAllByLocale_IdAndTmTextUnit_Tm_id(
            tmTestData.frFR.getId(), tmTestData.tm.getId());
    assertEquals("There should be 2 TMTextUnitVariants", 2, variants.size());

    logger.debug("Mark one translated string as not included");

    Long invalidTmTextUnitId = tmTestData.addCurrentTMTextUnitVariant1FrFR.getTmTextUnit().getId();
    tmService.addTMTextUnitCurrentVariant(
        invalidTmTextUnitId,
        tmTestData.addCurrentTMTextUnitVariant1FrFR.getLocale().getId(),
        tmTestData.addCurrentTMTextUnitVariant1FrFR.getContent(),
        "this translation fails compilation",
        TMTextUnitVariant.Status.REVIEW_NEEDED,
        false);

    variants =
        tmTextUnitVariantRepository.findAllByLocale_IdAndTmTextUnit_Tm_id(
            tmTestData.frFR.getId(), tmTestData.tm.getId());
    assertEquals("There should be 3 TMTextUnitVariants", 3, variants.size());

    TextUnitSearcherParameters textUnitSearcherParameters =
        new TextUnitSearcherParametersForTesting();
    textUnitSearcherParameters.setRepositoryIds(tmTestData.repository.getId());
    textUnitSearcherParameters.setLocaleId(tmTestData.frFR.getId());

    textUnitSearcherParameters.setStatusFilter(StatusFilter.NOT_REJECTED);
    List<TextUnitDTO> textUnitDTOs = textUnitSearcher.search(textUnitSearcherParameters);

    assertEquals(
        "The searcher should have returned 1 included text unit DTOs", 1, textUnitDTOs.size());

    for (TextUnitDTO textUnitDTO : textUnitDTOs) {
      assertNotEquals(
          "The found variant should not be the invalid one",
          invalidTmTextUnitId,
          textUnitDTO.getTmTextUnitId());
    }

    textUnitSearcherParameters.setStatusFilter(StatusFilter.REJECTED);
    textUnitDTOs = textUnitSearcher.search(textUnitSearcherParameters);

    assertEquals(
        "The searcher should have returned only 1 excluded text unit DTO", 1, textUnitDTOs.size());
    assertEquals(
        "The found variant should be the invalid one",
        invalidTmTextUnitId,
        textUnitDTOs.get(0).getTmTextUnitId());
  }

  @Transactional(noRollbackFor = {Throwable.class})
  @Test
  public void testReviewNeeded() {
    TMTestData tmTestData = new TMTestData(testIdWatcher);

    List<TMTextUnitVariant> variants =
        tmTextUnitVariantRepository.findAllByLocale_IdAndTmTextUnit_Tm_id(
            tmTestData.frFR.getId(), tmTestData.tm.getId());
    assertEquals("There should be 2 TMTextUnitVariants", 2, variants.size());

    logger.debug("Mark one translated string as review needed");

    Long reviewNeededTmTextUnitId =
        tmTestData.addCurrentTMTextUnitVariant1FrFR.getTmTextUnit().getId();
    tmService.addTMTextUnitCurrentVariant(
        reviewNeededTmTextUnitId,
        tmTestData.addCurrentTMTextUnitVariant1FrFR.getLocale().getId(),
        tmTestData.addCurrentTMTextUnitVariant1FrFR.getContent(),
        "this translation fails compilation",
        TMTextUnitVariant.Status.REVIEW_NEEDED);

    variants =
        tmTextUnitVariantRepository.findAllByLocale_IdAndTmTextUnit_Tm_id(
            tmTestData.frFR.getId(), tmTestData.tm.getId());
    assertEquals("There should be 3 TMTextUnitVariants", 3, variants.size());

    TextUnitSearcherParameters textUnitSearcherParameters =
        new TextUnitSearcherParametersForTesting();
    textUnitSearcherParameters.setRepositoryIds(tmTestData.repository.getId());
    textUnitSearcherParameters.setLocaleId(tmTestData.frFR.getId());
    textUnitSearcherParameters.setStatusFilter(StatusFilter.REVIEW_NOT_NEEDED);

    List<TextUnitDTO> textUnitDTOs = textUnitSearcher.search(textUnitSearcherParameters);

    assertEquals(
        "The searcher should have returned 1 included text unit DTOs", 1, textUnitDTOs.size());

    for (TextUnitDTO textUnitDTO : textUnitDTOs) {
      assertNotEquals(
          "The found variant should not be the one that needs review",
          reviewNeededTmTextUnitId,
          textUnitDTO.getTmTextUnitId());
    }

    textUnitSearcherParameters.setStatusFilter(StatusFilter.REVIEW_NEEDED);
    textUnitDTOs = textUnitSearcher.search(textUnitSearcherParameters);

    assertEquals(
        "The searcher should have returned only 1 excluded text unit DTO", 1, textUnitDTOs.size());
    assertEquals(
        "The found variant should be the one that needs review",
        reviewNeededTmTextUnitId,
        textUnitDTOs.get(0).getTmTextUnitId());
  }

  @Test
  public void testCountNone() throws Exception {
    TMTestData tmTestData = new TMTestData(testIdWatcher);

    TextUnitSearcherParameters textUnitSearcherParameters = new TextUnitSearcherParameters();
    textUnitSearcherParameters.setRepositoryIds(tmTestData.repository.getId());
    textUnitSearcherParameters.setStatusFilter(StatusFilter.FOR_TRANSLATION);
    textUnitSearcherParameters.setLocaleTags(Arrays.asList("fr-FR"));
    textUnitSearcherParameters.setUsedFilter(UsedFilter.USED);
    textUnitSearcherParameters.setDoNotTranslateFilter(false);
    textUnitSearcherParameters.setName("name with no match");

    TextUnitAndWordCount textUnitAndWordCount =
        textUnitSearcher.countTextUnitAndWordCount(textUnitSearcherParameters);
    assertEquals(
        "Should return no text unit hence count = 0", 0, textUnitAndWordCount.getTextUnitCount());
    assertEquals(
        "Should return no text unit hence word count = 0",
        0,
        textUnitAndWordCount.getTextUnitWordCount());
  }

  @Test
  public void testCount() throws Exception {
    TMTestData tmTestData = new TMTestData(testIdWatcher);

    TextUnitSearcherParameters textUnitSearcherParameters = new TextUnitSearcherParameters();
    textUnitSearcherParameters.setRepositoryIds(tmTestData.repository.getId());
    textUnitSearcherParameters.setStatusFilter(StatusFilter.FOR_TRANSLATION);
    textUnitSearcherParameters.setUsedFilter(UsedFilter.USED);

    List<TextUnitDTO> search = textUnitSearcher.search(textUnitSearcherParameters);

    long numberOfWords = 0;

    for (TextUnitDTO textUnitDTO : search) {
      numberOfWords +=
          tmTextUnitRepository.findById(textUnitDTO.getTmTextUnitId()).orElse(null).getWordCount();
    }

    TextUnitAndWordCount textUnitAndWordCount =
        textUnitSearcher.countTextUnitAndWordCount(textUnitSearcherParameters);
    logger.info(
        "for translation used from count: {}, {}",
        textUnitAndWordCount.getTextUnitCount(),
        textUnitAndWordCount.getTextUnitWordCount());
    assertEquals(search.size(), textUnitAndWordCount.getTextUnitCount());
    assertEquals(numberOfWords, textUnitAndWordCount.getTextUnitWordCount());
  }

  @Test
  public void testCreatedDate() throws Exception {

    ZonedDateTime now = ZonedDateTime.now();
    ZonedDateTime secondsBefore = now.minusSeconds(2);
    ZonedDateTime secondsAfter = now.plusSeconds(2);

    TMTestData tmTestData = new TMTestData(testIdWatcher);
    TextUnitSearcherParameters textUnitSearcherParameters = new TextUnitSearcherParameters();
    textUnitSearcherParameters.setRepositoryIds(tmTestData.repository.getId());
    textUnitSearcherParameters.setUsedFilter(UsedFilter.USED);

    List<TextUnitDTO> search = textUnitSearcher.search(textUnitSearcherParameters);
    assertEquals(8, search.size());

    textUnitSearcherParameters.setTmTextUnitCreatedBefore(secondsBefore);
    search = textUnitSearcher.search(textUnitSearcherParameters);
    assertEquals(0, search.size());

    textUnitSearcherParameters.setTmTextUnitCreatedAfter(secondsBefore);
    textUnitSearcherParameters.setTmTextUnitCreatedBefore(null);
    search = textUnitSearcher.search(textUnitSearcherParameters);
    assertEquals(8, search.size());

    textUnitSearcherParameters.setTmTextUnitCreatedAfter(secondsBefore);
    textUnitSearcherParameters.setTmTextUnitCreatedBefore(secondsAfter);
    search = textUnitSearcher.search(textUnitSearcherParameters);
    assertEquals(8, search.size());

    textUnitSearcherParameters.setTmTextUnitCreatedAfter(secondsAfter);
    textUnitSearcherParameters.setTmTextUnitCreatedBefore(null);
    search = textUnitSearcher.search(textUnitSearcherParameters);
    assertEquals(0, search.size());
  }

  @Test
  public void testSkipAssetPathWithPattern() {
    TMTestData testData = new TMTestData(testIdWatcher);

    TextUnitSearcherParameters params = new TextUnitSearcherParameters();
    params.setRepositoryIds(testData.repository.getId());
    params.setSkipAssetPathWithPattern("%skip_me");

    Asset asset =
        assetService.createAssetWithContent(
            testData.repository.getId(), "fake_skip_me", "fake for test other tm repo");

    TMTextUnit tu1 =
        tmService.addTMTextUnit(
            testData.tm.getId(), asset.getId(), "fake1", "Content1", "Comment1");
    TMTextUnit tu2 =
        tmService.addTMTextUnit(
            testData.tm.getId(), asset.getId(), "fake2", "Content2", "Comment2");
    TMTextUnit tu3 =
        tmService.addTMTextUnit(
            testData.tm.getId(), testData.asset.getId(), "fake3", "Content4", "Comment4");
    TMTextUnit tu4 =
        tmService.addTMTextUnit(
            testData.tm.getId(), testData.asset.getId(), "fake4", "Content4", "Comment4");

    List<TextUnitDTO> textUnits = textUnitSearcher.search(params);

    assertThat(textUnits).isNotEmpty();
    assertThat(textUnits)
        .extracting(TextUnitDTO::getTmTextUnitId)
        .doesNotContain(tu1.getId(), tu2.getId());
    assertThat(textUnits)
        .extracting(TextUnitDTO::getTmTextUnitId)
        .contains(tu3.getId(), tu4.getId());

    params.setSkipAssetPathWithPattern("dont_use%");

    asset =
        assetService.createAssetWithContent(
            testData.repository.getId(), "dont_use_me", "fake for test other tm repo");

    tu1 =
        tmService.addTMTextUnit(
            testData.tm.getId(), asset.getId(), "dont_use_fake1", "Content1", "Comment1");
    tu2 =
        tmService.addTMTextUnit(
            testData.tm.getId(), asset.getId(), "dont_use_fake2", "Content2", "Comment2");
    tu3 =
        tmService.addTMTextUnit(
            testData.tm.getId(), testData.asset.getId(), "use_fake3", "Content4", "Comment4");
    tu4 =
        tmService.addTMTextUnit(
            testData.tm.getId(), testData.asset.getId(), "use_fake4", "Content4", "Comment4");

    textUnits = textUnitSearcher.search(params);

    assertThat(textUnits).isNotEmpty();
    assertThat(textUnits)
        .extracting(TextUnitDTO::getTmTextUnitId)
        .doesNotContain(tu1.getId(), tu2.getId());
    assertThat(textUnits)
        .extracting(TextUnitDTO::getTmTextUnitId)
        .contains(tu3.getId(), tu4.getId());

    params.setSkipAssetPathWithPattern("%ignore%");

    asset =
        assetService.createAssetWithContent(
            testData.repository.getId(), "wil_be_ignored", "fake for test other tm repo");

    tu1 =
        tmService.addTMTextUnit(
            testData.tm.getId(), asset.getId(), "ignore_fake1", "Content1", "Comment1");
    tu2 =
        tmService.addTMTextUnit(
            testData.tm.getId(), asset.getId(), "ignore_use_fake2", "Content2", "Comment2");
    tu3 =
        tmService.addTMTextUnit(
            testData.tm.getId(), testData.asset.getId(), "no_fake3", "Content4", "Comment4");
    tu4 =
        tmService.addTMTextUnit(
            testData.tm.getId(), testData.asset.getId(), "no_fake4", "Content4", "Comment4");

    textUnits = textUnitSearcher.search(params);

    assertThat(textUnits).isNotEmpty();
    assertThat(textUnits)
        .extracting(TextUnitDTO::getTmTextUnitId)
        .doesNotContain(tu1.getId(), tu2.getId());
    assertThat(textUnits)
        .extracting(TextUnitDTO::getTmTextUnitId)
        .contains(tu3.getId(), tu4.getId());
  }

  @Test
  public void testSkipTextUnitsWithPattern() {
    TMTestData testData = new TMTestData(testIdWatcher);
    TMTextUnit tu1, tu2, tu3;

    TextUnitSearcherParameters params = new TextUnitSearcherParameters();
    params.setRepositoryIds(testData.repository.getId());
    params.setSkipTextUnitWithPattern("%skip_me");

    tu1 =
        tmService.addTMTextUnit(
            testData.tm.getId(),
            testData.asset.getId(),
            "text_unit_skip_me",
            "Content1",
            "Comment1");
    tu2 =
        tmService.addTMTextUnit(
            testData.tm.getId(),
            testData.asset.getId(),
            "text_unit2_skip_me",
            "Content2",
            "Comment2");
    tu3 =
        tmService.addTMTextUnit(
            testData.tm.getId(), testData.asset.getId(), "skip_me_not", "Content2", "Comment2");

    List<TextUnitDTO> textUnits = textUnitSearcher.search(params);

    assertThat(textUnits).isNotEmpty();
    assertThat(textUnits)
        .allSatisfy(textUnit -> assertThat(textUnit.getName()).doesNotEndWith("skip_me"));
    assertThat(textUnits).extracting(TextUnitDTO::getTmTextUnitId).contains(tu3.getId());

    params.setSkipTextUnitWithPattern("not_this%");

    tu1 =
        tmService.addTMTextUnit(
            testData.tm.getId(), testData.asset.getId(), "not_this_text", "Content1", "Comment1");
    tu2 =
        tmService.addTMTextUnit(
            testData.tm.getId(), testData.asset.getId(), "not_this_text2", "Content2", "Comment2");
    tu3 =
        tmService.addTMTextUnit(
            testData.tm.getId(),
            testData.asset.getId(),
            "this_should_be_in_not_this_one",
            "Content2",
            "Comment2");

    textUnits = textUnitSearcher.search(params);

    assertThat(textUnits).isNotEmpty();
    assertThat(textUnits)
        .allSatisfy(textUnit -> assertThat(textUnit.getName()).doesNotStartWith("not_this"));
    assertThat(textUnits).extracting(TextUnitDTO::getTmTextUnitId).contains(tu3.getId());

    params.setSkipTextUnitWithPattern("%ignore%");

    tu1 =
        tmService.addTMTextUnit(
            testData.tm.getId(), testData.asset.getId(), "unit_to_ignored", "Content1", "Comment1");
    tu2 =
        tmService.addTMTextUnit(
            testData.tm.getId(),
            testData.asset.getId(),
            "should_ignore_this_unit",
            "Content2",
            "Comment2");
    tu3 =
        tmService.addTMTextUnit(
            testData.tm.getId(),
            testData.asset.getId(),
            "this_should_show_up",
            "Content2",
            "Comment2");

    textUnits = textUnitSearcher.search(params);

    assertThat(textUnits).isNotEmpty();
    assertThat(textUnits)
        .allSatisfy(textUnit -> assertThat(textUnit.getName()).doesNotContain("ignore"));
    assertThat(textUnits).extracting(TextUnitDTO::getTmTextUnitId).contains(tu3.getId());
  }

  @Transactional
  @Test
  public void testExactMatchSearchName() {
    testSearchText(
        "name",
        "zuora_error_message_verify_state_province",
        SearchType.EXACT,
        Arrays.asList("zuora_error_message_verify_state_province"));
  }

  @Transactional
  @Test
  public void testContainsSearchName() {
    testSearchText("name", "TEST", SearchType.CONTAINS, Arrays.asList("TEST1", "TEST2", "TEST3"));
  }

  @Transactional
  @Test
  public void testILikeSearchName() {
    testSearchText(
        "name",
        "%e%t%",
        SearchType.ILIKE,
        Arrays.asList("zuora_error_message_verify_state_province", "TEST1", "TEST2", "TEST3"));
  }

  @Transactional
  @Test
  public void testExactMatchSearchSource() {
    testSearchText(
        "source",
        "Please enter a valid state, region or province",
        SearchType.EXACT,
        Arrays.asList("Please enter a valid state, region or province"));
  }

  @Transactional
  @Test
  public void testContainsSearchSource() {
    testSearchText(
        "source",
        "ent",
        SearchType.CONTAINS,
        Arrays.asList("Please enter a valid state, region or province", "Content2", "Content3"));
  }

  @Transactional
  @Test
  public void testILikeSearchSource() {
    testSearchText(
        "source",
        "%TE%",
        SearchType.ILIKE,
        Arrays.asList("Please enter a valid state, region or province", "Content2", "Content3"));
  }

  @Transactional
  @Test
  public void testExactMatchSearchTarget() {
    testSearchText(
        "target",
        "Veuillez indiquer un état, une région ou une province valide.",
        SearchType.EXACT,
        Arrays.asList("Veuillez indiquer un état, une région ou une province valide."));
  }

  @Transactional
  @Test
  public void testContainsSearchTarget() {
    testSearchText(
        "target", "또는", SearchType.CONTAINS, Arrays.asList("올바른 국가, 지역 또는 시/도를 입력하십시오."));
  }

  @Transactional
  @Test
  public void testILikeSearchTarget() {
    testSearchText(
        "target",
        "%C%E%",
        SearchType.ILIKE,
        Arrays.asList(
            "Veuillez indiquer un état, une région ou une province valide.",
            "Content2 fr-CA",
            "Content3 fr-CA",
            "Content3 fr-FR"));
  }

  @Test
  public void testExactMatchLocationUsageByAssetPath() {
    testSearchText("assetPath", "fake_for_test", SearchType.EXACT, List.of("fake_for_test"));
  }

  @Test
  public void testContainsLocationUsageByAssetPath() {
    testSearchText("assetPath", "fake", SearchType.CONTAINS, List.of("fake_for_test"));
  }

  @Test
  public void testILikeLocationUsageByAssetPath() {
    testSearchText("assetPath", "%FAKE_FOR_%test", SearchType.ILIKE, List.of("fake_for_test"));
  }

  @Test
  public void testExactMatchLocationUsageByUsages() {
    testSearchText("usage", "usage_test", SearchType.EXACT, List.of("usage_test"), true);
  }

  @Test
  public void testContainsLocationUsageByUsages() {
    testSearchText("usage", "usage", SearchType.CONTAINS, List.of("usage_test"), true);
  }

  @Test
  public void testILikeLocationUsageByUsages() {
    testSearchText("usage", "%USAGE_%TEST", SearchType.ILIKE, List.of("usage_test"), true);
  }

  private List<AssetTextUnitToTMTextUnit> getAssetTextUnitToTMTextUnit(
      AssetExtraction assetExtraction) {
    TypedQuery<AssetTextUnitToTMTextUnit> query =
        this.entityManager
            .createQuery(
                "select m from AssetTextUnitToTMTextUnit m join fetch m.assetTextUnit join fetch m.assetTextUnit.usages where m.assetTextUnit.assetExtraction.id = :assetExtractionId",
                AssetTextUnitToTMTextUnit.class)
            .setParameter("assetExtractionId", assetExtraction.getId());
    return query.getResultList();
  }

  public void testSearchText(
      String attribute,
      String value,
      SearchType searchType,
      List<String> expectedNames,
      boolean addUsage) {
    TMTestData tmTestData = new TMTestData(testIdWatcher, addUsage);

    TextUnitSearcherParameters textUnitSearcherParameters =
        new TextUnitSearcherParametersForTesting();

    if ("name".equals(attribute)) {
      textUnitSearcherParameters.setName(value);
    } else if ("source".equals(attribute)) {
      textUnitSearcherParameters.setSource(value);
    } else if ("target".equals(attribute)) {
      textUnitSearcherParameters.setTarget(value);
    } else if ("usage".equals(attribute) || "assetPath".equals(attribute)) {
      textUnitSearcherParameters.setLocationUsage(value);
    } else {
      throw new RuntimeException();
    }

    textUnitSearcherParameters.setSearchType(searchType);
    textUnitSearcherParameters.setRepositoryIds(tmTestData.repository.getId());

    List<TextUnitDTO> textUnitDTOs = textUnitSearcher.search(textUnitSearcherParameters);

    assertFalse(textUnitDTOs.isEmpty());

    List<AssetTextUnitToTMTextUnit> assetTextUnitToTMTextUnits =
        this.getAssetTextUnitToTMTextUnit(tmTestData.assetExtraction);

    for (TextUnitDTO textUnitDTO : textUnitDTOs) {
      logger.debug(ToStringBuilder.reflectionToString(textUnitDTO, ToStringStyle.MULTI_LINE_STYLE));
    }

    for (TextUnitDTO textUnitDTO : textUnitDTOs) {
      String attributeToCheck;

      if ("name".equals(attribute)) {
        attributeToCheck = textUnitDTO.getName();
      } else if ("source".equals(attribute)) {
        attributeToCheck = textUnitDTO.getSource();
      } else if ("target".equals(attribute)) {
        attributeToCheck = textUnitDTO.getTarget();
      } else if ("assetPath".equals(attribute)) {
        attributeToCheck = textUnitDTO.getAssetPath();
      } else {
        Optional<String> optionalUsage =
            assetTextUnitToTMTextUnits.stream()
                .filter(
                    assetTextUnitToTMTextUnit ->
                        Objects.equals(
                            assetTextUnitToTMTextUnit.getTmTextUnit().getId(),
                            textUnitDTO.getTmTextUnitId()))
                .map(AssetTextUnitToTMTextUnit::getAssetTextUnit)
                .map(AssetTextUnit::getUsages)
                .flatMap(Collection::stream)
                .filter(usage -> usage.equals("usage_test"))
                .findFirst();
        if (optionalUsage.isEmpty()) {
          throw new RuntimeException();
        }
        attributeToCheck = optionalUsage.get();
      }

      if (!expectedNames.contains(attributeToCheck)) {
        Assert.fail(
            "the search returned an unexpected textunit, " + attribute + ": " + attributeToCheck);
      }
    }
  }

  public void testSearchText(
      String attribute, String value, SearchType searchType, List<String> expectedNames) {
    this.testSearchText(attribute, value, searchType, expectedNames, false);
  }
}

package com.box.l10n.mojito.service.rollback;

import static junit.framework.TestCase.assertNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.box.l10n.mojito.entity.Asset;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.TM;
import com.box.l10n.mojito.entity.TMTextUnit;
import com.box.l10n.mojito.entity.TMTextUnitCurrentVariant;
import com.box.l10n.mojito.service.asset.AssetService;
import com.box.l10n.mojito.service.assetExtraction.ServiceTestBase;
import com.box.l10n.mojito.service.locale.LocaleService;
import com.box.l10n.mojito.service.repository.RepositoryNameAlreadyUsedException;
import com.box.l10n.mojito.service.repository.RepositoryService;
import com.box.l10n.mojito.service.tm.TMRepository;
import com.box.l10n.mojito.service.tm.TMService;
import com.box.l10n.mojito.service.tm.TMTextUnitCurrentVariantRepository;
import com.box.l10n.mojito.service.tm.TMTextUnitRepository;
import com.box.l10n.mojito.test.TestIdWatcher;
import java.util.Arrays;
import java.util.List;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** @author aloison */
public class CurrentVariantRollbackServiceTest extends ServiceTestBase {

  @Autowired TMService tmService;

  @Autowired TMRepository tmRepository;

  @Autowired CurrentVariantRollbackService currentVariantRollbackService;

  @Autowired TMTextUnitRepository tmTextUnitRepository;

  @Autowired TMTextUnitCurrentVariantRepository tmTextUnitCurrentVariantRepository;

  @Autowired LocaleService localeService;

  @Autowired AssetService assetService;

  @Autowired RepositoryService repositoryService;

  TM tm;
  Repository repository;
  Asset asset;

  Long jaLocaleId;
  Long frLocaleId;

  @Rule public TestIdWatcher testIdWatcher = new TestIdWatcher();

  @Before
  public void before() throws RepositoryNameAlreadyUsedException {
    // TODO(P1) change with testTM
    if (tm == null) {
      tm = new TM();
      tmRepository.save(tm);
    }

    if (frLocaleId == null) {
      frLocaleId = localeService.findByBcp47Tag("fr-FR").getId();
    }

    if (jaLocaleId == null) {
      jaLocaleId = localeService.findByBcp47Tag("ja-JP").getId();
    }

    if (repository == null) {
      repository = repositoryService.createRepository(testIdWatcher.getEntityName("repository"));
      asset =
          assetService.createAssetWithContent(
              repository.getId(), "path/to/asset", "test asset content");
    }
  }

  @Test
  public void testRollbackShouldDeleteCurrentVariantsWhenTmTextUnitDidNotHaveAnyVariants()
      throws InterruptedException {
    TMTextUnit tmTextUnit =
        tmService.addTMTextUnit(
            tm.getId(), asset.getId(), "hello_world", "Hello World!", "Comments about hello world");
    Long tmTextUnitId = tmTextUnit.getId();
    DateTime dateTimeBeforeAddingVariant = new DateTime();

    assertOnlyEnglishVariantIsPresent(tmTextUnitId);

    Thread.sleep(10);

    tmService.addCurrentTMTextUnitVariant(tmTextUnit.getId(), frLocaleId, "Bonjour le monde!");
    tmService.addCurrentTMTextUnitVariant(tmTextUnit.getId(), jaLocaleId, "こんにちは、世界!");

    assertCurrentVariantWasSet(
        "The added variants should be set as current",
        Arrays.asList(frLocaleId, jaLocaleId),
        tmTextUnitId);

    currentVariantRollbackService.rollbackCurrentVariantsFromTMToDate(
        dateTimeBeforeAddingVariant, tm.getId());
    assertNoCurrentVariantSet(
        "After rollback, there should not be any current variants anymore",
        Arrays.asList(frLocaleId, jaLocaleId),
        tmTextUnitId);
  }

  @Test
  public void
      testRollbackShouldDeleteCurrentVariantsForGivenLocalesWhenTmTextUnitDidNotHaveAnyVariants()
          throws InterruptedException {
    TMTextUnit tmTextUnit =
        tmService.addTMTextUnit(
            tm.getId(), asset.getId(), "hello_world", "Hello World!", "Comments about hello world");
    Long tmTextUnitId = tmTextUnit.getId();
    DateTime dateTimeBeforeAddingVariant = new DateTime();

    assertOnlyEnglishVariantIsPresent(tmTextUnitId);

    Thread.sleep(10);

    tmService.addCurrentTMTextUnitVariant(tmTextUnit.getId(), frLocaleId, "Bonjour le monde!");
    tmService.addCurrentTMTextUnitVariant(tmTextUnit.getId(), jaLocaleId, "こんにちは、世界!");

    assertCurrentVariantWasSet(
        "The added variants should be set as current",
        Arrays.asList(frLocaleId, jaLocaleId),
        tmTextUnitId);

    // only rollback ja-JP
    CurrentVariantRollbackParameters rollbackParameters = new CurrentVariantRollbackParameters();
    rollbackParameters.setLocaleIds(Arrays.asList(jaLocaleId));

    currentVariantRollbackService.rollbackCurrentVariantsFromTMToDate(
        dateTimeBeforeAddingVariant, tm.getId(), rollbackParameters);
    assertCurrentVariantWasSet(
        "After rollback, there should still be a current variant for fr-FR",
        Arrays.asList(frLocaleId),
        tmTextUnitId);
    assertNoCurrentVariantSet(
        "After rollback, there should not be any current variant anymore for ja-JP",
        Arrays.asList(jaLocaleId),
        tmTextUnitId);
  }

  @Test
  public void testRollbackShouldUndoChangesToCurrentVariantsWhenTmTextUnitHadVariants()
      throws InterruptedException {

    TMTextUnit tmTextUnit =
        tmService.addTMTextUnit(
            tm.getId(), asset.getId(), "hello_world", "Hello World!", "Comments about hello world");
    Long tmTextUnitId = tmTextUnit.getId();
    tmService.addCurrentTMTextUnitVariant(tmTextUnit.getId(), frLocaleId, "Bonjour le monde!");
    tmService.addCurrentTMTextUnitVariant(tmTextUnit.getId(), jaLocaleId, "こんにちは、世界!");

    DateTime dateTimeBeforeChangingCurrentVariant = new DateTime();

    assertVariantContentForCurrentVariantEquals("Bonjour le monde!", frLocaleId, tmTextUnitId);
    assertVariantContentForCurrentVariantEquals("こんにちは、世界!", jaLocaleId, tmTextUnitId);

    Thread.sleep(10);

    tmService.addCurrentTMTextUnitVariant(tmTextUnitId, frLocaleId, "Bonjour le monde! 2");
    tmService.addCurrentTMTextUnitVariant(tmTextUnitId, frLocaleId, "Bonjour le monde! 3");
    tmService.addCurrentTMTextUnitVariant(tmTextUnitId, jaLocaleId, "こんにちは、世界! 2");
    tmService.addCurrentTMTextUnitVariant(tmTextUnitId, jaLocaleId, "こんにちは、世界! 3");

    assertVariantContentForCurrentVariantEquals("Bonjour le monde! 3", frLocaleId, tmTextUnitId);
    assertVariantContentForCurrentVariantEquals("こんにちは、世界! 3", jaLocaleId, tmTextUnitId);

    currentVariantRollbackService.rollbackCurrentVariantsFromTMToDate(
        dateTimeBeforeChangingCurrentVariant, tm.getId());

    assertVariantContentForCurrentVariantEquals("Bonjour le monde!", frLocaleId, tmTextUnitId);
    assertVariantContentForCurrentVariantEquals("こんにちは、世界!", jaLocaleId, tmTextUnitId);
  }

  @Test
  public void
      testRollbackShouldUndoChangesToCurrentVariantsForGivenLocalesWhenTmTextUnitHadVariants()
          throws InterruptedException {

    TMTextUnit tmTextUnit =
        tmService.addTMTextUnit(
            tm.getId(), asset.getId(), "hello_world", "Hello World!", "Comments about hello world");
    Long tmTextUnitId = tmTextUnit.getId();
    tmService.addCurrentTMTextUnitVariant(tmTextUnit.getId(), frLocaleId, "Bonjour le monde!");
    tmService.addCurrentTMTextUnitVariant(tmTextUnit.getId(), jaLocaleId, "こんにちは、世界!");

    DateTime dateTimeBeforeChangingCurrentVariant = new DateTime();

    assertVariantContentForCurrentVariantEquals("Bonjour le monde!", frLocaleId, tmTextUnitId);
    assertVariantContentForCurrentVariantEquals("こんにちは、世界!", jaLocaleId, tmTextUnitId);

    Thread.sleep(10);

    tmService.addCurrentTMTextUnitVariant(tmTextUnitId, frLocaleId, "Bonjour le monde! 2");
    tmService.addCurrentTMTextUnitVariant(tmTextUnitId, frLocaleId, "Bonjour le monde! 3");
    tmService.addCurrentTMTextUnitVariant(tmTextUnitId, jaLocaleId, "こんにちは、世界! 2");
    tmService.addCurrentTMTextUnitVariant(tmTextUnitId, jaLocaleId, "こんにちは、世界! 3");

    assertVariantContentForCurrentVariantEquals("Bonjour le monde! 3", frLocaleId, tmTextUnitId);
    assertVariantContentForCurrentVariantEquals("こんにちは、世界! 3", jaLocaleId, tmTextUnitId);

    // only rollback ja-JP
    CurrentVariantRollbackParameters rollbackParameters = new CurrentVariantRollbackParameters();
    rollbackParameters.setLocaleIds(Arrays.asList(jaLocaleId));

    currentVariantRollbackService.rollbackCurrentVariantsFromTMToDate(
        dateTimeBeforeChangingCurrentVariant, tm.getId(), rollbackParameters);

    assertVariantContentForCurrentVariantEquals(
        "After rollback, the current variant should not have changed for fr-FR",
        "Bonjour le monde! 3",
        frLocaleId,
        tmTextUnitId);
    assertVariantContentForCurrentVariantEquals(
        "After rollback, the current variant should have been rolled back for ja-JP",
        "こんにちは、世界!",
        jaLocaleId,
        tmTextUnitId);
  }

  @Test
  public void
      testRollbackShouldDeleteCurrentVariantsForGivenTextUnitsWhenTmTextUnitDidNotHaveAnyVariants()
          throws InterruptedException {

    TMTextUnit tmTextUnit1 =
        tmService.addTMTextUnit(
            tm.getId(), asset.getId(), "hello_world", "Hello World!", "Comments about hello world");
    Long tmTextUnit1Id = tmTextUnit1.getId();
    TMTextUnit tmTextUnit2 =
        tmService.addTMTextUnit(
            tm.getId(),
            asset.getId(),
            "hello_new_world",
            "Hello New World!",
            "Comments about hello new world");
    Long tmTextUnit2Id = tmTextUnit2.getId();

    assertOnlyEnglishVariantIsPresent(tmTextUnit1Id);
    assertOnlyEnglishVariantIsPresent(tmTextUnit2Id);

    DateTime dateTimeBeforeAddingVariant = new DateTime();

    Thread.sleep(10);

    tmService.addCurrentTMTextUnitVariant(tmTextUnit1.getId(), frLocaleId, "Bonjour le monde!");
    tmService.addCurrentTMTextUnitVariant(tmTextUnit1.getId(), jaLocaleId, "こんにちは、世界!");
    tmService.addCurrentTMTextUnitVariant(
        tmTextUnit2.getId(), frLocaleId, "Bonjour le nouveau monde!");
    tmService.addCurrentTMTextUnitVariant(tmTextUnit2.getId(), jaLocaleId, "新しい世界をこんにちは!");

    assertCurrentVariantWasSet(
        "The added variants should be set as current",
        Arrays.asList(frLocaleId, jaLocaleId),
        tmTextUnit1Id);
    assertCurrentVariantWasSet(
        "The added variants should be set as current",
        Arrays.asList(frLocaleId, jaLocaleId),
        tmTextUnit2Id);

    // only rollback TMTextUnit 1
    CurrentVariantRollbackParameters rollbackParameters = new CurrentVariantRollbackParameters();
    rollbackParameters.setTmTextUnitIds(Arrays.asList(tmTextUnit1Id));

    currentVariantRollbackService.rollbackCurrentVariantsFromTMToDate(
        dateTimeBeforeAddingVariant, tm.getId(), rollbackParameters);

    assertNoCurrentVariantSet(
        "After rollback, the current variants of the text unit 1 should have been deleted",
        Arrays.asList(frLocaleId, jaLocaleId),
        tmTextUnit1Id);
    assertCurrentVariantWasSet(
        "After rollback, the current variants of the text unit 2 should still be there",
        Arrays.asList(frLocaleId, jaLocaleId),
        tmTextUnit2Id);
    assertVariantContentForCurrentVariantEquals(
        "Bonjour le nouveau monde!", frLocaleId, tmTextUnit2Id);
    assertVariantContentForCurrentVariantEquals("新しい世界をこんにちは!", jaLocaleId, tmTextUnit2Id);
  }

  @Test
  public void
      testRollbackShouldUndoChangesToCurrentVariantsForGivenTextUnitsWhenTmTextUnitHadVariants()
          throws InterruptedException {

    TMTextUnit tmTextUnit1 =
        tmService.addTMTextUnit(
            tm.getId(), asset.getId(), "hello_world", "Hello World!", "Comments about hello world");
    Long tmTextUnit1Id = tmTextUnit1.getId();
    TMTextUnit tmTextUnit2 =
        tmService.addTMTextUnit(
            tm.getId(),
            asset.getId(),
            "hello_new_world",
            "Hello New World!",
            "Comments about hello new world");
    Long tmTextUnit2Id = tmTextUnit2.getId();

    tmService.addCurrentTMTextUnitVariant(tmTextUnit1.getId(), frLocaleId, "Bonjour le monde!");
    tmService.addCurrentTMTextUnitVariant(tmTextUnit1.getId(), jaLocaleId, "こんにちは、世界!");
    tmService.addCurrentTMTextUnitVariant(
        tmTextUnit2.getId(), frLocaleId, "Bonjour le nouveau monde!");
    tmService.addCurrentTMTextUnitVariant(tmTextUnit2.getId(), jaLocaleId, "新しい世界をこんにちは!");

    assertVariantContentForCurrentVariantEquals("Bonjour le monde!", frLocaleId, tmTextUnit1Id);
    assertVariantContentForCurrentVariantEquals("こんにちは、世界!", jaLocaleId, tmTextUnit1Id);
    assertVariantContentForCurrentVariantEquals(
        "Bonjour le nouveau monde!", frLocaleId, tmTextUnit2Id);
    assertVariantContentForCurrentVariantEquals("新しい世界をこんにちは!", jaLocaleId, tmTextUnit2Id);

    DateTime dateTimeBeforeChangingCurrentVariant = new DateTime();

    Thread.sleep(10);

    tmService.addCurrentTMTextUnitVariant(tmTextUnit1Id, frLocaleId, "Bonjour le monde! 2");
    tmService.addCurrentTMTextUnitVariant(tmTextUnit1Id, frLocaleId, "Bonjour le monde! 3");
    tmService.addCurrentTMTextUnitVariant(tmTextUnit1Id, jaLocaleId, "こんにちは、世界! 2");
    tmService.addCurrentTMTextUnitVariant(tmTextUnit1Id, jaLocaleId, "こんにちは、世界! 3");

    tmService.addCurrentTMTextUnitVariant(tmTextUnit2Id, frLocaleId, "Bonjour le nouveau monde! 2");
    tmService.addCurrentTMTextUnitVariant(tmTextUnit2Id, frLocaleId, "Bonjour le nouveau monde! 3");
    tmService.addCurrentTMTextUnitVariant(tmTextUnit2Id, jaLocaleId, "新しい世界をこんにちは! 2");
    tmService.addCurrentTMTextUnitVariant(tmTextUnit2Id, jaLocaleId, "新しい世界をこんにちは! 3");

    assertVariantContentForCurrentVariantEquals("Bonjour le monde! 3", frLocaleId, tmTextUnit1Id);
    assertVariantContentForCurrentVariantEquals("こんにちは、世界! 3", jaLocaleId, tmTextUnit1Id);
    assertVariantContentForCurrentVariantEquals(
        "Bonjour le nouveau monde! 3", frLocaleId, tmTextUnit2Id);
    assertVariantContentForCurrentVariantEquals("新しい世界をこんにちは! 3", jaLocaleId, tmTextUnit2Id);

    // only rollback ja-JP
    CurrentVariantRollbackParameters rollbackParameters = new CurrentVariantRollbackParameters();
    rollbackParameters.setLocaleIds(Arrays.asList(jaLocaleId));

    currentVariantRollbackService.rollbackCurrentVariantsFromTMToDate(
        dateTimeBeforeChangingCurrentVariant, tm.getId(), rollbackParameters);

    assertVariantContentForCurrentVariantEquals(
        "After rollback, the current variant 1 should not have changed for fr-FR",
        "Bonjour le monde! 3",
        frLocaleId,
        tmTextUnit1Id);
    assertVariantContentForCurrentVariantEquals(
        "After rollback, the current variant 2 should not have changed for fr-FR",
        "Bonjour le nouveau monde! 3",
        frLocaleId,
        tmTextUnit2Id);
    assertVariantContentForCurrentVariantEquals(
        "After rollback, the current variant 1 should have been rolled back for ja-JP",
        "こんにちは、世界!",
        jaLocaleId,
        tmTextUnit1Id);
    assertVariantContentForCurrentVariantEquals(
        "After rollback, the current variant 2 should have been rolled back for ja-JP",
        "新しい世界をこんにちは!",
        jaLocaleId,
        tmTextUnit2Id);
  }

  @Test
  public void
      testRollbackShouldUndoChangesToCurrentVariantsForGivenLocalesAndTextUnitsWhenTmTextUnitHadVariants()
          throws InterruptedException {

    TMTextUnit tmTextUnit1 =
        tmService.addTMTextUnit(
            tm.getId(), asset.getId(), "hello_world", "Hello World!", "Comments about hello world");
    Long tmTextUnit1Id = tmTextUnit1.getId();
    TMTextUnit tmTextUnit2 =
        tmService.addTMTextUnit(
            tm.getId(),
            asset.getId(),
            "hello_new_world",
            "Hello New World!",
            "Comments about hello new world");
    Long tmTextUnit2Id = tmTextUnit2.getId();

    tmService.addCurrentTMTextUnitVariant(tmTextUnit1.getId(), frLocaleId, "Bonjour le monde!");
    tmService.addCurrentTMTextUnitVariant(tmTextUnit1.getId(), jaLocaleId, "こんにちは、世界!");
    tmService.addCurrentTMTextUnitVariant(
        tmTextUnit2.getId(), frLocaleId, "Bonjour le nouveau monde!");
    tmService.addCurrentTMTextUnitVariant(tmTextUnit2.getId(), jaLocaleId, "新しい世界をこんにちは!");

    DateTime dateTimeBeforeChangingCurrentVariant = new DateTime();

    Thread.sleep(10);

    tmService.addCurrentTMTextUnitVariant(tmTextUnit1Id, frLocaleId, "Bonjour le monde! 2");
    tmService.addCurrentTMTextUnitVariant(tmTextUnit1Id, frLocaleId, "Bonjour le monde! 3");
    tmService.addCurrentTMTextUnitVariant(tmTextUnit1Id, jaLocaleId, "こんにちは、世界! 2");
    tmService.addCurrentTMTextUnitVariant(tmTextUnit1Id, jaLocaleId, "こんにちは、世界! 3");

    tmService.addCurrentTMTextUnitVariant(tmTextUnit2Id, frLocaleId, "Bonjour le nouveau monde! 2");
    tmService.addCurrentTMTextUnitVariant(tmTextUnit2Id, frLocaleId, "Bonjour le nouveau monde! 3");
    tmService.addCurrentTMTextUnitVariant(tmTextUnit2Id, jaLocaleId, "新しい世界をこんにちは! 2");
    tmService.addCurrentTMTextUnitVariant(tmTextUnit2Id, jaLocaleId, "新しい世界をこんにちは! 3");

    // only rollback text unit 2 and ja-JP
    CurrentVariantRollbackParameters rollbackParameters = new CurrentVariantRollbackParameters();
    rollbackParameters.setLocaleIds(Arrays.asList(jaLocaleId));
    rollbackParameters.setTmTextUnitIds(Arrays.asList(tmTextUnit2Id));

    currentVariantRollbackService.rollbackCurrentVariantsFromTMToDate(
        dateTimeBeforeChangingCurrentVariant, tm.getId(), rollbackParameters);

    assertVariantContentForCurrentVariantEquals(
        "After rollback, the current variant 1 should not have changed for fr-FR",
        "Bonjour le monde! 3",
        frLocaleId,
        tmTextUnit1Id);
    assertVariantContentForCurrentVariantEquals(
        "After rollback, the current variant 2 should not have changed for fr-FR",
        "Bonjour le nouveau monde! 3",
        frLocaleId,
        tmTextUnit2Id);
    assertVariantContentForCurrentVariantEquals(
        "After rollback, the current variant 1 should not have changed for ja-JP",
        "こんにちは、世界! 3",
        jaLocaleId,
        tmTextUnit1Id);
    assertVariantContentForCurrentVariantEquals(
        "After rollback, the current variant 2 should have been rolled back for ja-JP",
        "新しい世界をこんにちは!",
        jaLocaleId,
        tmTextUnit2Id);
  }

  /**
   * Asserts whether English is the only variant present for the given {@link
   * com.box.l10n.mojito.entity.TMTextUnit#id}
   *
   * @param tmTextUnitId {@link com.box.l10n.mojito.entity.TMTextUnit#id}
   */
  protected void assertOnlyEnglishVariantIsPresent(Long tmTextUnitId) {
    List<TMTextUnitCurrentVariant> currentVariants =
        tmTextUnitCurrentVariantRepository.findByTmTextUnit_Id(tmTextUnitId);
    assertEquals("There should only be the English variant yet", 1, currentVariants.size());
  }

  /**
   * Asserts that there is a current variant associated to the given tmTextUnit and locales
   *
   * @param errorMessage Error message
   * @param localeIds List of {@link com.box.l10n.mojito.entity.Locale#id}s
   * @param tmTextUnitId {@link com.box.l10n.mojito.entity.TMTextUnit#id}
   */
  protected void assertCurrentVariantWasSet(
      String errorMessage, List<Long> localeIds, Long tmTextUnitId) {
    for (Long localeId : localeIds) {
      TMTextUnitCurrentVariant currentVariant =
          tmTextUnitCurrentVariantRepository.findByLocale_IdAndTmTextUnit_Id(
              localeId, tmTextUnitId);
      assertNotNull(errorMessage, currentVariant);
    }
  }

  /**
   * Asserts that there is a current variant associated to the given tmTextUnit and locales
   *
   * @param errorMessage Error message
   * @param localeIds List of {@link com.box.l10n.mojito.entity.Locale#id}s
   * @param tmTextUnitId {@link com.box.l10n.mojito.entity.TMTextUnit#id}
   */
  protected void assertNoCurrentVariantSet(
      String errorMessage, List<Long> localeIds, Long tmTextUnitId) {
    for (Long localeId : localeIds) {
      TMTextUnitCurrentVariant currentVariant =
          tmTextUnitCurrentVariantRepository.findByLocale_IdAndTmTextUnit_Id(
              localeId, tmTextUnitId);
      assertNull(errorMessage, currentVariant);
    }
  }

  /**
   * Asserts that the content of the current variant associated to the given {@link
   * com.box.l10n.mojito.entity.TMTextUnit#id} and {@link com.box.l10n.mojito.entity.Locale#id} is
   * the same as the expected one.
   *
   * @param expectedContent The expected content
   * @param localeId {@link com.box.l10n.mojito.entity.Locale#id}
   * @param tmTextUnitId {@link com.box.l10n.mojito.entity.TMTextUnit#id}
   */
  protected void assertVariantContentForCurrentVariantEquals(
      String expectedContent, Long localeId, Long tmTextUnitId) {
    assertVariantContentForCurrentVariantEquals(null, expectedContent, localeId, tmTextUnitId);
  }

  /**
   * Asserts that the content of the current variant associated to the given {@link
   * com.box.l10n.mojito.entity.TMTextUnit#id} and {@link com.box.l10n.mojito.entity.Locale#id} is
   * the same as the expected one.
   *
   * @param errorMessage Error message
   * @param expectedContent The expected content
   * @param localeId {@link com.box.l10n.mojito.entity.Locale#id}
   * @param tmTextUnitId {@link com.box.l10n.mojito.entity.TMTextUnit#id}
   */
  protected void assertVariantContentForCurrentVariantEquals(
      String errorMessage, String expectedContent, Long localeId, Long tmTextUnitId) {
    TMTextUnitCurrentVariant currentVariant =
        tmTextUnitCurrentVariantRepository.findByLocale_IdAndTmTextUnit_Id(localeId, tmTextUnitId);
    assertEquals(errorMessage, expectedContent, currentVariant.getTmTextUnitVariant().getContent());
  }
}

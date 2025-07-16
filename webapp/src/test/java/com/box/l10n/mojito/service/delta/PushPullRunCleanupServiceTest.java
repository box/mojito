package com.box.l10n.mojito.service.delta;

import static com.box.l10n.mojito.service.delta.CleanPushPullPerAssetConfigurationProperties.DayRange;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.box.l10n.mojito.entity.Asset;
import com.box.l10n.mojito.entity.Locale;
import com.box.l10n.mojito.entity.PullRun;
import com.box.l10n.mojito.entity.PullRunAsset;
import com.box.l10n.mojito.entity.PushRun;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.TM;
import com.box.l10n.mojito.entity.TMTextUnit;
import com.box.l10n.mojito.entity.TMTextUnitVariant;
import com.box.l10n.mojito.service.DBUtils;
import com.box.l10n.mojito.service.asset.AssetService;
import com.box.l10n.mojito.service.assetExtraction.ServiceTestBase;
import com.box.l10n.mojito.service.locale.LocaleService;
import com.box.l10n.mojito.service.pullrun.PullRunAssetRepository;
import com.box.l10n.mojito.service.pullrun.PullRunAssetService;
import com.box.l10n.mojito.service.pullrun.PullRunRepository;
import com.box.l10n.mojito.service.pullrun.PullRunService;
import com.box.l10n.mojito.service.pullrun.PullRunTextUnitVariantRepository;
import com.box.l10n.mojito.service.pushrun.PushRunAssetRepository;
import com.box.l10n.mojito.service.pushrun.PushRunRepository;
import com.box.l10n.mojito.service.pushrun.PushRunService;
import com.box.l10n.mojito.service.repository.RepositoryNameAlreadyUsedException;
import com.box.l10n.mojito.service.repository.RepositoryService;
import com.box.l10n.mojito.service.tm.TMRepository;
import com.box.l10n.mojito.service.tm.TMService;
import com.box.l10n.mojito.test.TestIdWatcher;
import com.google.common.collect.ImmutableList;
import jakarta.persistence.EntityManager;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

public class PushPullRunCleanupServiceTest extends ServiceTestBase {

  @Rule public TestIdWatcher testIdWatcher = new TestIdWatcher();

  @Autowired EntityManager entityManager;

  @Autowired PushPullRunCleanupService pushPullRunCleanupService;

  @Autowired AssetService assetService;

  @Autowired PushRunService pushRunService;

  @Autowired PushRunRepository pushRunRepository;

  @Autowired PushRunAssetRepository pushRunAssetRepository;

  @Autowired PullRunService pullRunService;

  @Autowired PullRunRepository pullRunRepository;

  @Autowired PullRunAssetService pullRunAssetService;

  @Autowired PullRunAssetRepository pullRunAssetRepository;

  @Autowired PullRunTextUnitVariantRepository pullRunTextUnitVariantRepository;

  @Autowired RepositoryService repositoryService;

  @Autowired LocaleService localeService;

  @Autowired DBUtils dbUtils;

  @Autowired TMService tmService;

  @Autowired CleanPushPullPerAssetConfigurationProperties configurationProperties;

  TM tm;

  Repository repository;

  Asset asset;

  @Autowired TMRepository tmRepository;

  @Before
  public void before() throws RepositoryNameAlreadyUsedException {
    DayRange dayRange = new DayRange(1, 31);
    this.configurationProperties.setDayRanges(ImmutableList.of(dayRange));
    if (tm == null) {
      tm = new TM();
      tmRepository.save(tm);
    }

    if (repository == null) {
      repository = repositoryService.createRepository(testIdWatcher.getEntityName("repository"));
      asset =
          assetService.createAssetWithContent(
              repository.getId(), "path/to/asset", "test asset content");
    }
  }

  @Transactional
  @Test
  public void testCleanOldPushPullData() throws Exception {
    Assume.assumeTrue(dbUtils.isMysql());
    Repository repository =
        repositoryService.createRepository(
            testIdWatcher.getEntityName("repository") + "testCleanOldPushPullData");

    PushRun firstPushRun = pushRunService.createPushRun(repository, "firstCleanOldPushPullData");
    Assert.assertTrue(pushRunAssetRepository.findByPushRun(firstPushRun).isEmpty());

    TMTextUnit firstTmTextUnit =
        tmService.addTMTextUnit(
            tm.getId(),
            asset.getId(),
            "hello_world 3",
            "Hello World!",
            "Comments about hello world");
    TMTextUnit firstTmTextUnit2 =
        tmService.addTMTextUnit(
            tm.getId(),
            asset.getId(),
            "hello_world 4",
            "Hello World!",
            "Comments about hello world");

    pushRunService.associatePushRunToTextUnitIds(
        firstPushRun, asset, Arrays.asList(firstTmTextUnit.getId(), firstTmTextUnit2.getId()));
    List<TMTextUnit> newTextUnits =
        pushRunService.getPushRunTextUnits(firstPushRun, PageRequest.of(0, Integer.MAX_VALUE));
    Assert.assertEquals(2, newTextUnits.size());

    PushRun pushRun = pushRunService.createPushRun(repository, "cleanOldPushPullData");
    Assert.assertTrue(pushRunAssetRepository.findByPushRun(pushRun).isEmpty());

    TMTextUnit tmTextUnit1 =
        tmService.addTMTextUnit(
            tm.getId(),
            asset.getId(),
            "hello_world 1",
            "Hello World!",
            "Comments about hello world");
    TMTextUnit tmTextUnit2 =
        tmService.addTMTextUnit(
            tm.getId(),
            asset.getId(),
            "hello_world 2",
            "Hello World!",
            "Comments about hello world");

    pushRunService.associatePushRunToTextUnitIds(
        pushRun, asset, Arrays.asList(tmTextUnit1.getId(), tmTextUnit2.getId()));
    List<TMTextUnit> textUnits =
        pushRunService.getPushRunTextUnits(pushRun, PageRequest.of(0, Integer.MAX_VALUE));
    Assert.assertEquals(2, textUnits.size());

    PullRun firstPullRun = pullRunService.getOrCreate("firstCleanOldPushPullData", repository);
    PullRunAsset firstPullRunAsset = pullRunAssetService.createPullRunAsset(firstPullRun, asset);

    Locale frFR = localeService.findByBcp47Tag("fr-FR");
    TMTextUnitVariant firstTuv1 =
        tmService.addCurrentTMTextUnitVariant(
            firstTmTextUnit.getId(), frFR.getId(), "le hello_world 3");

    TMTextUnitVariant firstTuv2 =
        tmService.addCurrentTMTextUnitVariant(
            firstTmTextUnit2.getId(), frFR.getId(), "le hello_world 4");

    pullRunAssetService.replaceTextUnitVariants(
        firstPullRunAsset,
        frFR.getId(),
        Arrays.asList(firstTuv1.getId(), firstTuv2.getId()),
        "fr-FR");
    List<TMTextUnitVariant> firstRecordedVariants =
        pullRunTextUnitVariantRepository.findByPullRun(firstPullRun, Pageable.unpaged());
    Assert.assertEquals(2, firstRecordedVariants.size());

    PullRun pullRun = pullRunService.getOrCreate("cleanOldPushPullData", repository);
    PullRunAsset pullRunAsset = pullRunAssetService.createPullRunAsset(pullRun, asset);

    TMTextUnitVariant tuv1 =
        tmService.addCurrentTMTextUnitVariant(
            tmTextUnit1.getId(), frFR.getId(), "le hello_world 1");

    TMTextUnitVariant tuv2 =
        tmService.addCurrentTMTextUnitVariant(
            tmTextUnit1.getId(), frFR.getId(), "le hello_world 2");

    pullRunAssetService.replaceTextUnitVariants(
        pullRunAsset, frFR.getId(), Arrays.asList(tuv1.getId(), tuv2.getId()), "fr-FR");
    List<TMTextUnitVariant> recordedVariants =
        pullRunTextUnitVariantRepository.findByPullRun(pullRun, Pageable.unpaged());
    Assert.assertEquals(2, recordedVariants.size());

    Thread.sleep(500);

    // This should only delete the first push and pull runs
    pushPullRunCleanupService.cleanOldPushPullData(Duration.ofDays(1000));

    entityManager.flush();
    entityManager.clear();

    Assert.assertEquals(
        0,
        pushRunService
            .getPushRunTextUnits(firstPushRun, PageRequest.of(0, Integer.MAX_VALUE))
            .size());
    Assert.assertEquals(
        0, pullRunTextUnitVariantRepository.findByPullRun(firstPullRun, Pageable.unpaged()).size());
    assertTrue(pushRunRepository.findById(firstPushRun.getId()).isEmpty());
    assertTrue(pullRunRepository.findById(firstPullRun.getId()).isEmpty());
    Assert.assertEquals(
        2,
        pushRunService.getPushRunTextUnits(pushRun, PageRequest.of(0, Integer.MAX_VALUE)).size());
    Assert.assertEquals(
        2, pullRunTextUnitVariantRepository.findByPullRun(pullRun, Pageable.unpaged()).size());

    // This should delete all of the old data
    pushPullRunCleanupService.cleanOldPushPullData(Duration.ofSeconds(-1));

    entityManager.flush();
    entityManager.clear();

    Assert.assertEquals(
        0,
        pushRunService.getPushRunTextUnits(pushRun, PageRequest.of(0, Integer.MAX_VALUE)).size());
    Assert.assertEquals(
        0, pullRunTextUnitVariantRepository.findByPullRun(pullRun, Pageable.unpaged()).size());

    assertFalse(pushRunRepository.findById(pushRun.getId()).isPresent());
    assertFalse(pullRunRepository.findById(pullRun.getId()).isPresent());
  }
}

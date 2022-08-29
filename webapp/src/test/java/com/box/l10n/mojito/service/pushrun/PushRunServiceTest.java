package com.box.l10n.mojito.service.pushrun;

import com.box.l10n.mojito.entity.Asset;
import com.box.l10n.mojito.entity.PushRun;
import com.box.l10n.mojito.entity.PushRunAsset;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.TM;
import com.box.l10n.mojito.entity.TMTextUnit;
import com.box.l10n.mojito.service.asset.AssetService;
import com.box.l10n.mojito.service.assetExtraction.ServiceTestBase;
import com.box.l10n.mojito.service.repository.RepositoryNameAlreadyUsedException;
import com.box.l10n.mojito.service.repository.RepositoryService;
import com.box.l10n.mojito.service.tm.TMRepository;
import com.box.l10n.mojito.service.tm.TMService;
import com.box.l10n.mojito.test.TestIdWatcher;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

/** @author garion */
public class PushRunServiceTest extends ServiceTestBase {

  @Rule public TestIdWatcher testIdWatcher = new TestIdWatcher();

  @Autowired AssetService assetService;

  @Autowired PushRunService pushRunService;

  @Autowired PushRunAssetRepository pushRunAssetRepository;

  @Autowired PushRunAssetService pushRunAssetService;

  @Autowired PushRunAssetTmTextUnitRepository pushRunAssetTmTextUnitRepository;

  @Autowired PushRunAssetTmTextUnitService pushRunAssetTmTextUnitService;

  @Autowired RepositoryService repositoryService;

  @Autowired TMRepository tmRepository;

  @Autowired TMService tmService;

  TM tm;

  Repository repository;

  Asset asset;

  @Before
  public void before() throws RepositoryNameAlreadyUsedException {
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

  @Test
  public void testCreatePushRun() {
    String pushRunName = "testCreatePushRun";

    PushRun pushRun = pushRunService.createPushRun(repository, pushRunName);

    Assert.assertNotNull(pushRun);
    Assert.assertEquals(pushRunName, pushRun.getName());
  }

  @Test
  public void testClearPushRunLinkedData() {
    PushRun pushRun = pushRunService.createPushRun(repository, "testClearPushRunLinkedData");

    PushRunAsset pushRunAsset = pushRunAssetService.createPushRunAsset(pushRun, asset);
    Assert.assertFalse(pushRunAssetRepository.findByPushRun(pushRun).isEmpty());

    TMTextUnit tmTextUnit =
        tmService.addTMTextUnit(
            tm.getId(), asset.getId(), "hello_world", "Hello World!", "Comments about hello world");
    pushRunAssetTmTextUnitService.createPushRunAssetTmTextUnit(pushRunAsset, tmTextUnit);
    Assert.assertFalse(
        pushRunAssetTmTextUnitRepository
            .findByPushRunAsset(pushRunAsset, PageRequest.of(0, Integer.MAX_VALUE))
            .isEmpty());

    pushRunService.clearPushRunLinkedData(pushRun);
    Assert.assertTrue(pushRunAssetRepository.findByPushRun(pushRun).isEmpty());
    Assert.assertTrue(
        pushRunAssetTmTextUnitRepository
            .findByPushRunAsset(pushRunAsset, PageRequest.of(0, Integer.MAX_VALUE))
            .isEmpty());
  }

  @Test
  public void testAssociatePushRunToTextUnits() {
    PushRun pushRun = pushRunService.createPushRun(repository, "testAssociatePushRunToTextUnits");
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
  }

  @Test
  public void testAssociatePushRunToTextUnitsWithExistingData() {
    PushRun pushRun =
        pushRunService.createPushRun(repository, "testAssociatePushRunToTextUnitsWithExistingData");
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

    TMTextUnit tmTextUnit3 =
        tmService.addTMTextUnit(
            tm.getId(),
            asset.getId(),
            "hello_world 3",
            "Hello World!",
            "Comments about hello world");
    pushRunService.associatePushRunToTextUnitIds(
        pushRun, asset, Collections.singletonList(tmTextUnit3.getId()));
    textUnits = pushRunService.getPushRunTextUnits(pushRun, PageRequest.of(0, Integer.MAX_VALUE));
    Assert.assertEquals(1, textUnits.size());
    Assert.assertTrue(textUnits.stream().findFirst().isPresent());
    Assert.assertEquals(tmTextUnit3.getId(), textUnits.stream().findFirst().get().getId());
  }

  @Test
  public void testAssociatePushRunToTextUnitIds() {
    PushRun pushRun = pushRunService.createPushRun(repository, "testAssociatePushRunToTextUnitIds");
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
  }
}

package com.box.l10n.mojito.service.pullrun;

import com.box.l10n.mojito.entity.Asset;
import com.box.l10n.mojito.entity.PullRun;
import com.box.l10n.mojito.entity.PullRunAsset;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.TMTextUnitVariant;
import com.box.l10n.mojito.service.asset.AssetService;
import com.box.l10n.mojito.service.assetExtraction.ServiceTestBase;
import com.box.l10n.mojito.service.repository.RepositoryNameAlreadyUsedException;
import com.box.l10n.mojito.service.tm.TMTestData;
import com.box.l10n.mojito.test.TestIdWatcher;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author garion
 */
public class PullRunServiceTest extends ServiceTestBase {

    @Rule
    public TestIdWatcher testIdWatcher = new TestIdWatcher();

    @Autowired
    AssetService assetService;

    @Autowired
    PullRunService pullRunService;

    @Autowired
    PullRunAssetRepository pullRunAssetRepository;

    @Autowired
    PullRunAssetService pullRunAssetService;

    @Autowired
    PullRunTextUnitVariantRepository pullRunTextUnitVariantRepository;

    @Autowired
    PullRunTextUnitVariantService pullRunTextUnitVariantService;

    TMTestData tmTestData;

    Repository repository;

    Asset asset;

    @Before
    public void before() throws RepositoryNameAlreadyUsedException {
        if (tmTestData == null) {
            tmTestData = new TMTestData(testIdWatcher);
            asset = tmTestData.asset;
            repository = tmTestData.repository;
        }
    }

    @Test
    public void testCreatePullRun() {
        String pullRunName = "testCreatePullRun";

        PullRun pullRun = pullRunService.createPullRun(repository, pullRunName);

        Assert.assertNotNull(pullRun);
        Assert.assertEquals(pullRunName, pullRun.getName());
    }

    @Test
    public void testClearPullRunLinkedData() {
        // TODO(jean) how is this not colliding between test run? usually we need getEntity
        PullRun pullRun = pullRunService.createPullRun(repository, "testClearPullRunLinkedData");

        PullRunAsset pullRunAsset = pullRunAssetService.createPullRunAsset(pullRun, asset);
        Assert.assertFalse(pullRunAssetRepository.findByPullRun(pullRun).isEmpty());

        pullRunTextUnitVariantService.createPullRunTextUnitVariant(pullRunAsset,
                                                                   tmTestData.addCurrentTMTextUnitVariant1FrFR);
        Assert.assertFalse(pullRunTextUnitVariantRepository.findByPullRunAsset(pullRunAsset, PageRequest.of(0,
                                                                                                            Integer.MAX_VALUE))
                                   .isEmpty());

        pullRunService.clearPullRunLinkedData(pullRun);
        Assert.assertTrue(pullRunTextUnitVariantRepository.findByPullRunAsset(pullRunAsset, PageRequest.of(0,
                                                                                                           Integer.MAX_VALUE))
                                  .isEmpty());
    }

    @Test
    public void testAssociatePullRunToTextUnitVariants() {
        PullRun pullRun = pullRunService.createPullRun(repository, "associatePullRunToTextUnitVariants");

        pullRunService.associatePullRunToTextUnitVariants(pullRun, asset,
                                                          Arrays.asList(tmTestData.addCurrentTMTextUnitVariant1FrFR,
                                                                        tmTestData.addCurrentTMTextUnitVariant2FrCA));

        List<TMTextUnitVariant> tmTextUnitVariants = pullRunService.getTextUnitVariants(pullRun, PageRequest.of(0,
                                                                                                                Integer.MAX_VALUE));
        Assert.assertEquals(2, tmTextUnitVariants.size());
    }


    @Test
    public void testAssociatePullRunToTextUnitVariantsWithExistingData() {
        PullRun pullRun = pullRunService.createPullRun(repository, "associatePullRunToTextUnitVariantsWithExistingData");

        pullRunService.associatePullRunToTextUnitVariants(pullRun, asset,
                                                          Arrays.asList(tmTestData.addCurrentTMTextUnitVariant1FrFR,
                                                                        tmTestData.addCurrentTMTextUnitVariant2FrCA));

        List<TMTextUnitVariant> tmTextUnitVariants = pullRunService.getTextUnitVariants(pullRun, PageRequest.of(0,
                                                                                                                Integer.MAX_VALUE));
        Assert.assertEquals(2, tmTextUnitVariants.size());

        pullRunService.associatePullRunToTextUnitVariants(pullRun, asset,
                                                          Collections.singletonList(tmTestData.addCurrentTMTextUnitVariant2FrCA));
        tmTextUnitVariants = pullRunService.getTextUnitVariants(pullRun, PageRequest.of(0, Integer.MAX_VALUE));
        Assert.assertEquals(1, tmTextUnitVariants.size());
    }

    @Test
    public void testAssociatePullRunToTextUnitIds() {
        PullRun pullRun = pullRunService.createPullRun(repository, "associatePullRunToTextUnitIds");

        pullRunService.associatePullRunToTextUnitIds(pullRun, asset,
                                                     Arrays.asList(tmTestData.addCurrentTMTextUnitVariant1FrFR.getId(),
                                                                   tmTestData.addCurrentTMTextUnitVariant2FrCA.getId()));

        List<TMTextUnitVariant> tmTextUnitVariants = pullRunService.getTextUnitVariants(pullRun, PageRequest.of(0,
                                                                                                                Integer.MAX_VALUE));
        Assert.assertEquals(2, tmTextUnitVariants.size());
    }
}
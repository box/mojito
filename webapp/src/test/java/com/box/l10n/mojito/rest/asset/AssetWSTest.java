package com.box.l10n.mojito.rest.asset;

import com.box.l10n.mojito.entity.Asset;
import com.box.l10n.mojito.entity.AssetTextUnit;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.rest.WSTestBase;
import com.box.l10n.mojito.rest.WSTestDataFactory;
import com.box.l10n.mojito.rest.client.AssetClient;
import com.box.l10n.mojito.rest.client.PollableTaskClient;
import com.box.l10n.mojito.rest.client.exception.RepositoryNotFoundException;
import com.box.l10n.mojito.service.asset.AssetRepository;
import com.box.l10n.mojito.service.assetTextUnit.AssetTextUnitRepository;
import com.box.l10n.mojito.service.locale.LocaleService;
import com.box.l10n.mojito.service.repository.RepositoryNameAlreadyUsedException;
import com.box.l10n.mojito.service.repository.RepositoryService;
import com.box.l10n.mojito.test.TestIdWatcher;
import com.box.l10n.mojito.test.category.IntegrationTest;
import com.google.common.collect.Sets;
import java.util.List;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author aloison
 */
public class AssetWSTest extends WSTestBase {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(AssetWSTest.class);

    @Autowired
    WSTestDataFactory testDataFactory;

    @Autowired
    RepositoryService repositoryService;

    @Autowired
    AssetRepository assetRepository;

    @Autowired
    AssetTextUnitRepository assetTextUnitRepository;

    @Autowired
    LocaleService localeService;

    @Autowired
    AssetClient assetClient;

    @Autowired
    PollableTaskClient pollableTaskClient;

    @Rule
    public TestIdWatcher testIdWatcher = new TestIdWatcher();

    @Test
    @Category({IntegrationTest.class})
    public void testImportSourceAsset() throws RepositoryNameAlreadyUsedException {

        Repository repository = testDataFactory.createRepository(testIdWatcher);
        com.box.l10n.mojito.rest.entity.SourceAsset sourceAsset = createSourceAsset(repository);
        com.box.l10n.mojito.rest.entity.SourceAsset sourceAssetAfterPost = assetClient.sendSourceAsset(sourceAsset);

        pollableTaskClient.waitForPollableTask(sourceAssetAfterPost.getPollableTask().getId(), 5000L);

        Asset addedAsset = assetRepository.findOne(sourceAssetAfterPost.getAddedAssetId());
        assertNotNull("The asset should have been created", addedAsset);

        assertEquals(sourceAsset.getRepositoryId(), addedAsset.getRepository().getId());
        assertEquals(sourceAsset.getPath(), addedAsset.getPath());
        assertEquals(sourceAsset.getContent(), addedAsset.getContent());

        assertNotNull("Extraction of the asset should have completed", addedAsset.getLastSuccessfulAssetExtraction());

        List<AssetTextUnit> assetTextUnits = assetTextUnitRepository.findByAssetExtraction(addedAsset.getLastSuccessfulAssetExtraction());

        assertEquals("There should be 3 asset text units extracted", 3, assetTextUnits.size());
        assertEquals("Account_security_and_password_settings", assetTextUnits.get(2).getName());
    }

    @Test
    public void testDeleteAssetById() throws RepositoryNotFoundException, RepositoryNameAlreadyUsedException {
        Repository repository = testDataFactory.createRepository(testIdWatcher);
        List<com.box.l10n.mojito.rest.entity.Asset> assets = assetClient.getAssetsByRepositoryId(repository.getId());
        assertEquals("There should be no asset for this repository yet", 0, assets.size());
        
        com.box.l10n.mojito.rest.entity.SourceAsset sourceAsset = createSourceAsset(repository);
        com.box.l10n.mojito.rest.entity.SourceAsset sourceAssetAfterPost = assetClient.sendSourceAsset(sourceAsset);
        pollableTaskClient.waitForPollableTask(sourceAssetAfterPost.getPollableTask().getId(), 5000L);

        Asset asset = assetRepository.findOne(sourceAssetAfterPost.getAddedAssetId());
        Long assetId = asset.getId();
        assertNotNull("The asset should have been created", asset);
        assertFalse("The asset should have not been deleted yet", asset.getDeleted());
        assets = assetClient.getAssetsByRepositoryId(repository.getId());
        assertEquals("There should be one asset for this repository", 1, assets.size());
        assets = assetClient.getAssetsByRepositoryId(repository.getId(), false);
        assertEquals("There should be one asset that is not deleted for this repository", 1, assets.size());
        assets = assetClient.getAssetsByRepositoryId(repository.getId(), true);
        assertEquals("There should be no deleted asset for this repository", 0, assets.size());
        
        assetClient.deleteAssetById(assetId);
        asset = assetRepository.findOne(sourceAssetAfterPost.getAddedAssetId());
        assertEquals("The asset id should have not changed", assetId, asset.getId());
        assertTrue("The asset should be deleted", asset.getDeleted());
        assets = assetClient.getAssetsByRepositoryId(repository.getId());
        assertEquals("There should be one asset for this repository", 1, assets.size());
        assets = assetClient.getAssetsByRepositoryId(repository.getId(), false);
        assertEquals("There should be no asset that is not deleted for this repository", 0, assets.size());
        assets = assetClient.getAssetsByRepositoryId(repository.getId(), true);
        assertEquals("There should be one deleted asset for this repository", 1, assets.size());
    }
    
    @Test
    public void testDeleteUnusedAssets() throws RepositoryNotFoundException, RepositoryNameAlreadyUsedException {
        Repository repository = testDataFactory.createRepository(testIdWatcher);
        List<com.box.l10n.mojito.rest.entity.Asset> assets = assetClient.getAssetsByRepositoryId(repository.getId());
        assertEquals("There should be no asset for this repository yet", 0, assets.size());
        
        String path1 = "/path/to/fake/file1.xliff";
        String path2 = "/path/to/fake/file2.xliff"; 
        String path3 = "/path/to/fake/file3.xliff"; 
        com.box.l10n.mojito.rest.entity.SourceAsset sourceAsset1 = createSourceAsset(repository);
        sourceAsset1.setPath(path1);
        com.box.l10n.mojito.rest.entity.SourceAsset sourceAssetAfterPost1 = assetClient.sendSourceAsset(sourceAsset1);
        pollableTaskClient.waitForPollableTask(sourceAssetAfterPost1.getPollableTask().getId(), 5000L);

        com.box.l10n.mojito.rest.entity.SourceAsset sourceAsset2 = createSourceAsset(repository);
        sourceAsset2.setPath(path2);
        com.box.l10n.mojito.rest.entity.SourceAsset sourceAssetAfterPost2 = assetClient.sendSourceAsset(sourceAsset2);
        pollableTaskClient.waitForPollableTask(sourceAssetAfterPost2.getPollableTask().getId(), 5000L);
        
        com.box.l10n.mojito.rest.entity.SourceAsset sourceAsset3 = createSourceAsset(repository);
        sourceAsset2.setPath(path3);
        com.box.l10n.mojito.rest.entity.SourceAsset sourceAssetAfterPost3 = assetClient.sendSourceAsset(sourceAsset2);
        pollableTaskClient.waitForPollableTask(sourceAssetAfterPost3.getPollableTask().getId(), 5000L);
        
        assets = assetClient.getAssetsByRepositoryId(repository.getId());
        assertEquals("There should be three assets for this repository", 3, assets.size());
        
        assetClient.deleteAssetsByIds(Sets.newHashSet(sourceAssetAfterPost2.getAddedAssetId(), sourceAssetAfterPost3.getAddedAssetId()));
        assets = assetClient.getAssetsByRepositoryId(repository.getId());
        assertEquals("There should be three assets for this repository", 3, assets.size());
        assets = assetClient.getAssetsByRepositoryId(repository.getId(), false);
        assertEquals("There should be one undeleted asset for this repository", 1, assets.size());
        assertEquals("Asset with path1 should not be deleted", path1, assets.get(0).getPath());
        assets = assetClient.getAssetsByRepositoryId(repository.getId(), true);
        assertEquals("There should be two deleted assets for this repository", 2, assets.size());
        assertTrue(assets.get(0).getPath().equals(path2) || assets.get(0).getPath().equals(path3));
        assertTrue(assets.get(1).getPath().equals(path2) || assets.get(1).getPath().equals(path3));
        
        List<Long> assetIds = assetClient.getAssetIds(repository.getId(), null);
        assertEquals("There should be three asset ids for this repository", 3, assetIds.size());
        assetIds = assetClient.getAssetIds(repository.getId(), false);
        assertEquals("There should be one undeleted asset id for this repository", 1, assetIds.size());
        assertEquals(sourceAssetAfterPost1.getAddedAssetId(), assetIds.iterator().next());
        assetIds = assetClient.getAssetIds(repository.getId(), true);
        assertEquals("There should be two deleted asset ids for this repository", 2, assetIds.size());
        assertTrue(assetIds.iterator().next().equals(sourceAssetAfterPost2.getAddedAssetId()) || assetIds.iterator().next().equals(sourceAssetAfterPost3.getAddedAssetId()));  
        assertTrue(assetIds.iterator().next().equals(sourceAssetAfterPost2.getAddedAssetId()) || assetIds.iterator().next().equals(sourceAssetAfterPost3.getAddedAssetId()));  
    }
    
    protected com.box.l10n.mojito.rest.entity.SourceAsset createSourceAsset(Repository repository) {
        com.box.l10n.mojito.rest.entity.SourceAsset sourceAsset = new com.box.l10n.mojito.rest.entity.SourceAsset();
        sourceAsset.setRepositoryId(repository.getId());
        sourceAsset.setPath("/path/to/fake/file.xliff");
        sourceAsset.setContent(testDataFactory.getTestSourceAssetContent());

        return sourceAsset;
    }
}

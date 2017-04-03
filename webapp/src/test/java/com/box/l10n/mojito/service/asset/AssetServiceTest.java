package com.box.l10n.mojito.service.asset;

import com.box.l10n.mojito.entity.Asset;
import com.box.l10n.mojito.entity.AssetExtraction;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.service.assetExtraction.AssetExtractionRepository;
import com.box.l10n.mojito.service.assetExtraction.AssetExtractionService;
import com.box.l10n.mojito.service.assetExtraction.ServiceTestBase;
import com.box.l10n.mojito.service.pollableTask.PollableFuture;
import com.box.l10n.mojito.service.pollableTask.PollableTaskException;
import com.box.l10n.mojito.service.pollableTask.PollableTaskService;
import com.box.l10n.mojito.service.repository.RepositoryService;
import com.box.l10n.mojito.test.TestIdWatcher;
import com.google.common.collect.Sets;
import java.util.List;
import java.util.Set;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import static org.slf4j.LoggerFactory.getLogger;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author aloison
 */
public class AssetServiceTest extends ServiceTestBase {

    /**
     * logger
     */
    static Logger logger = getLogger(AssetServiceTest.class);

    @Autowired
    AssetService assetService;

    @Autowired
    AssetExtractionService assetExtractionService;

    @Autowired
    AssetRepository assetRepository;

    @Autowired
    AssetExtractionRepository assetExtractionRepository;

    @Autowired
    RepositoryService repositoryService;

    @Autowired
    PollableTaskService pollableTaskService;

    @Rule
    public TestIdWatcher testIdWatcher = new TestIdWatcher();

    @Test
    public void testAddOrUpdateAssetAndProcessIfNeededShouldStartExtractionIfAssetExistButAssetExtractionIsMissing() throws Exception {

        String content = "content";
        String path = "path/to/asset.csv";

        Repository repository = repositoryService.createRepository(testIdWatcher.getEntityName("repository"));

        Asset asset = assetService.createAsset(repository.getId(), content, path);
        addAssetAndWaitUntilDoneProcessing(repository.getId(), content, path);

        List<AssetExtraction> assetExtractions = assetExtractionRepository.findByAsset(asset);

        assertEquals("There should be 1 assetExtraction created when updating an exising asset without assetExtraction (due to some processing failure)", 1, assetExtractions.size());
    }

    @Test
    public void testAddOrUpdateAssetAndProcessIfNeededShouldStartExtractionIfAssetExistButAssetExtractionIsOutdated() throws Exception {

        String content = "content";
        String path = "path/to/asset.csv";

        Repository repository = repositoryService.createRepository(testIdWatcher.getEntityName("repository"));

        Asset asset = assetService.createAsset(repository.getId(), content, path);

        Asset fakePreviousAssetVersion = new Asset();
        fakePreviousAssetVersion.setId(asset.getId());
        fakePreviousAssetVersion.setContentMd5("TEST OUTDATED MD5");
        AssetExtraction createAssetExtraction = assetExtractionService.createAssetExtraction(fakePreviousAssetVersion, null);
        assetExtractionService.markAssetExtractionAsLastSuccessful(asset, createAssetExtraction);

        addAssetAndWaitUntilDoneProcessing(repository.getId(), content, path);

        List<AssetExtraction> assetExtractions = assetExtractionRepository.findByAsset(asset);

        assertEquals("There should be 1 assetExtraction created when updating an exising asset with outdated assetExtraction (due to some processing failure)", 2, assetExtractions.size());
    }

    @Test
    public void testAddOrUpdateAssetAndProcessIfNeededShouldNotStartExtractionIfAssetAlreadyExists() throws Exception {

        String content = "content";
        String path = "path/to/asset.csv";

        Repository repository = repositoryService.createRepository(testIdWatcher.getEntityName("repository"));

        Asset asset = addAssetAndWaitUntilDoneProcessing(repository.getId(), content, path);

        addAssetAndWaitUntilDoneProcessing(repository.getId(), content, path);

        List<AssetExtraction> assetExtractions = assetExtractionRepository.findByAsset(asset);

        assertEquals("There should be no assetExtraction created when adding an already existing asset", 1, assetExtractions.size());
    }

    @Test
    public void testAddOrUpdateAssetAndProcessIfNeededShouldStartExtractionIfNewAsset() throws Exception {

        String content = "newContent";
        String path = "path/to/new/asset.csv";

        Repository repository = repositoryService.createRepository(testIdWatcher.getEntityName("repository"));

        Asset asset = addAssetAndWaitUntilDoneProcessing(repository.getId(), content, path);

        List<AssetExtraction> assetExtractions = assetExtractionRepository.findByAsset(asset);

        assertEquals("There should be one assetExtraction created when adding a new asset", 1, assetExtractions.size());
    }

    @Test
    public void testAddOrUpdateAssetAndProcessIfNeededShouldUpdateAssetContentAndStartExtractionIfContentOfExistingAssetChanged() throws Exception {

        String content = "content";
        String newContent = "newContent";
        String path = "path/to/existing/asset.csv";

        Repository repository = repositoryService.createRepository(testIdWatcher.getEntityName("repository"));

        assetService.createAsset(repository.getId(), content, path);

        Asset asset = addAssetAndWaitUntilDoneProcessing(repository.getId(), newContent, path);

        assertEquals("Content of existing asset should be updated if changed", newContent, asset.getContent());

        List<AssetExtraction> assetExtractions = assetExtractionRepository.findByAsset(asset);

        assertEquals("There should be one assetExtraction created when the adding an asset with changed content", 1, assetExtractions.size());
    }

    private Asset addAssetAndWaitUntilDoneProcessing(Long repositoryId, String assetContent, String assetPath) throws Exception {

        PollableFuture<Asset> assetResult = assetService.addOrUpdateAssetAndProcessIfNeeded(repositoryId, assetContent, assetPath, null);

        try {
            pollableTaskService.waitForPollableTask(assetResult.getPollableTask().getId());
        } catch (PollableTaskException | InterruptedException e) {
            throw new RuntimeException(e);
        }

        return assetResult.get();
    }
    
    @Test
    public void testAddOrUpdateAssetAndProcessIfNeededShouldUpdateAssetContentAndStartExtractionIfAssetDeletedAndAssetExtractionIsOutdated() throws Exception {

        String content = "content";
        String newContent = "newContent";
        String path = "path/to/asset.csv";

        Repository repository = repositoryService.createRepository(testIdWatcher.getEntityName("repository"));

        Asset asset = addAssetAndWaitUntilDoneProcessing(repository.getId(), content, path);
        List<AssetExtraction> assetExtractions = assetExtractionRepository.findByAsset(asset);
        assertEquals("There should be one assetExtraction created when the adding an asset with initial content", 1, assetExtractions.size());
        Long assetId = asset.getId();
        
        assetService.deleteAsset(asset);
        assertTrue("The asset should have been deleted", assetRepository.findOne(assetId).getDeleted());

        asset = addAssetAndWaitUntilDoneProcessing(repository.getId(), newContent, path);
        assertEquals("Content of existing asset should be updated if changed", newContent, asset.getContent());
        assertEquals("Asset id should have remained the same", assetId, asset.getId());

        assetExtractions = assetExtractionRepository.findByAsset(asset);
        assertEquals("There should be one more assetExtraction created when the adding an asset with changed content", 2, assetExtractions.size());
        assertFalse("The asset extraction process should un-delete the deleted asset", assetRepository.findOne(assetId).getDeleted());
    }
    
    @Test
    public void testAddOrUpdateAssetAndProcessIfNeededShouldNotUpdateAssetContentAndStartExtractionIfAssetDeletedAndAssetExtractionIsSame() throws Exception {

        String content = "content";
        String path = "path/to/asset.csv";

        Repository repository = repositoryService.createRepository(testIdWatcher.getEntityName("repository"));
        
        Asset asset = assetService.createAsset(repository.getId(), content, path);
        AssetExtraction createAssetExtraction = assetExtractionService.createAssetExtraction(asset, null);
        assetExtractionService.markAssetExtractionAsLastSuccessful(asset, createAssetExtraction);
        List<AssetExtraction> assetExtractions = assetExtractionRepository.findByAsset(asset);
        assertEquals("There should be one assetExtraction created when the adding an asset with initial content", 1, assetExtractions.size());
        Long assetId = asset.getId();
        
        assetService.deleteAsset(asset);
        assertTrue("The asset should have been deleted", assetRepository.findOne(assetId).getDeleted());
        
        addAssetAndWaitUntilDoneProcessing(repository.getId(), content, path);

        assetExtractions = assetExtractionRepository.findByAsset(asset);
        assertEquals("There should be no more assetExtraction created when the adding an asset with same content", 1, assetExtractions.size());
        assertFalse("The asset extraction process should un-delete the deleted asset", assetRepository.findOne(assetId).getDeleted());
    }
    
    @Test
    public void testDeleteAsset() throws Exception {

        String content1 = "content1";
        String content2 = "content2";
        String content3 = "content3";
        String path1 = "path/to/asset1.csv";
        String path2 = "path/to/asset2.csv";
        String path3 = "path/to/asset3.csv";

        Repository repository = repositoryService.createRepository(testIdWatcher.getEntityName("repository"));
        Asset asset1 = assetService.createAsset(repository.getId(), content1, path1);
        addAssetAndWaitUntilDoneProcessing(repository.getId(), content1, path1);
        Asset asset2 = assetService.createAsset(repository.getId(), content2, path2);
        addAssetAndWaitUntilDoneProcessing(repository.getId(), content2, path2);
        Asset asset3 = assetService.createAsset(repository.getId(), content3, path3);
        addAssetAndWaitUntilDoneProcessing(repository.getId(), content3, path3);
        
        assertFalse("asset1 should not be deleted yet", asset1.getDeleted());
        assertFalse("asset2 should not be deleted yet", asset2.getDeleted());
        assertFalse("asset3 should not be deleted", asset3.getDeleted());
        
        assetService.deleteAssets(Sets.newHashSet(asset1.getId(), asset2.getId()));
        asset1 = assetRepository.findByPathAndRepositoryId(path1, repository.getId());
        assertTrue("asset1 should have been deleted", asset1.getDeleted());
        asset2 = assetRepository.findByPathAndRepositoryId(path2, repository.getId());
        assertTrue("asset2 should have been deleted", asset2.getDeleted());
        asset3 = assetRepository.findByPathAndRepositoryId(path3, repository.getId());
        assertFalse("asset3 should not be deleted", asset3.getDeleted());
        
        Set<Long> assetIds = assetRepository.findIdByRepositoryId(repository.getId());
        assertEquals("There should be 3 asset ids", 3, assetIds.size());
        
        assetIds = assetRepository.findIdByRepositoryIdAndDeleted(repository.getId(), false);
        assertEquals("There should be one undeleted asset id", 1, assetIds.size());
        assertEquals(asset3.getId(), assetIds.iterator().next());
        
        assetIds = assetRepository.findIdByRepositoryIdAndDeleted(repository.getId(), true);
        assertEquals("There should be two deleted asset ids", 2, assetIds.size());
        
    }
}

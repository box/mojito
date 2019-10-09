package com.box.l10n.mojito.service.asset;

import com.box.l10n.mojito.entity.Asset;
import com.box.l10n.mojito.entity.AssetExtraction;
import com.box.l10n.mojito.entity.Branch;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.service.assetExtraction.AssetExtractionRepository;
import com.box.l10n.mojito.service.assetExtraction.AssetExtractionService;
import com.box.l10n.mojito.service.assetExtraction.ServiceTestBase;
import com.box.l10n.mojito.service.branch.BranchRepository;
import com.box.l10n.mojito.service.pollableTask.PollableFuture;
import com.box.l10n.mojito.service.pollableTask.PollableTaskException;
import com.box.l10n.mojito.service.pollableTask.PollableTaskService;
import com.box.l10n.mojito.service.repository.RepositoryService;
import com.box.l10n.mojito.test.TestIdWatcher;
import com.google.common.collect.Sets;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;
import static org.slf4j.LoggerFactory.getLogger;

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
    BranchRepository branchRepository;

    @Autowired
    PollableTaskService pollableTaskService;

    @Rule
    public TestIdWatcher testIdWatcher = new TestIdWatcher();

    @Test
    public void testAddOrUpdateAssetAndProcessIfNeededShouldStartExtractionIfAssetExistButAssetExtractionIsMissing() throws Exception {

        String content = "content";
        String path = "path/to/asset.csv";

        Repository repository = repositoryService.createRepository(testIdWatcher.getEntityName("repository"));

        logger.debug("initialize repository ...: {}", repository.getId());


        Asset asset = assetService.createAssetWithContent(repository.getId(), path, content);
        addAssetAndWaitUntilDoneProcessing(repository.getId(), content, path, null);

        List<AssetExtraction> assetExtractions = assetExtractionRepository.findByAsset(asset);

        assertEquals("There should be 1 assetExtraction created when updating an exising asset without assetExtraction (due to some processing failure)", 1, assetExtractions.size());
    }

    @Test
    public void testAddOrUpdateAssetAndProcessIfNeededShouldStartExtractionIfAssetExistButAssetExtractionIsOutdated() throws Exception {

        String content = "content";
        String path = "path/to/asset.csv";

        Repository repository = repositoryService.createRepository(testIdWatcher.getEntityName("repository"));

        Asset asset = assetService.createAssetWithContent(repository.getId(), path, content);

        Asset fakePreviousAssetVersion = new Asset();
        fakePreviousAssetVersion.setId(asset.getId());
        AssetExtraction createAssetExtraction = assetExtractionService.createAssetExtraction(fakePreviousAssetVersion, null);
        assetExtractionService.markAssetExtractionAsLastSuccessful(asset, createAssetExtraction);

        addAssetAndWaitUntilDoneProcessing(repository.getId(), content, path, null);

        List<AssetExtraction> assetExtractions = assetExtractionRepository.findByAsset(asset);

        assertEquals("There should be 1 assetExtraction created when updating an exising asset with outdated assetExtraction (due to some processing failure)", 2, assetExtractions.size());
    }

    @Test
    public void testAddOrUpdateAssetAndProcessIfNeededShouldNotStartExtractionIfAssetAlreadyExists() throws Exception {

        String content = "content";
        String path = "path/to/asset.csv";

        Repository repository = repositoryService.createRepository(testIdWatcher.getEntityName("repository"));

        Asset asset = addAssetAndWaitUntilDoneProcessing(repository.getId(), content, path, null);

        addAssetAndWaitUntilDoneProcessing(repository.getId(), content, path, null);

        List<AssetExtraction> assetExtractions = assetExtractionRepository.findByAsset(asset);

        assertEquals("There should be no assetExtraction created when adding an already existing asset", 1, assetExtractions.size());
    }

    @Test
    public void testAddOrUpdateAssetAndProcessIfNeededShouldStartExtractionIfNewAsset() throws Exception {

        String content = "newContent";
        String path = "path/to/new/asset.csv";

        Repository repository = repositoryService.createRepository(testIdWatcher.getEntityName("repository"));

        Asset asset = addAssetAndWaitUntilDoneProcessing(repository.getId(), content, path, null);

        List<AssetExtraction> assetExtractions = assetExtractionRepository.findByAsset(asset);

        assertEquals("There should be one assetExtraction created when adding a new asset", 1, assetExtractions.size());
    }

    @Test
    public void testAddOrUpdateAssetAndProcessIfNeededShouldUpdateAssetContentAndStartExtractionIfContentOfExistingAssetChanged() throws Exception {

        String content = "content";
        String newContent = "newContent";
        String path = "path/to/existing/asset.csv";

        Repository repository = repositoryService.createRepository(testIdWatcher.getEntityName("repository"));

        assetService.createAssetWithContent(repository.getId(), path, content);

        Asset asset = addAssetAndWaitUntilDoneProcessing(repository.getId(), newContent, path, null);

        asset = assetRepository.findById(asset.getId()).orElse(null);

        assertEquals("Content of existing asset should be updated if changed", newContent, asset.getLastSuccessfulAssetExtraction().getAssetContent().getContent());

        List<AssetExtraction> assetExtractions = assetExtractionRepository.findByAsset(asset);

        assertEquals("There should be one assetExtraction created when the adding an asset with changed content", 1, assetExtractions.size());
    }

    private Asset addAssetAndWaitUntilDoneProcessing(Long repositoryId, String assetContent, String assetPath, String branchName) throws Exception {

        PollableFuture<Asset> assetResult = assetService.addOrUpdateAssetAndProcessIfNeeded(repositoryId, assetContent, assetPath,
                branchName, null, null, null);

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

        Asset asset = addAssetAndWaitUntilDoneProcessing(repository.getId(), content, path, null);
        List<AssetExtraction> assetExtractions = assetExtractionRepository.findByAsset(asset);
        assertEquals("There should be one assetExtraction created when the adding an asset with initial content", 1, assetExtractions.size());
        Long assetId = asset.getId();

        assetService.deleteAsset(asset);

        asset = assetRepository.findById(assetId).orElse(null);
        assertTrue("The asset should have been deleted", asset.getDeleted());

        asset = addAssetAndWaitUntilDoneProcessing(repository.getId(), newContent, path, null);
        asset = assetRepository.findById(assetId).orElse(null);

        assertEquals("Content of existing asset should be updated if changed", newContent, asset.getLastSuccessfulAssetExtraction().getAssetContent().getContent());
        assertEquals("Asset id should have remained the same", assetId, asset.getId());

        assetExtractions = assetExtractionRepository.findByAsset(asset);
        assertEquals("There should be one more assetExtraction created when the adding an asset with changed content", 2, assetExtractions.size());
        assertFalse("The asset extraction process should un-delete the deleted asset", assetRepository.findById(assetId).get().getDeleted());
    }

    @Test
    public void testAddOrUpdateAssetAndProcessIfNeededReAddingAsset() throws Exception {

        String content = "content";
        String path = "path/to/asset.csv";

        Repository repository = repositoryService.createRepository(testIdWatcher.getEntityName("repository"));

        Asset asset = addAssetAndWaitUntilDoneProcessing(repository.getId(), content, path, null);

        List<AssetExtraction> assetExtractions = assetExtractionRepository.findByAsset(asset);
        assertEquals("There should be one assetExtraction created when the adding an asset with initial content", 1, assetExtractions.size());
        Long assetId = asset.getId();

        assetService.deleteAsset(asset);
        assertTrue("The asset should have been deleted", assetRepository.findById(assetId).get().getDeleted());

        addAssetAndWaitUntilDoneProcessing(repository.getId(), content, path, null);

        assetExtractions = assetExtractionRepository.findByAsset(asset);
        assertEquals("When re-adding an asset (same content as previously deleted), it will get processed as normal", 2, assetExtractions.size());
        assertFalse("The asset extraction process should un-delete the deleted asset", assetRepository.findById(assetId).orElse(null).getDeleted());
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
        Asset asset1 = assetService.createAssetWithContent(repository.getId(), path1, content1);
        addAssetAndWaitUntilDoneProcessing(repository.getId(), content1, path1, null);
        Asset asset2 = assetService.createAssetWithContent(repository.getId(), path2, content2);
        addAssetAndWaitUntilDoneProcessing(repository.getId(), content2, path2, null);
        Asset asset3 = assetService.createAssetWithContent(repository.getId(), path3, content3);
        addAssetAndWaitUntilDoneProcessing(repository.getId(), content3, path3, null);

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

    @Test
    public void testFindAllAssets() throws Exception {
        String content1 = "content1";
        String content2 = "content2";
        String content3 = "content3";
        String path1 = "path/to/asset1.csv";
        String path2 = "path/to/asset2.csv";
        String path3 = "path/to/asset3.csv";

        Repository repository = repositoryService.createRepository(testIdWatcher.getEntityName("repository"));

        Asset asset1 = addAssetAndWaitUntilDoneProcessing(repository.getId(), content1, path1, null);
        Asset asset2 = addAssetAndWaitUntilDoneProcessing(repository.getId(), content2, path2, null);
        addAssetAndWaitUntilDoneProcessing(repository.getId(), content1, path1, "branch1");
        Asset asset3 = addAssetAndWaitUntilDoneProcessing(repository.getId(), content3, path3, "branch2");

        String pathOld = "path/to/old";
        String contentOld = "contentOld";
        Asset oldAssetNotProcessing = assetService.createAssetWithContent(repository.getId(), pathOld, contentOld);

        Branch branchNull = branchRepository.findByNameAndRepository(null, repository);
        Branch branch1 = branchRepository.findByNameAndRepository("branch1", repository);
        Branch branch2 = branchRepository.findByNameAndRepository("branch2", repository);

        List<Asset> all = assetService.findAll(repository.getId(), null, null, null, 150000L);
        assertTrue("return nothing for inexiting branch", all.isEmpty());

        all = assetService.findAll(repository.getId(), null, null, null, null);
        assertEquals("4 asset total, no filtering", 4L, all.size());
        assertEquals(path1, all.get(0).getPath());
        assertEquals(path2, all.get(1).getPath());
        assertEquals(path3, all.get(2).getPath());
        assertEquals(pathOld, all.get(3).getPath());

        all = assetService.findAll(repository.getId(), null, null, null, branchNull.getId());
        assertEquals("2 asset in the branch with name: null", 2L, all.size());
        assertEquals(path1, all.get(0).getPath());
        assertEquals(path2, all.get(1).getPath());

        all = assetService.findAll(repository.getId(), null, null, null, branch1.getId());
        assertEquals("1 asset in branch1", 1L, all.size());
        assertEquals(path1, all.get(0).getPath());

        all = assetService.findAll(repository.getId(), null, null, null, branch2.getId());
        assertEquals("1 asset in branch2", 1L, all.size());
        assertEquals(path3, all.get(0).getPath());

        all = assetService.findAll(repository.getId(), null, true, null, null);
        assertTrue("no deleted asset/branches", all.isEmpty());

        all = assetService.findAll(repository.getId(), null, false, null, null);
        assertEquals("all are not deleted", 4L, all.size());

        all = assetService.findAll(repository.getId(), null, false, null, branch2.getId());
        assertEquals("1 asset in branch2", 1L, all.size());
        assertEquals(path3, all.get(0).getPath());

        assetService.deleteAsset(oldAssetNotProcessing);

        all = assetService.findAll(repository.getId(), null, null, null, null);
        assertEquals("all should still be 4", 4L, all.size());

        all = assetService.findAll(repository.getId(), null, true, null, null);
        assertEquals("old asset should now be deleted", 1L, all.size());
        assertEquals(pathOld, all.get(0).getPath());

        all = assetService.findAll(repository.getId(), null, false, null, null);
        assertEquals("3 not deleted", 3L, all.size());
        assertEquals(path1, all.get(0).getPath());
        assertEquals(path2, all.get(1).getPath());
        assertEquals(path3, all.get(2).getPath());

        assetService.deleteAssetOfBranch(asset1.getId(), branchNull.getId());
        all = assetService.findAll(repository.getId(), null, false, null, branchNull.getId());
        assertEquals("1 not deleted in branch name: null", 1L, all.size());

        all = assetService.findAll(repository.getId(), null, false, null, null);
        assertEquals("3 not deleted", 3L, all.size());

        assetService.deleteAssetOfBranch(asset2.getId(), branchNull.getId());
        all = assetService.findAll(repository.getId(), null, false, null, branchNull.getId());
        assertEquals("all deleted in branch name: null", 0L, all.size());

        all = assetService.findAll(repository.getId(), null, false, null, null);
        assertEquals("2 not deleted", 2L, all.size());

        assetService.deleteAssetsOfBranch(Sets.newHashSet(asset2.getId(), asset3.getId()), branchNull.getId());
        all = assetService.findAll(repository.getId(), null, false, null, branchNull.getId());
        assertEquals("all deleted", 0L, all.size());

    }
}

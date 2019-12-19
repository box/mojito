package com.box.l10n.mojito.service.assetExtraction;

import com.box.l10n.mojito.entity.Asset;
import com.box.l10n.mojito.entity.AssetExtraction;
import com.box.l10n.mojito.entity.AssetTextUnit;
import com.box.l10n.mojito.entity.PollableTask;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.service.asset.AssetRepository;
import com.box.l10n.mojito.service.asset.AssetService;
import com.box.l10n.mojito.service.assetTextUnit.AssetTextUnitRepository;
import com.box.l10n.mojito.service.pollableTask.PollableFuture;
import com.box.l10n.mojito.service.pollableTask.PollableTaskRepository;
import com.box.l10n.mojito.service.pollableTask.PollableTaskService;
import com.box.l10n.mojito.service.repository.RepositoryService;
import com.box.l10n.mojito.test.TestIdWatcher;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static org.junit.Assert.*;

/**
 * @author aloison
 */
public class AssetExtractionCleanupServiceTest extends ServiceTestBase {

    @Autowired
    RepositoryService repositoryService;

    @Autowired
    AssetService assetService;

    @Autowired
    PollableTaskService pollableTaskService;

    @Autowired
    AssetRepository assetRepository;

    @Autowired
    AssetExtractionRepository assetExtractionRepository;

    @Autowired
    AssetExtractionCleanupService assetExtractionCleanupService;

    @Autowired
    AssetTextUnitRepository assetTextUnitRepository;

    @Autowired
    PollableTaskRepository pollableTaskRepository;

    @Autowired
    AssetTextUnitToTMTextUnitRepository assetTextUnitToTMTextUnitRepository;

    @Rule
    public TestIdWatcher testIdWatcher = new TestIdWatcher();

    @Test
    public void testCleanupOldAssetExtractionsWhenAllExtractionsHaveFinished() throws Exception {

        String assetPath = "path/to/asset.xliff";
        Repository repository = createRepoWithThreeAssetExtractions(assetPath);

        Asset asset = assetRepository.findByPathAndRepositoryId(assetPath, repository.getId());
        List<AssetExtraction> originalAssetExtractions = assetExtractionRepository.findByAsset(asset);
        AssetExtraction lastSuccessfulAssetExtraction = asset.getLastSuccessfulAssetExtraction();
        assertEquals("There should be 3 asset extractions", 3, originalAssetExtractions.size());

        assetExtractionCleanupService.cleanupOldAssetExtractions();

        List<AssetExtraction> assetExtractionsAfterCleanup = assetExtractionRepository.findByAsset(asset);
        assertEquals("There should be only 1 asset extraction remaining", 1, assetExtractionsAfterCleanup.size());

        AssetExtraction remainingAssetExtraction = assetExtractionsAfterCleanup.get(0);
        assertEquals("The remaining asset extraction should be the last successful one", lastSuccessfulAssetExtraction.getId(), remainingAssetExtraction.getId());


        List<Long> assetExtractionIdsAfterCleanup = new ArrayList<>();
        for (AssetExtraction assetExtraction : assetExtractionsAfterCleanup) {
            assetExtractionIdsAfterCleanup.add(assetExtraction.getId());
        }

        for (AssetExtraction assetExtraction : originalAssetExtractions) {
            if (!assetExtractionIdsAfterCleanup.contains(assetExtraction.getId())) {
                List<AssetTextUnit> oldAssetTextUnits = assetTextUnitRepository.findByAssetExtraction(assetExtraction);
                assertTrue("There should be no AssetTextUnits belonging to old and fully processed assetExtractions remaining", oldAssetTextUnits.isEmpty());
            }
        }
    }

    @Test
    public void testCleanupOldAssetExtractionsMultipleBranch() throws Exception {

        String assetPath = "path/to/asset.xliff";
        Repository repository = createRepoWithThreeAssetExtractions(assetPath);
        Asset asset = assetRepository.findByPathAndRepositoryId(assetPath, repository.getId());

        List<AssetExtraction> originalAssetExtractions = assetExtractionRepository.findByAsset(asset);
        AssetExtraction lastSuccessfulAssetExtraction = asset.getLastSuccessfulAssetExtraction();
        assertEquals("There should be 3 asset extractions", 3, originalAssetExtractions.size());

        List<String> branches = Arrays.asList("branch1", "branch2");
        branches.forEach(branch -> createOrUpdateAssetAndWaitUntilProcessingEnds(repository, assetPath, 1, branch));
        branches.forEach(branch -> createOrUpdateAssetAndWaitUntilProcessingEnds(repository, assetPath, 2, branch));

        List<AssetExtraction> afterBranches = assetExtractionRepository.findByAsset(asset);
        assertEquals("There should be 7 asset extractions (3 orginal + 4 x per branch)", 11, afterBranches.size());

        assetExtractionCleanupService.cleanupOldAssetExtractions();

        List<AssetExtraction> assetExtractionsAfterCleanup = assetExtractionRepository.findByAsset(asset);
        assertEquals("There should be 1 assets extraction per branch (3) and one merged asset extraction", 4, assetExtractionsAfterCleanup.size());
    }

    @Test
    public void testCleanupOldAssetExtractionsWhenNotAllExtractionsHaveFinished() throws Exception {

        String assetPath = "path/to/asset.xliff";
        Repository repository = createRepoWithThreeAssetExtractions(assetPath);
        Asset asset = assetRepository.findByPathAndRepositoryId(assetPath, repository.getId());

        // force previous asset extraction's state
        AssetExtraction notFinishedAssetExtraction = setNotLastSuccessfulExtractionStateToNotFinished(asset);
        assertFalse("Asset extraction's state should now be NOT ALL FINISHED", notFinishedAssetExtraction.getPollableTask().isAllFinished());

        List<AssetExtraction> originalAssetExtractions = assetExtractionRepository.findByAsset(asset);
        AssetExtraction lastSuccessfulAssetExtraction = asset.getLastSuccessfulAssetExtraction();
        assertEquals("There should be 3 asset extractions", 3, originalAssetExtractions.size());

        assetExtractionCleanupService.cleanupOldAssetExtractions();

        List<AssetExtraction> assetExtractionsAfterCleanup = assetExtractionRepository.findByAsset(asset);
        assertEquals("There should be 2 assets extraction remaining", 2, assetExtractionsAfterCleanup.size());
        assertEquals("The 1st remaining asset extraction should be the non finished one", assetExtractionsAfterCleanup.get(0).getId(), notFinishedAssetExtraction.getId());
        assertEquals("The 2nd remaining asset extraction should be the last successful one", assetExtractionsAfterCleanup.get(1).getId(), lastSuccessfulAssetExtraction.getId());


        List<Long> assetExtractionIdsAfterCleanup = new ArrayList<>();
        for (AssetExtraction assetExtraction : assetExtractionsAfterCleanup) {
            assetExtractionIdsAfterCleanup.add(assetExtraction.getId());
        }

        for (AssetExtraction assetExtraction : originalAssetExtractions) {
            if (!assetExtractionIdsAfterCleanup.contains(assetExtraction.getId())) {
                List<AssetTextUnit> oldAssetTextUnits = assetTextUnitRepository.findByAssetExtraction(assetExtraction);
                assertTrue("There should be no AssetTextUnits belonging to old and fully processed assetExtractions remaining", oldAssetTextUnits.isEmpty());
            }
        }
    }


    private Repository createRepoWithThreeAssetExtractions(String assetPath) throws Exception {
        Repository repository = repositoryService.createRepository(testIdWatcher.getEntityName("repository"));

        for (int i = 1; i <= 3; i++) {
            createOrUpdateAssetAndWaitUntilProcessingEnds(repository, assetPath, i, null);
        }

        return repository;
    }

    private void createOrUpdateAssetAndWaitUntilProcessingEnds(Repository repository, String assetPath, int assetVersion, String branch) {
        try {
            String xliff = xliffDataFactory.generateSourceXliff(Arrays.asList(
                    xliffDataFactory.createTextUnit(1L, "2_factor_challenge_buttom", "Submit" + assetVersion, null)
            ));

            PollableFuture<Asset> assetPollableFuture = assetService.addOrUpdateAssetAndProcessIfNeeded(repository.getId(), xliff, assetPath, branch, null, null, null);
            pollableTaskService.waitForPollableTask(assetPollableFuture.getPollableTask().getId());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private AssetExtraction setNotLastSuccessfulExtractionStateToNotFinished(Asset asset) {

        AssetExtraction assetExtractionToChange = null;

        List<AssetExtraction> assetExtractions = assetExtractionRepository.findByAsset(asset);
        AssetExtraction lastSuccessfulAssetExtraction = asset.getLastSuccessfulAssetExtraction();

        // retrieving last non last successful asset extraction
        for (AssetExtraction assetExtraction : assetExtractions) {
            if (!Objects.equals(assetExtraction.getId(), lastSuccessfulAssetExtraction.getId())) {
                assetExtractionToChange = assetExtraction;
            }
        }

        assertNotNull(assetExtractionToChange);

        PollableTask pollableTask = assetExtractionToChange.getPollableTask();
        pollableTask.setFinishedDate(null);
        pollableTaskRepository.save(pollableTask);

        return assetExtractionToChange;
    }
}

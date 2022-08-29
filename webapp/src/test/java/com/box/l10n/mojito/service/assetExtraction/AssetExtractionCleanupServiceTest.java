package com.box.l10n.mojito.service.assetExtraction;

import static org.junit.Assert.assertEquals;

import com.box.l10n.mojito.entity.Asset;
import com.box.l10n.mojito.entity.AssetExtraction;
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
import java.util.Arrays;
import java.util.List;
import org.joda.time.DateTime;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Those tests don't make sense for the new implementation but test the behavior for rollout of the
 * new feature if using multi server as there could be cleanup going on.
 *
 * @author aloison
 */
public class AssetExtractionCleanupServiceTest extends ServiceTestBase {

  @Autowired RepositoryService repositoryService;

  @Autowired AssetService assetService;

  @Autowired PollableTaskService pollableTaskService;

  @Autowired AssetRepository assetRepository;

  @Autowired AssetExtractionRepository assetExtractionRepository;

  @Autowired AssetExtractionCleanupService assetExtractionCleanupService;

  @Autowired AssetTextUnitRepository assetTextUnitRepository;

  @Autowired PollableTaskRepository pollableTaskRepository;

  @Autowired AssetTextUnitToTMTextUnitRepository assetTextUnitToTMTextUnitRepository;

  @Autowired AssetExtractionService assetExtractionService;

  @Rule public TestIdWatcher testIdWatcher = new TestIdWatcher();

  @Test
  public void testCleanupOldAssetExtractionsWhenAllExtractionsHaveFinished() throws Exception {

    String assetPath = "path/to/asset.xliff";
    Repository repository = createRepoWithThreeAssetExtractions(assetPath);

    Asset asset = assetRepository.findByPathAndRepositoryId(assetPath, repository.getId());
    List<AssetExtraction> originalAssetExtractions = assetExtractionRepository.findByAsset(asset);
    AssetExtraction lastSuccessfulAssetExtraction = asset.getLastSuccessfulAssetExtraction();
    assertEquals("There should be 3 asset extractions", 3, originalAssetExtractions.size());

    assetExtractionCleanupService.cleanupOldAssetExtractions();

    List<AssetExtraction> assetExtractionsAfterCleanup =
        assetExtractionRepository.findByAsset(asset);
    assertEquals(
        "There should be only 2 asset extraction remaining",
        2,
        assetExtractionsAfterCleanup.size());
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
    branches.forEach(
        branch -> createOrUpdateAssetAndWaitUntilProcessingEnds(repository, assetPath, 1, branch));
    branches.forEach(
        branch -> createOrUpdateAssetAndWaitUntilProcessingEnds(repository, assetPath, 2, branch));

    List<AssetExtraction> afterBranches = assetExtractionRepository.findByAsset(asset);
    assertEquals(
        "There should be 5 asset extractions (3 orginal + 1 x per branch)",
        5,
        afterBranches.size());

    assetExtractionCleanupService.cleanupOldAssetExtractions();

    List<AssetExtraction> assetExtractionsAfterCleanup =
        assetExtractionRepository.findByAsset(asset);
    assertEquals(
        "There should be 1 assets extraction per branch (3) and one merged asset extraction",
        4,
        assetExtractionsAfterCleanup.size());
  }

  private Repository createRepoWithThreeAssetExtractions(String assetPath) throws Exception {
    Repository repository =
        repositoryService.createRepository(testIdWatcher.getEntityName("repository"));
    Asset asset = createOrUpdateAssetAndWaitUntilProcessingEnds(repository, assetPath, 0, null);
    PollableTask pollableTask = new PollableTask();
    pollableTask.setName("fortest");
    pollableTask.setFinishedDate(new DateTime());
    pollableTaskRepository.save(pollableTask);
    AssetExtraction createAssetExtraction =
        assetExtractionService.createAssetExtraction(asset, pollableTask);
    return repository;
  }

  private Asset createOrUpdateAssetAndWaitUntilProcessingEnds(
      Repository repository, String assetPath, int assetVersion, String branch) {
    try {
      String xliff =
          xliffDataFactory.generateSourceXliff(
              Arrays.asList(
                  xliffDataFactory.createTextUnit(
                      1L, "2_factor_challenge_buttom", "Submit" + assetVersion, null)));

      PollableFuture<Asset> assetPollableFuture =
          assetService.addOrUpdateAssetAndProcessIfNeeded(
              repository.getId(), assetPath, xliff, false, branch, null, null, null, null);
      pollableTaskService.waitForPollableTask(assetPollableFuture.getPollableTask().getId());
      return assetPollableFuture.get();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}

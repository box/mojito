package com.box.l10n.mojito.service.asset;

import com.box.l10n.mojito.entity.Asset;
import com.box.l10n.mojito.entity.AssetContent;
import com.box.l10n.mojito.entity.AssetExtraction;
import com.box.l10n.mojito.entity.Branch;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.TMTextUnit;
import com.box.l10n.mojito.okapi.asset.UnsupportedAssetFilterTypeException;
import com.box.l10n.mojito.service.assetExtraction.AssetExtractionRepository;
import com.box.l10n.mojito.service.assetExtraction.ServiceTestBase;
import com.box.l10n.mojito.service.assetcontent.AssetContentRepository;
import com.box.l10n.mojito.service.branch.BranchRepository;
import com.box.l10n.mojito.service.pollableTask.PollableFuture;
import com.box.l10n.mojito.service.pollableTask.PollableTaskException;
import com.box.l10n.mojito.service.pollableTask.PollableTaskService;
import com.box.l10n.mojito.service.repository.RepositoryNameAlreadyUsedException;
import com.box.l10n.mojito.service.repository.RepositoryService;
import com.box.l10n.mojito.service.tm.TMTextUnitRepository;
import com.box.l10n.mojito.test.TestIdWatcher;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author aloison, jyi
 */
public class AssetServiceConcurrentTest extends ServiceTestBase {

    /**
     * logger
     */
    static Logger logger = getLogger(AssetServiceConcurrentTest.class);

    @Autowired
    AssetService assetService;

    @Autowired
    BranchRepository branchRepository;

    @Autowired
    RepositoryService repositoryService;

    @Autowired
    AssetRepository assetRepository;

    @Autowired
    AssetContentRepository assetContentRepository;

    @Autowired
    AssetExtractionRepository assetExtractionRepository;

    @Autowired
    PollableTaskService pollableTaskService;

    @Autowired
    TMTextUnitRepository tmTextUnitRepository;

    @Rule
    public TestIdWatcher testIdWatcher = new TestIdWatcher();

    /**
     * Test for processing same or updated asset with single text unit in parallel.
     *
     * @throws ExecutionException
     * @throws InterruptedException
     */
    @Test
    public void testConcurrencyForAssetExtraction() throws ExecutionException, InterruptedException, RepositoryNameAlreadyUsedException, AssetUpdateException, UnsupportedAssetFilterTypeException {
        logger.debug("Testing for concurrency when processing same asset with same/updated contents in parallel");

        Repository repository = repositoryService.createRepository(testIdWatcher.getEntityName("repository"));
        String assetPath = "path/to/existing/asset.xliff";

        int numThreads = 30;
        List<PollableFuture<Asset>> assetResults = new ArrayList<>();
        List<String> assetContents = new ArrayList<>();

        for (int i = 1; i <= numThreads; i++) {
            String source = (i % 2 == 0) ? ("source" + i) : ("source" + (i - 1));
            String assetContent = xliffDataFactory.generateSourceXliff(Arrays.asList(
                    xliffDataFactory.createTextUnit(1L, "name", source, null)
            ));
            assetContents.add(assetContent);

            logger.debug("addOrUpdateAssetAndProcessIfNeeded: {}, source: {}", i, source);
            PollableFuture<Asset> assetResult = assetService.addOrUpdateAssetAndProcessIfNeeded(repository.getId(), assetPath, assetContent, false, null, null, null, null);
            assetResults.add(assetResult);
        }

        List<Exception> exceptions = new ArrayList<>();
        for (int i = 0; i < assetResults.size(); i++) {
            try {
                logger.debug("Get asset result: {} (i={})", assetResults.get(i).getPollableTask().getId(), i);
                PollableFuture<Asset> assetResult = assetResults.get(i);
                pollableTaskService.waitForPollableTask(assetResult.getPollableTask().getId(), getTimeoutForIndex(i, 5000));
                Asset asset = assetRepository.findById(assetResult.get().getId()).orElse(null);
            } catch (PollableTaskException | InterruptedException e) {
                exceptions.add(e);
            }
        }

        Branch branch = branchRepository.findByNameAndRepository(null, repository);

        Set<String> assetContentsFromDB = new HashSet();

        for (AssetContent assetContent : assetContentRepository.findByAssetRepositoryIdAndBranchName(repository.getId(), null)) {
            assetContentsFromDB.add(assetContent.getContent());
        }

        assertEquals(new HashSet<>(assetContents), assetContentsFromDB);

        if (!exceptions.isEmpty()) {
            exceptions.stream().forEach(e -> logger.error("No exception should have been thrown", e));
        }
        assertTrue("No exceptions should have been thrown", exceptions.isEmpty());

        Asset asset = assetRepository.findByPathAndRepositoryId(assetPath, repository.getId());
        assertNotNull("There should be one asset", asset);

        List<AssetExtraction> assetExtractions = assetExtractionRepository.findByAsset(asset);

        assertEquals("There should be 2 asset extractions", 2, assetExtractions.size());
    }

    /**
     * To use when waiting for a list of pollable with a global timeout. Wait for the first element the whole timeout
     * and then don't wait.
     *
     * Gives a long timeout for the idx = 0 else ~ no wait.
     */
    int getTimeoutForIndex(int idx, int timeout) {
        return idx == 0 ? timeout : 1;
    }

    /**
     * Test for adding 10 new assets simultaneously to a repository.
     *
     * @throws ExecutionException
     * @throws InterruptedException
     */
    @Test
    public void testConcurrentMultipleAssetExtractions() throws ExecutionException, InterruptedException, RepositoryNameAlreadyUsedException, AssetUpdateException, UnsupportedAssetFilterTypeException {
        logger.debug("Testing for concurrency when processing 10 different assets in parallel");

        Repository repository = repositoryService.createRepository(testIdWatcher.getEntityName("repository"));
        StringBuilder assetContent = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            assetContent.append("# Test [" + i + "]\n"
                    + "test." + i + " = Test " + i + "\n");
        }

        int numThreads = 10;
        List<PollableFuture<Asset>> assetResults = new ArrayList<>();

        for (int i = 0; i < numThreads; i++) {
            String assetPath = "test_" + i + "/en.properties";
            PollableFuture<Asset> assetResult = assetService.addOrUpdateAssetAndProcessIfNeeded(repository.getId(), assetPath, assetContent.toString(), false, null, null, null, null);
            assetResults.add(assetResult);
        }

        List<Exception> exceptions = new ArrayList<>();
        processAssets(assetContent, assetResults, exceptions);
        logger.debug("{} exceptions found", exceptions.size());
        assertTrue("No exceptions should have been thrown", exceptions.isEmpty());

        Set<Long> assetIds = assetRepository.findIdByRepositoryId(repository.getId());
        assertEquals("There should be " + numThreads + " asset ids", numThreads, assetIds.size());

        List<TMTextUnit> tmTextUnits = tmTextUnitRepository.findByTm_id(repository.getTm().getId());
        assertEquals("There should be " + 100 * numThreads + " tmTextUnits", 100 * numThreads, tmTextUnits.size());
    }

    public void processAssets(StringBuilder assetContent, List<PollableFuture<Asset>> assetResults, List<Exception> exceptions) throws ExecutionException {

        for (int i = 0; i < assetResults.size(); i++) {
            try {
                logger.debug("Get asset result: {}", assetResults.get(i).getPollableTask().getId());
                PollableFuture<Asset> assetResult = assetResults.get(i);
                pollableTaskService.waitForPollableTask(assetResult.getPollableTask().getId(), getTimeoutForIndex(i, 120000));
                Asset asset = assetRepository.findById(assetResult.get().getId()).orElse(null);

//                assertEquals(assetContent.toString(), asset.getLastSuccessfulAssetExtraction().getAssetContent().getContent());
            } catch (PollableTaskException | InterruptedException e) {
                logger.error(ExceptionUtils.getStackTrace(e));
                exceptions.add(e);
            }
        }
    }

    /**
     * Test for adding a new asset with 100 text units in parallel to a repository.
     *
     * @throws ExecutionException
     * @throws InterruptedException
     */
    @Test
    public void testConcurrentAssetExtractions() throws ExecutionException, InterruptedException, RepositoryNameAlreadyUsedException, AssetUpdateException, UnsupportedAssetFilterTypeException {
        logger.debug("Testing for concurrency when processing single new asset in parallel");

        Repository repository = repositoryService.createRepository(testIdWatcher.getEntityName("repository"));
        StringBuilder assetContent = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            assetContent.append("# Test [" + i + "]\n"
                    + "test." + i + " = Test " + i + "\n");
        }

        int numThreads = 5;
        List<PollableFuture<Asset>> assetResults = new ArrayList<>();

        for (int i = 0; i < numThreads; i++) {
            String assetPath = "en.properties";
            PollableFuture<Asset> assetResult = assetService.addOrUpdateAssetAndProcessIfNeeded(repository.getId(), assetPath, assetContent.toString(), false, null, null, null, null);
            assetResults.add(assetResult);
        }

        List<Exception> exceptions = new ArrayList<>();
        processAssets(assetContent, assetResults, exceptions);
        logger.debug("{} exceptions found", exceptions.size());
        assertTrue("No exceptions should have been thrown", exceptions.isEmpty());

        Set<Long> assetIds = assetRepository.findIdByRepositoryId(repository.getId());
        assertEquals("There should be 1 asset ids", 1, assetIds.size());

        List<TMTextUnit> tmTextUnits = tmTextUnitRepository.findByTm_id(repository.getTm().getId());
        assertEquals("There should be 100 tmTextUnits", 100, tmTextUnits.size());
    }
}

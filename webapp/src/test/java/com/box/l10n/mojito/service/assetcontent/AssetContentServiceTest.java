package com.box.l10n.mojito.service.assetcontent;

import com.box.l10n.mojito.entity.Asset;
import com.box.l10n.mojito.entity.AssetContent;
import com.box.l10n.mojito.entity.Branch;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.service.asset.AssetService;
import com.box.l10n.mojito.service.assetExtraction.ServiceTestBase;
import com.box.l10n.mojito.service.branch.BranchService;
import com.box.l10n.mojito.service.repository.RepositoryNameAlreadyUsedException;
import com.box.l10n.mojito.service.repository.RepositoryService;
import com.box.l10n.mojito.test.TestIdWatcher;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.slf4j.LoggerFactory.getLogger;

public class AssetContentServiceTest extends ServiceTestBase {

    /**
     * logger
     */
    static Logger logger = getLogger(AssetContentServiceTest.class);

    @Rule
    public TestIdWatcher testIdWatcher = new TestIdWatcher();

    @Autowired
    AssetContentService assetContentService;

    @Autowired
    AssetContentRepository assetContentRepository;

    @Autowired
    AssetService assetService;

    @Autowired
    RepositoryService repositoryService;

    @Autowired
    BranchService branchService;

    @Test
    public void createAssetContentAndFind() throws RepositoryNameAlreadyUsedException {
        Repository repository = repositoryService.createRepository(testIdWatcher.getEntityName("repository"));

        Asset fortest1 = assetService.createAsset(repository.getId(), "fortest1", false);

        assertTrue(assetContentRepository.findByAssetRepositoryIdAndBranchName(repository.getId(), null).isEmpty());

        AssetContent content1 = assetContentService.createAssetContent(fortest1, "fortest1-content1");
        List<AssetContent> assetContents = assetContentRepository.findByAssetRepositoryIdAndBranchName(repository.getId(), null);
        assertEquals(1L, assetContents.size());
        assertEquals("fortest1-content1", assetContents.get(0).getContent());

        AssetContent content2 = assetContentService.createAssetContent(fortest1, "fortest1-content2");
        assetContents = assetContentRepository.findByAssetRepositoryIdAndBranchName(repository.getId(), null);
        assertEquals(2L, assetContents.size());
        assertEquals("fortest1-content1", assetContents.get(0).getContent());
        assertEquals("fortest1-content2", assetContents.get(1).getContent());

        AssetContent content2re = assetContentService.createAssetContent(fortest1, "fortest1-content2");
        assetContents = assetContentRepository.findByAssetRepositoryIdAndBranchName(repository.getId(), null);
        assertEquals(3L, assetContents.size());
        assertEquals("fortest1-content1", assetContents.get(0).getContent());
        assertEquals("fortest1-content2", assetContents.get(1).getContent());
        assertEquals("fortest1-content2", assetContents.get(2).getContent());

        Asset fortest2 = assetService.createAsset(repository.getId(), "fortest2", false);
        AssetContent forTest2Content1 = assetContentService.createAssetContent(fortest1, "fortest2-content1");
        assetContents = assetContentRepository.findByAssetRepositoryIdAndBranchName(repository.getId(), null);
        assertEquals(4L, assetContents.size());
        assertEquals("fortest1-content1", assetContents.get(0).getContent());
        assertEquals("fortest1-content2", assetContents.get(1).getContent());
        assertEquals("fortest1-content2", assetContents.get(2).getContent());
        assertEquals("fortest2-content1", assetContents.get(3).getContent());

        Branch branch1 = branchService.getUndeletedOrCreateBranch(fortest1.getRepository(), "branch1", null);
        AssetContent forTest2Content1Branch1 = assetContentService.createAssetContent(fortest1, "fortest2-content1-branch1", false, branch1);
        assetContents = assetContentRepository.findByAssetRepositoryIdAndBranchName(repository.getId(), null);
        assertEquals(4L, assetContents.size());
        assetContents = assetContentRepository.findByAssetRepositoryIdAndBranchName(repository.getId(), "branch1");
        assertEquals(1L, assetContents.size());
        assertEquals("fortest2-content1-branch1", assetContents.get(0).getContent());

    }

}
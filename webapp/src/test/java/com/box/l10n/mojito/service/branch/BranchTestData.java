package com.box.l10n.mojito.service.branch;

import com.box.l10n.mojito.entity.Asset;
import com.box.l10n.mojito.entity.AssetContent;
import com.box.l10n.mojito.entity.Branch;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.RepositoryLocale;
import com.box.l10n.mojito.service.asset.AssetService;
import com.box.l10n.mojito.service.assetExtraction.AssetExtractionService;
import com.box.l10n.mojito.service.assetExtraction.extractor.UnsupportedAssetFilterTypeException;
import com.box.l10n.mojito.service.assetcontent.AssetContentService;
import com.box.l10n.mojito.service.repository.RepositoryLocaleCreationException;
import com.box.l10n.mojito.service.repository.RepositoryNameAlreadyUsedException;
import com.box.l10n.mojito.service.repository.RepositoryService;
import com.box.l10n.mojito.test.TestIdWatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import javax.annotation.PostConstruct;

@Configurable
public class BranchTestData {

    @Autowired
    RepositoryService repositoryService;

    @Autowired
    AssetService assetService;

    @Autowired
    BranchService branchService;

    @Autowired
    AssetContentService assetContentService;

    @Autowired
    AssetExtractionService assetExtractionService;

    TestIdWatcher testIdWatcher;

    private Repository repository;
    private RepositoryLocale repositoryLocaleFrFr;
    private RepositoryLocale repositoryLocaleJaJp;
    private Asset asset;
    private Branch master;
    private Branch branch1;
    private Branch branch2;

    public RepositoryLocale getRepositoryLocaleFrFr() {
        return repositoryLocaleFrFr;
    }

    public RepositoryLocale getRepositoryLocaleJaJp() {
        return repositoryLocaleJaJp;
    }

    public Asset getAsset() {
        return asset;
    }

    public Branch getMaster() {
        return master;
    }

    public Branch getBranch1() {
        return branch1;
    }

    public Branch getBranch2() {
        return branch2;
    }

    public Repository getRepository() {
        return repository;
    }

    public BranchTestData(TestIdWatcher testIdWatcher) {
        this.testIdWatcher = testIdWatcher;
    }

    @PostConstruct
    public BranchTestData init() throws RepositoryNameAlreadyUsedException, RepositoryLocaleCreationException, InterruptedException, java.util.concurrent.ExecutionException, UnsupportedAssetFilterTypeException {
        repository = repositoryService.createRepository(testIdWatcher.getEntityName("repository"));
        repositoryLocaleFrFr = repositoryService.addRepositoryLocale(repository, "fr-FR");
        repositoryLocaleJaJp = repositoryService.addRepositoryLocale(repository, "ja-JP");

        String assetPath = "path/to/file.properties";
        asset = assetService.createAsset(repository.getId(), assetPath, false);

        String masterContent = "# string1 description\n"
                + "string1=content1\n"
                + "string2=content2\n";

        master = branchService.createBranch(asset.getRepository(), "master", null);
        AssetContent assetContentMaster = assetContentService.createAssetContent(asset, masterContent, master);
        assetExtractionService.processAssetAsync(assetContentMaster.getId(), null, null).get();

        String branch1Content = "# string1 description\n"
                + "string1=content1\n"
                + "string3=content3\n";

        branch1 = branchService.createBranch(asset.getRepository(), "branch1", null);
        AssetContent assetContentBranch1 = assetContentService.createAssetContent(asset, branch1Content, branch1);
        assetExtractionService.processAssetAsync(assetContentBranch1.getId(), null, null).get();

        String branch2Content = "# string1 description\n"
                + "string1=content1\n"
                + "string2=content2\n"
                + "string4=content4\n"
                + "string5=content5\n";

        branch2 = branchService.createBranch(asset.getRepository(), "branch2", null);
        AssetContent assetContentBranch2 = assetContentService.createAssetContent(asset, branch2Content, branch2);
        assetExtractionService.processAssetAsync(assetContentBranch2.getId(), null, null).get();
        return this;
    }
}

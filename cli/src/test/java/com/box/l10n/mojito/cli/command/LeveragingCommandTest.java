package com.box.l10n.mojito.cli.command;

import com.box.l10n.mojito.cli.CLITestBase;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.TMTextUnitVariant;
import com.box.l10n.mojito.rest.client.AssetClient;
import com.box.l10n.mojito.rest.entity.Asset;
import com.box.l10n.mojito.service.repository.RepositoryService;
import com.box.l10n.mojito.service.tm.TMImportService;
import com.box.l10n.mojito.service.tm.TMTextUnitVariantRepository;
import java.util.Iterator;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import static org.junit.Assert.*;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author jaurambault
 */
public class LeveragingCommandTest extends CLITestBase {

    @Autowired
    TMImportService tmImport;

    @Autowired
    AssetClient assetClient;

    @Autowired
    RepositoryService repositoryService;

    @Autowired
    TMTextUnitVariantRepository tmTextUnitVariantRepository;

    @Test
    public void copyTM() throws Exception {

        Repository sourceRepository = createTestRepoUsingRepoService();
        Repository targetRepository = repositoryService.createRepository(testIdWatcher.getEntityName("target-repoisotry"));

        repositoryService.addRepositoryLocale(targetRepository, "fr-FR");
        repositoryService.addRepositoryLocale(targetRepository, "fr-CA", "fr-FR", false);
        repositoryService.addRepositoryLocale(targetRepository, "ja-JP");

        getL10nJCommander().run("push", "-r", sourceRepository.getName(),
                "-s", getInputResourcesTestDir("source").getAbsolutePath());

        getL10nJCommander().run("push", "-r", targetRepository.getName(),
                "-s", getInputResourcesTestDir("source").getAbsolutePath());

        Asset asset = assetClient.getAssetByPathAndRepositoryId("source-xliff.xliff", sourceRepository.getId());
        importTranslations(asset.getId(), "source-xliff_", "fr-FR");
        importTranslations(asset.getId(), "source-xliff_", "ja-JP");

        List<TMTextUnitVariant> initialTargetTranslations = tmTextUnitVariantRepository.findByTmTextUnitTmRepositoriesOrderByContent(targetRepository);
        
        assertEquals("There must be only english for now", 5, initialTargetTranslations.size());

        getL10nJCommander().run("leveraging-copy-tm", "-s", sourceRepository.getName(), "-t", targetRepository.getName());

        List<TMTextUnitVariant> sourceTranslations = tmTextUnitVariantRepository.findByTmTextUnitTmRepositoriesOrderByContent(sourceRepository);
        List<TMTextUnitVariant> targetTranslations = tmTextUnitVariantRepository.findByTmTextUnitTmRepositoriesOrderByContent(targetRepository);

        Iterator<TMTextUnitVariant> itSource = sourceTranslations.iterator();
        Iterator<TMTextUnitVariant> itTarget = targetTranslations.iterator();

        while (itTarget.hasNext()) {
            TMTextUnitVariant next = itTarget.next();
            Assert.assertEquals("translation in source and target must be the same", itSource.next().getContent(), next.getContent());
        }

        Assert.assertFalse(itTarget.hasNext());
        Assert.assertFalse(itSource.hasNext());
    }

}

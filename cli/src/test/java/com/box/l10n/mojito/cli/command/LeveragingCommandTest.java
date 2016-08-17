package com.box.l10n.mojito.cli.command;

import com.box.l10n.mojito.cli.CLITestBase;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.TMTextUnitVariant;
import com.box.l10n.mojito.rest.client.AssetClient;
import com.box.l10n.mojito.rest.entity.Asset;
import com.box.l10n.mojito.service.repository.RepositoryService;
import com.box.l10n.mojito.service.tm.TMImportService;
import com.box.l10n.mojito.service.tm.TMTextUnitVariantRepository;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import static org.junit.Assert.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author jaurambault
 */
public class LeveragingCommandTest extends CLITestBase {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(LeveragingCommandTest.class);

    @Autowired
    TMImportService tmImport;

    @Autowired
    AssetClient assetClient;

    @Autowired
    RepositoryService repositoryService;

    @Autowired
    TMTextUnitVariantRepository tmTextUnitVariantRepository;

    @Test
    public void copyTMModeMD5() throws Exception {

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

    @Test
    public void copyTMModeExact() throws Exception {

        Repository sourceRepository = repositoryService.createRepository(testIdWatcher.getEntityName("source-repoisotry"));
        Repository targetRepository = repositoryService.createRepository(testIdWatcher.getEntityName("target-repoisotry"));

        repositoryService.addRepositoryLocale(sourceRepository, "fr-FR");
        repositoryService.addRepositoryLocale(sourceRepository, "fr-CA", "fr-FR", false);
        repositoryService.addRepositoryLocale(sourceRepository, "ja-JP");

        repositoryService.addRepositoryLocale(targetRepository, "fr-FR");
        repositoryService.addRepositoryLocale(targetRepository, "fr-CA", "fr-FR", false);
        repositoryService.addRepositoryLocale(targetRepository, "ja-JP");

        getL10nJCommander().run("push", "-r", sourceRepository.getName(),
                "-s", getInputResourcesTestDir("source").getAbsolutePath());

        getL10nJCommander().run("push", "-r", targetRepository.getName(),
                "-s", getInputResourcesTestDir("source2").getAbsolutePath());

        Asset asset = assetClient.getAssetByPathAndRepositoryId("source-xliff.xliff", sourceRepository.getId());
        importTranslations(asset.getId(), "source-xliff_", "fr-FR");
        importTranslations(asset.getId(), "source-xliff_", "ja-JP");

        List<TMTextUnitVariant> initialTargetTranslations = tmTextUnitVariantRepository.findByTmTextUnitTmRepositoriesOrderByContent(targetRepository);

        assertEquals("There must be only english for now", 5, initialTargetTranslations.size());

        getL10nJCommander().run("leveraging-copy-tm", "-s", sourceRepository.getName(), "-t", targetRepository.getName(), "-m", "EXACT");

        List<TMTextUnitVariant> targetTranslations = tmTextUnitVariantRepository.findByTmTextUnitTmRepositoriesOrderByContent(targetRepository);
        
        List<String> expectedTargetTranslations = new ArrayList<>();
        
        expectedTargetTranslations.add("1 day"); // en
        expectedTargetTranslations.add("1 heure"); // fr
        expectedTargetTranslations.add("1 hour"); // en
        expectedTargetTranslations.add("1 jour"); // fr
        expectedTargetTranslations.add("1 mois"); //fr
        expectedTargetTranslations.add("1 month"); // en
        expectedTargetTranslations.add("100 character description:"); // en
        expectedTargetTranslations.add("100文字の説明："); // ja 
        expectedTargetTranslations.add("15 min"); // en
        expectedTargetTranslations.add("15 min"); // fr
        expectedTargetTranslations.add("15分"); // ja, we make sure that the translation with same ID is picked up in priority (we don't want: "15分 dupplicate" to be returned)
        expectedTargetTranslations.add("1か月"); // ja
        expectedTargetTranslations.add("1日"); // ja
        expectedTargetTranslations.add("1時間"); // ja
        expectedTargetTranslations.add("Description de 100 caractères :"); //fr
                
        Iterator<TMTextUnitVariant> itTargetTranslations = targetTranslations.iterator();
        Iterator<String> itExpectedTargetTranslations = expectedTargetTranslations.iterator();
        
        for (TMTextUnitVariant targetTranslation : targetTranslations) {
            logger.error("target translation: {}", targetTranslation.getContent());
        }
        
        while (itExpectedTargetTranslations.hasNext()) {
            Assert.assertEquals("translation in source and target must be the same", itExpectedTargetTranslations.next(), itTargetTranslations.next().getContent());
        }

        Assert.assertFalse(itExpectedTargetTranslations.hasNext());
        Assert.assertFalse(itTargetTranslations.hasNext());
    }

}

package com.box.l10n.mojito.cli.command;

import com.box.l10n.mojito.cli.CLITestBase;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.RepositoryStatistic;
import com.box.l10n.mojito.rest.client.AssetClient;
import com.box.l10n.mojito.rest.client.DropClient;
import com.box.l10n.mojito.rest.entity.Asset;
import com.box.l10n.mojito.rest.entity.Drop;
import com.box.l10n.mojito.rest.entity.Page;
import com.box.l10n.mojito.service.repository.RepositoryRepository;
import com.box.l10n.mojito.service.tm.TMImportService;
import static org.junit.Assert.*;
import com.google.common.collect.Sets;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author jaurambault
 */
public class DropExportCommandTest extends CLITestBase {

    @Autowired
    TMImportService tmImport;

    @Autowired
    AssetClient assetClient;

    @Autowired
    DropClient dropClient;

    @Autowired
    RepositoryRepository repositoryRepository;

    @Test
    public void export() throws Exception {

        Repository repository = createTestRepoUsingRepoService();

        getL10nJCommander().run("push", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source").getAbsolutePath());

        waitForRepositoryToHaveStringsForTranslations(repository.getId());

        Page<Drop> findAllBefore = dropClient.getDrops(repository.getId(), null, null, null);

        getL10nJCommander().run("drop-export", "-r", repository.getName());

        Page<Drop> findAllAfter = dropClient.getDrops(repository.getId(), null, null, null);

        assertEquals("A Drop must have been added", findAllBefore.getTotalElements() + 1, findAllAfter.getTotalElements());
    }

    @Test
    public void exportFullyTranslated() throws Exception {

        Repository repository = createTestRepoUsingRepoService();

        getL10nJCommander().run("push", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source").getAbsolutePath());

        Asset asset = assetClient.getAssetByPathAndRepositoryId("source-xliff.xliff", repository.getId());
        importTranslations(asset.getId(), "source-xliff_", "fr-FR");
        importTranslations(asset.getId(), "source-xliff_", "ja-JP");

        waitForCondition("Must have text units that are fully translated", () -> {
            RepositoryStatistic repositoryStatistic = repositoryRepository.findOne(repository.getId()).getRepositoryStatistic();
            return repositoryStatistic.getUsedTextUnitCount() > 0
                    && repositoryStatistic.getRepositoryLocaleStatistics().stream()
                    .filter(rls -> Sets.newHashSet("fr-FR", "ja-JP").contains(rls.getLocale().getBcp47Tag()))
                    .allMatch(rls -> rls.getForTranslationCount() == 0);
        });

        Page<Drop> findAllBefore = dropClient.getDrops(repository.getId(), null, null, null);

        getL10nJCommander().run("drop-export", "-r", repository.getName());

        Page<Drop> findAllAfter = dropClient.getDrops(repository.getId(), null, null, null);

        assertEquals("A Drop should not have been added", findAllBefore.getTotalElements(), findAllAfter.getTotalElements());
    }

    @Test
    public void exportSelectFullyTranslatedLocales() throws Exception {

        Repository repository = createTestRepoUsingRepoService();

        getL10nJCommander().run("push", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source").getAbsolutePath());

        Asset asset = assetClient.getAssetByPathAndRepositoryId("source-xliff.xliff", repository.getId());
        importTranslations(asset.getId(), "source-xliff_", "fr-FR");
        // do NOT import translations for ja-JP

        waitForCondition("Must have text units that are fully translated", () -> {
            RepositoryStatistic repositoryStatistic = repositoryRepository.findOne(repository.getId()).getRepositoryStatistic();
            return repositoryStatistic.getUsedTextUnitCount() > 0
                    && repositoryStatistic.getRepositoryLocaleStatistics().stream()
                    .filter(rls -> Sets.newHashSet("fr-FR").contains(rls.getLocale().getBcp47Tag()))
                    .allMatch(rls -> rls.getForTranslationCount() == 0);
        });

        Page<Drop> findAllBefore = dropClient.getDrops(repository.getId(), null, null, null);

        // export only fr-FR which is fully translated
        getL10nJCommander().run("drop-export", "-r", repository.getName(), "-l", "fr-FR");

        Page<Drop> findAllAfter = dropClient.getDrops(repository.getId(), null, null, null);

        // drop should not be created because it consists only of fully translated locales,
        // even though repository as a whole is not fully translated (ja-JP is not translated)
        assertEquals("A Drop should not have been added", findAllBefore.getTotalElements(), findAllAfter.getTotalElements());
    }

    @Test
    public void exportReviewWithInheritance() throws Exception {

        Repository repository = createTestRepoUsingRepoService();

        getL10nJCommander().run("push", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source").getAbsolutePath());

        Asset asset = assetClient.getAssetByPathAndRepositoryId("source-xliff.xliff", repository.getId());
        importTranslations(asset.getId(), "source-xliff_", "fr-FR");
        importTranslations(asset.getId(), "source-xliff_", "ja-JP");

        Asset asset2 = assetClient.getAssetByPathAndRepositoryId("source2-xliff.xliff", repository.getId());
        importTranslations(asset2.getId(), "source2-xliff_", "fr-FR");
        importTranslations(asset2.getId(), "source2-xliff_", "ja-JP");

        Page<Drop> findAllBefore = dropClient.getDrops(repository.getId(), null, null, null);

        getL10nJCommander().run(new String[]{"drop-export", "-r", repository.getName(), "-l", "fr-CA", "-t", "REVIEW", "--use-inheritance"});

        Page<Drop> findAllAfter = dropClient.getDrops(repository.getId(), null, null, null);

        assertEquals("A Drop must have been added", findAllBefore.getTotalElements() + 1, findAllAfter.getTotalElements());
    }
}

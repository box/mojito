package com.box.l10n.mojito.cli.command;

import com.box.l10n.mojito.cli.CLITestBase;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.rest.client.AssetClient;
import com.box.l10n.mojito.rest.client.DropClient;
import com.box.l10n.mojito.rest.entity.Asset;
import com.box.l10n.mojito.rest.entity.Drop;
import com.box.l10n.mojito.rest.entity.Page;
import com.box.l10n.mojito.service.tm.TMImportService;
import static org.junit.Assert.*;
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

    @Test
    public void export() throws Exception {

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

        getL10nJCommander().run("drop-export", "-r", repository.getName());

        Page<Drop> findAllAfter = dropClient.getDrops(repository.getId(), null, null, null);

        assertEquals("A Drop must have been added", findAllBefore.getTotalElements() + 1, findAllAfter.getTotalElements());
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

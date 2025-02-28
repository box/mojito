package com.box.l10n.mojito.cli.command;

import com.box.l10n.mojito.apiclient.AssetClient;
import com.box.l10n.mojito.apiclient.model.AssetAssetSummary;
import com.box.l10n.mojito.cli.CLITestBase;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.test.XliffUtils;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author jaurambault
 */
public class TMExportCommandTest extends CLITestBase {

  @Autowired AssetClient assetClient;

  @Test
  public void export() throws Exception {

    Repository repository = createTestRepoUsingRepoService();

    getL10nJCommander()
        .run(
            "push",
            "-r",
            repository.getName(),
            "-s",
            getInputResourcesTestDir("source").getAbsolutePath());

    AssetAssetSummary asset =
        assetClient.getAssetByPathAndRepositoryId("source-xliff.xliff", repository.getId());
    importTranslations(asset.getId(), "source-xliff_", "fr-FR");
    importTranslations(asset.getId(), "source-xliff_", "ja-JP");

    AssetAssetSummary asset2 =
        assetClient.getAssetByPathAndRepositoryId("source2-xliff.xliff", repository.getId());
    importTranslations(asset2.getId(), "source2-xliff_", "fr-FR");
    importTranslations(asset2.getId(), "source2-xliff_", "ja-JP");

    getL10nJCommander()
        .run(
            "tm-export",
            "-r",
            repository.getName(),
            "-t",
            targetTestDir.getAbsolutePath(),
            "--target-basename",
            "fortest");

    modifyFilesInTargetTestDirectory(XliffUtils.replaceCreatedDateFunction());
    checkExpectedGeneratedResources();
  }
}

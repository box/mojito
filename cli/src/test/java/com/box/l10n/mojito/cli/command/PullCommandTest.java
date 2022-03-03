package com.box.l10n.mojito.cli.command;

import com.box.l10n.mojito.cli.CLITestBase;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.TMTextUnitVariant;
import com.box.l10n.mojito.rest.client.AssetClient;
import com.box.l10n.mojito.rest.client.RepositoryClient;
import com.box.l10n.mojito.rest.entity.Asset;
import com.box.l10n.mojito.rest.entity.RepositoryStatistic;
import com.box.l10n.mojito.service.tm.TMTextUnitVariantRepository;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Batch rename when adding a test: find . -name "*properties" -exec rename "s/properties/json/" {} \;
 */
public class PullCommandTest extends CLITestBase {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(PullCommandTest.class);

    @Autowired
    RepositoryClient repositoryClient;

    @Autowired
    AssetClient assetClient;

    @Autowired
    TMTextUnitVariantRepository tmTextUnitVariantRepository;

    @Test
    public void pull() throws Exception {

        Repository repository = createTestRepoUsingRepoService();

        getL10nJCommander().run("push", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source").getAbsolutePath());

        Asset asset = assetClient.getAssetByPathAndRepositoryId("source-xliff.xliff", repository.getId());
        importTranslations(asset.getId(), "source-xliff_", "fr-FR");
        importTranslations(asset.getId(), "source-xliff_", "ja-JP");

        Asset asset2 = assetClient.getAssetByPathAndRepositoryId("source2-xliff.xliff", repository.getId());
        importTranslations(asset2.getId(), "source2-xliff_", "fr-FR");
        importTranslations(asset2.getId(), "source2-xliff_", "ja-JP");

        getL10nJCommander().run("pull", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source").getAbsolutePath(),
                "-t", getTargetTestDir("target").getAbsolutePath());

        getL10nJCommander().run("pull", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source_modified").getAbsolutePath(),
                "-t", getTargetTestDir("target_modified").getAbsolutePath());

        checkExpectedGeneratedResources();
    }

    @Test
    public void pullWithAsyncWS() throws Exception {

        Repository repository = createTestRepoUsingRepoService();

        getL10nJCommander().run("push", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source").getAbsolutePath());

        Asset asset = assetClient.getAssetByPathAndRepositoryId("source-xliff.xliff", repository.getId());
        importTranslations(asset.getId(), "source-xliff_", "fr-FR");
        importTranslations(asset.getId(), "source-xliff_", "ja-JP");

        Asset asset2 = assetClient.getAssetByPathAndRepositoryId("source2-xliff.xliff", repository.getId());
        importTranslations(asset2.getId(), "source2-xliff_", "fr-FR");
        importTranslations(asset2.getId(), "source2-xliff_", "ja-JP");

        getL10nJCommander().run("pull", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source").getAbsolutePath(),
                "-t", getTargetTestDir("target").getAbsolutePath(),
                "--async-ws");

        getL10nJCommander().run("pull", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source_modified").getAbsolutePath(),
                "-t", getTargetTestDir("target_modified").getAbsolutePath(),
                "--async-ws");

        checkExpectedGeneratedResources();
    }

    @Test
    public void pullWithDuplicatedTextUnits() throws Exception {

        Repository repository = createTestRepoUsingRepoService();

        getL10nJCommander().run("push", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source").getAbsolutePath(),
                "-ft", "PROPERTIES_NOBASENAME");

        Asset asset = assetClient.getAssetByPathAndRepositoryId("en.properties", repository.getId());
        importTranslations(asset.getId(), "source-xliff_", "fr-FR");
        importTranslations(asset.getId(), "source-xliff_", "ja-JP");

        getL10nJCommander().run("pull", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source").getAbsolutePath(),
                "-t", getTargetTestDir("target").getAbsolutePath(),
                "-ft", "PROPERTIES_NOBASENAME");

        checkExpectedGeneratedResources();
    }

    @Test
    public void pullPropertiesNoBasename() throws Exception {

        Repository repository = createTestRepoUsingRepoService();

        getL10nJCommander().run("push", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source").getAbsolutePath(),
                "-ft", "PROPERTIES_NOBASENAME");

        Asset asset = assetClient.getAssetByPathAndRepositoryId("en.properties", repository.getId());
        importTranslations(asset.getId(), "source-xliff_", "fr-FR");
        importTranslations(asset.getId(), "source-xliff_", "ja-JP");

        getL10nJCommander().run("pull", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source").getAbsolutePath(),
                "-t", getTargetTestDir("target").getAbsolutePath(),
                "-ft", "PROPERTIES_NOBASENAME");

        getL10nJCommander().run("pull", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source_modified").getAbsolutePath(),
                "-t", getTargetTestDir("target_modified").getAbsolutePath(),
                "-ft", "PROPERTIES_NOBASENAME");

        checkExpectedGeneratedResources();
    }

    @Test
    public void pullProperties() throws Exception {

        Repository repository = createTestRepoUsingRepoService();

        getL10nJCommander().run("push", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source").getAbsolutePath());

        Asset asset = assetClient.getAssetByPathAndRepositoryId("demo.properties", repository.getId());
        importTranslations(asset.getId(), "source-xliff_", "fr-FR");
        importTranslations(asset.getId(), "source-xliff_", "ja-JP");

        getL10nJCommander().run("pull", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source").getAbsolutePath(),
                "-t", getTargetTestDir("target").getAbsolutePath());

        getL10nJCommander().run("pull", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source_modified").getAbsolutePath(),
                "-t", getTargetTestDir("target_modified").getAbsolutePath());

        checkExpectedGeneratedResources();
    }

    @Test
    public void pullPropertiesJava() throws Exception {

        Repository repository = createTestRepoUsingRepoService();

        getL10nJCommander().run("push", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source").getAbsolutePath(),
                "-ft", "PROPERTIES_JAVA");

        Asset asset = assetClient.getAssetByPathAndRepositoryId("demo.properties", repository.getId());
        importTranslations(asset.getId(), "source-xliff_", "fr-FR");
        importTranslations(asset.getId(), "source-xliff_", "ja-JP");

        getL10nJCommander().run("pull", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source").getAbsolutePath(),
                "-t", getTargetTestDir("target").getAbsolutePath(),
                "-ft", "PROPERTIES_JAVA");

        getL10nJCommander().run("pull", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source_modified").getAbsolutePath(),
                "-t", getTargetTestDir("target_modified").getAbsolutePath(),
                "-ft", "PROPERTIES_JAVA");

        checkExpectedGeneratedResources();
    }

    @Test
    public void pullPropertiesNoBasenameEnUs() throws Exception {

        Repository repository = createTestRepoUsingRepoService();

        getL10nJCommander().run("push", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source").getAbsolutePath(),
                "-ft", "PROPERTIES_NOBASENAME",
                "-sl", "en-US");

        Asset asset = assetClient.getAssetByPathAndRepositoryId("en-US.properties", repository.getId());
        importTranslations(asset.getId(), "source-xliff_", "fr-FR");
        importTranslations(asset.getId(), "source-xliff_", "ja-JP");

        getL10nJCommander().run("pull", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source").getAbsolutePath(),
                "-t", getTargetTestDir("target").getAbsolutePath(),
                "-ft", "PROPERTIES_NOBASENAME",
                "-sl", "en-US");

        getL10nJCommander().run("pull", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source_modified").getAbsolutePath(),
                "-t", getTargetTestDir("target_modified").getAbsolutePath(),
                "-ft", "PROPERTIES_NOBASENAME",
                "-sl", "en-US");

        checkExpectedGeneratedResources();
    }

    @Test
    public void leveraging() throws Exception {

        Repository repository = createTestRepoUsingRepoService();

        getL10nJCommander().run("push", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source").getAbsolutePath(),
                "-ft", "PROPERTIES_NOBASENAME");

        Asset asset = assetClient.getAssetByPathAndRepositoryId("en.properties", repository.getId());
        importTranslations(asset.getId(), "source-xliff_", "fr-FR");
        importTranslations(asset.getId(), "source-xliff_", "ja-JP");

        getL10nJCommander().run("pull", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source").getAbsolutePath(),
                "-t", getTargetTestDir("target").getAbsolutePath(),
                "-ft", "PROPERTIES_NOBASENAME");

        logger.debug("Change text unit to test leveraging");

        getL10nJCommander().run("push", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source_modified").getAbsolutePath(),
                "-ft", "PROPERTIES_NOBASENAME");

        getL10nJCommander().run("pull", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source_modified").getAbsolutePath(),
                "-t", getTargetTestDir("target_modified").getAbsolutePath(),
                "-ft", "PROPERTIES_NOBASENAME");

        checkExpectedGeneratedResources();
    }

    @Test
    public void localeMapping() throws Exception {

        Repository repository = createTestRepoUsingRepoService();

        getL10nJCommander().run("push", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source").getAbsolutePath());

        Asset asset = assetClient.getAssetByPathAndRepositoryId("source-xliff.xliff", repository.getId());
        importTranslations(asset.getId(), "source-xliff_", "fr-FR");
        importTranslations(asset.getId(), "source-xliff_", "ja-JP");

        getL10nJCommander().run("pull", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source").getAbsolutePath(),
                "-t", getTargetTestDir("target").getAbsolutePath(),
                "-lm", "fr:fr-FR,fr-FR:fr-FR,ja:ja-JP");

        checkExpectedGeneratedResources();
    }

    @Test
    public void assetMapping() throws Exception {
        Repository repository = createTestRepoUsingRepoService();

        getL10nJCommander().run("push", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source").getAbsolutePath());

        Asset asset = assetClient.getAssetByPathAndRepositoryId("source-xliff.xliff", repository.getId());
        importTranslations(asset.getId(), "source-xliff_", "fr-FR");
        importTranslations(asset.getId(), "source-xliff_", "ja-JP");

        getL10nJCommander().run("pull", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("mapping").getAbsolutePath(),
                "-t", getTargetTestDir("target").getAbsolutePath(),
                "-am", "mapping-xliff.xliff:source-xliff.xliff");

        checkExpectedGeneratedResources();
    }

    @Test
    public void pullAndroidStrings() throws Exception {

        Repository repository = createTestRepoUsingRepoService();

        getL10nJCommander().run("push", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source").getAbsolutePath());

        Asset asset = assetClient.getAssetByPathAndRepositoryId("res/values/strings.xml", repository.getId());
        importTranslations(asset.getId(), "source-xliff_", "fr-FR");
        importTranslations(asset.getId(), "source-xliff_", "ja-JP");

        getL10nJCommander().run("pull", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source").getAbsolutePath(),
                "-t", getTargetTestDir("target").getAbsolutePath());

        getL10nJCommander().run("pull", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source_modified").getAbsolutePath(),
                "-t", getTargetTestDir("target_modified").getAbsolutePath());

        checkExpectedGeneratedResources();
    }

    @Test
    public void pullMacStrings() throws Exception {

        Repository repository = createTestRepoUsingRepoService();

        getL10nJCommander().run("push", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source").getAbsolutePath());

        Asset asset = assetClient.getAssetByPathAndRepositoryId("en.lproj/Localizable.strings", repository.getId());
        importTranslations(asset.getId(), "source-xliff_", "fr-FR");
        importTranslations(asset.getId(), "source-xliff_", "ja-JP");

        getL10nJCommander().run("pull", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source").getAbsolutePath(),
                "-t", getTargetTestDir("target").getAbsolutePath());

        getL10nJCommander().run("pull", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source_modified").getAbsolutePath(),
                "-t", getTargetTestDir("target_modified").getAbsolutePath());

        checkExpectedGeneratedResources();
    }

    @Test
    public void pullMacStringsdict() throws Exception {

        Repository repository = createTestRepoUsingRepoService();

        getL10nJCommander().run("push", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source").getAbsolutePath());

        Asset asset = assetClient.getAssetByPathAndRepositoryId("en.lproj/Localizable.stringsdict", repository.getId());
        importTranslations(asset.getId(), "source-xliff_", "fr-FR");
        importTranslations(asset.getId(), "source-xliff_", "ja-JP");

        getL10nJCommander().run("pull", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source").getAbsolutePath(),
                "-t", getTargetTestDir("target").getAbsolutePath());

        getL10nJCommander().run("pull", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source_modified").getAbsolutePath(),
                "-t", getTargetTestDir("target_modified").getAbsolutePath());

        checkExpectedGeneratedResources();
    }

    @Test
    public void pullResw() throws Exception {

        Repository repository = createTestRepoUsingRepoService();

        getL10nJCommander().run("push", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source").getAbsolutePath());

        Asset asset = assetClient.getAssetByPathAndRepositoryId("en/Resources.resw", repository.getId());
        importTranslations(asset.getId(), "source-xliff_", "fr-FR");
        importTranslations(asset.getId(), "source-xliff_", "ja-JP");

        getL10nJCommander().run("pull", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source").getAbsolutePath(),
                "-t", getTargetTestDir("target").getAbsolutePath(),
                "-lm", "fr:fr-FR,fr-CA:fr-CA,ja:ja-JP");

        getL10nJCommander().run("pull", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source_modified").getAbsolutePath(),
                "-t", getTargetTestDir("target_modified").getAbsolutePath(),
                "-lm", "fr:fr-FR,fr-CA:fr-CA,ja:ja-JP");

        checkExpectedGeneratedResources();
    }

    @Test
    public void pullResx() throws Exception {

        Repository repository = createTestRepoUsingRepoService();

        getL10nJCommander().run("push", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source").getAbsolutePath());

        Asset asset = assetClient.getAssetByPathAndRepositoryId("Test.resx", repository.getId());
        importTranslations(asset.getId(), "source-xliff_", "fr-FR");
        importTranslations(asset.getId(), "source-xliff_", "ja-JP");

        getL10nJCommander().run("pull", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source").getAbsolutePath(),
                "-t", getTargetTestDir("target").getAbsolutePath());

        getL10nJCommander().run("pull", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source_modified").getAbsolutePath(),
                "-t", getTargetTestDir("target_modified").getAbsolutePath());

        checkExpectedGeneratedResources();
    }

    @Test
    public void pullResxSourceRegex() throws Exception {

        Repository repository = createTestRepoUsingRepoService();

        getL10nJCommander().run("push", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source").getAbsolutePath(),
                "-sr", "Localization\\.resx|Test\\.resx");

        Asset asset = assetClient.getAssetByPathAndRepositoryId("Localization.resx", repository.getId());
        importTranslations(asset.getId(), "source-xliff_Localization_", "fr-FR");
        importTranslations(asset.getId(), "source-xliff_Localization_", "ja-JP");
        asset = assetClient.getAssetByPathAndRepositoryId("Test.resx", repository.getId());
        importTranslations(asset.getId(), "source-xliff_Test_", "fr-FR");
        importTranslations(asset.getId(), "source-xliff_Test_", "ja-JP");

        getL10nJCommander().run("pull", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source").getAbsolutePath(),
                "-t", getTargetTestDir("target").getAbsolutePath(),
                "-sr", "Localization\\.resx|Test\\.resx");

        getL10nJCommander().run("pull", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source_modified").getAbsolutePath(),
                "-t", getTargetTestDir("target_modified").getAbsolutePath(),
                "-sr", "Localization\\.resx|Test\\.resx");

        checkExpectedGeneratedResources();
    }

    @Test
    public void testLatestTMTextUnitVariant() throws Exception {
        Repository repository1 = createTestRepoUsingRepoService("repo1", false);
        Repository repository2 = createTestRepoUsingRepoService("repo2", false);

        getL10nJCommander().run("push", "-r", repository1.getName(),
                "-s", getInputResourcesTestDir("source").getAbsolutePath());
        getL10nJCommander().run("push", "-r", repository2.getName(),
                "-s", getInputResourcesTestDir("source").getAbsolutePath());

        Asset asset1 = assetClient.getAssetByPathAndRepositoryId("source-xliff.xliff", repository1.getId());
        importTranslations(asset1.getId(), "source-xliff_", "fr-FR");

        logger.debug("Test findLatestTMTextUnitVariant");
        TMTextUnitVariant latestTmTextUnitVariantOfRepository1 = tmTextUnitVariantRepository.findTopByTmTextUnitTmIdOrderByCreatedDateDesc(repository1.getTm().getId());
        assertNotNull("should have TMTextUnitVariant from imports above", latestTmTextUnitVariantOfRepository1);

        logger.debug("Test findLatestTMTextUnitVariant again after a translation is added to other TM");
        Asset asset2 = assetClient.getAssetByPathAndRepositoryId("source-xliff.xliff", repository2.getId());
        importTranslations(asset2.getId(), "source-xliff_", "fr-FR");
        TMTextUnitVariant tmTextUnitVariant = tmTextUnitVariantRepository.findTopByTmTextUnitTmIdOrderByCreatedDateDesc(repository1.getTm().getId());
        assertEquals("should have returned the same TMTextUnitVariant as above latestTmTextUnitVariantOfRepository1",
                latestTmTextUnitVariantOfRepository1.getId(), tmTextUnitVariant.getId());
    }

    @Test
    public void pullXcodeXliff() throws Exception {

        Repository repository = createTestRepoUsingRepoService();

        getL10nJCommander().run("push", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source").getAbsolutePath(),
                "-ft", "XCODE_XLIFF");

        Asset asset = assetClient.getAssetByPathAndRepositoryId("en.xliff", repository.getId());
        importTranslations(asset.getId(), "source-xliff_", "fr-FR");
        importTranslations(asset.getId(), "source-xliff_", "ja-JP");

        getL10nJCommander().run("pull", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source").getAbsolutePath(),
                "-t", getTargetTestDir("target").getAbsolutePath(),
                "-lm", "fr:fr-FR,fr-CA:fr-CA,ja:ja-JP",
                "-ft", "XCODE_XLIFF");

        getL10nJCommander().run("pull", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source_modified").getAbsolutePath(),
                "-t", getTargetTestDir("target_modified").getAbsolutePath(),
                "-lm", "fr:fr-FR,fr-CA:fr-CA,ja:ja-JP",
                "-ft", "XCODE_XLIFF");

        checkExpectedGeneratedResources();
    }

    @Test
    public void pullPo() throws Exception {

        Repository repository = createTestRepoUsingRepoService();

        getL10nJCommander().run("push", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source").getAbsolutePath());

        Asset asset = assetClient.getAssetByPathAndRepositoryId("LC_MESSAGES/messages.pot", repository.getId());
        importTranslations(asset.getId(), "source-xliff_", "fr-FR");
        importTranslations(asset.getId(), "source-xliff_", "ja-JP");

        getL10nJCommander().run("pull", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source").getAbsolutePath(),
                "-t", getTargetTestDir("target").getAbsolutePath(),
                "-lm", "fr:fr-FR,fr-CA:fr-CA,ja:ja-JP");

        getL10nJCommander().run("pull", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source_modified").getAbsolutePath(),
                "-t", getTargetTestDir("target_modified").getAbsolutePath(),
                "-lm", "fr:fr-FR,fr-CA:fr-CA,ja:ja-JP");

        checkExpectedGeneratedResources();
    }

    @Test
    public void removeUntranslated() throws Exception {

        Repository repository = createTestRepoUsingRepoService();

        getL10nJCommander().run("push", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source").getAbsolutePath());

        Asset asset = assetClient.getAssetByPathAndRepositoryId("LC_MESSAGES/messages.pot", repository.getId());
        importTranslations(asset.getId(), "source-xliff_", "fr-FR");
        importTranslations(asset.getId(), "source-xliff_", "ja-JP");

        getL10nJCommander().run("pull", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source").getAbsolutePath(),
                "-t", getTargetTestDir("target").getAbsolutePath(),
                "-lm", "fr:fr-FR,ja:ja-JP",
                "--inheritance-mode", "REMOVE_UNTRANSLATED");

        getL10nJCommander().run("pull", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source_modified").getAbsolutePath(),
                "-t", getTargetTestDir("target_modified").getAbsolutePath(),
                "-lm", "fr:fr-FR,ja:ja-JP",
                "--inheritance-mode", "REMOVE_UNTRANSLATED");

        checkExpectedGeneratedResources();
    }

    @Test
    public void onlyApproved() throws Exception {

        Repository repository = createTestRepoUsingRepoService();

        getL10nJCommander().run("push", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source").getAbsolutePath());

        Asset asset = assetClient.getAssetByPathAndRepositoryId("test.xliff", repository.getId());
        importTranslations(asset.getId(), "source-xliff_", "fr-FR");
        importTranslations(asset.getId(), "source-xliff_", "ja-JP");

        updateTranslationsStatus(asset.getId(), TMTextUnitVariant.Status.REVIEW_NEEDED, "fr-FR");
        updateTranslationsStatus(asset.getId(), TMTextUnitVariant.Status.REVIEW_NEEDED, "ja-JP");

        getL10nJCommander().run("pull", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source").getAbsolutePath(),
                "-t", getTargetTestDir("target").getAbsolutePath(),
                "-lm", "fr:fr-FR,fr-CA:fr-CA,ja:ja-JP",
                "--inheritance-mode", "REMOVE_UNTRANSLATED",
                "--status", "ACCEPTED");

        updateTranslationsStatus(asset.getId(), TMTextUnitVariant.Status.APPROVED, "fr-FR");
        updateTranslationsStatus(asset.getId(), TMTextUnitVariant.Status.APPROVED, "ja-JP");

        getL10nJCommander().run("pull", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source_modified").getAbsolutePath(),
                "-t", getTargetTestDir("target_modified").getAbsolutePath(),
                "-lm", "fr:fr-FR,fr-CA:fr-CA,ja:ja-JP",
                "--inheritance-mode", "REMOVE_UNTRANSLATED",
                "--status", "ACCEPTED");

        checkExpectedGeneratedResources();
    }

    @Test
    public void pullXtb() throws Exception {
        Repository repository = createTestRepoUsingRepoService();

        getL10nJCommander().run("push", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source").getAbsolutePath(),
                "-sl", "en-US");

        Asset asset = assetClient.getAssetByPathAndRepositoryId("Resources-en-US.xtb", repository.getId());
        importTranslations(asset.getId(), "source-xliff_", "fr-FR");
        importTranslations(asset.getId(), "source-xliff_", "ja-JP");

        getL10nJCommander().run("pull", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source").getAbsolutePath(),
                "-t", getTargetTestDir("target").getAbsolutePath(),
                "-sl", "en-US");

        getL10nJCommander().run("pull", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source_modified").getAbsolutePath(),
                "-t", getTargetTestDir("target_modified").getAbsolutePath(),
                "-sl", "en-US");

        checkExpectedGeneratedResources();
    }

    @Test
    public void pullCsv() throws Exception {
        Repository repository = createTestRepoUsingRepoService();

        getL10nJCommander().run("push", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source").getAbsolutePath());

        Asset asset = assetClient.getAssetByPathAndRepositoryId("demo.csv", repository.getId());

        importTranslations(asset.getId(), "source-xliff_", "fr-FR");
        importTranslations(asset.getId(), "source-xliff_", "ja-JP");

        getL10nJCommander().run("pull", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source").getAbsolutePath(),
                "-t", getTargetTestDir("target").getAbsolutePath());

        getL10nJCommander().run("pull", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source_modified").getAbsolutePath(),
                "-t", getTargetTestDir("target_modified").getAbsolutePath());

        checkExpectedGeneratedResources();
    }

    @Test
    public void pullJS() throws Exception {
        Repository repository = createTestRepoUsingRepoService();

        getL10nJCommander().run("push", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source").getAbsolutePath());

        Asset asset = assetClient.getAssetByPathAndRepositoryId("en.js", repository.getId());

        importTranslations(asset.getId(), "source-xliff_", "fr-FR");
        importTranslations(asset.getId(), "source-xliff_", "ja-JP");

        getL10nJCommander().run("pull", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source").getAbsolutePath(),
                "-t", getTargetTestDir("target").getAbsolutePath(),
                "-lm", "fr:fr-FR,ja:ja-JP");

        getL10nJCommander().run("pull", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source_modified").getAbsolutePath(),
                "-t", getTargetTestDir("target_modified").getAbsolutePath(),
                "-lm", "fr:fr-FR,ja:ja-JP");

        checkExpectedGeneratedResources();
    }

    @Test
    public void pullTS() throws Exception {
        Repository repository = createTestRepoUsingRepoService();

        getL10nJCommander().run("push", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source").getAbsolutePath());

        Asset asset = assetClient.getAssetByPathAndRepositoryId("en.ts", repository.getId());

        importTranslations(asset.getId(), "source-xliff_", "fr-FR");
        importTranslations(asset.getId(), "source-xliff_", "ja-JP");

        getL10nJCommander().run("pull", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source").getAbsolutePath(),
                "-t", getTargetTestDir("target").getAbsolutePath(),
                "-lm", "fr:fr-FR,ja:ja-JP");

        getL10nJCommander().run("pull", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source_modified").getAbsolutePath(),
                "-t", getTargetTestDir("target_modified").getAbsolutePath(),
                "-lm", "fr:fr-FR,ja:ja-JP");

        checkExpectedGeneratedResources();
    }

    @Test
    public void pullJson() throws Exception {

        Repository repository = createTestRepoUsingRepoService();

        getL10nJCommander().run("push", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source").getAbsolutePath(),
                "-ft", "JSON");

        Asset asset = assetClient.getAssetByPathAndRepositoryId("demo.json", repository.getId());
        importTranslations(asset.getId(), "source-xliff_", "fr-FR");
        importTranslations(asset.getId(), "source-xliff_", "ja-JP");

        getL10nJCommander().run("pull", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source").getAbsolutePath(),
                "-t", getTargetTestDir("target").getAbsolutePath(),
                "-ft", "JSON");

        getL10nJCommander().run("pull", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source_modified").getAbsolutePath(),
                "-t", getTargetTestDir("target_modified").getAbsolutePath(),
                "-ft", "JSON");

        checkExpectedGeneratedResources();
    }

    @Test
    public void pullJsonNobasename() throws Exception {

        Repository repository = createTestRepoUsingRepoService();

        getL10nJCommander().run("push", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source").getAbsolutePath(),
                "-ft", "JSON_NOBASENAME");

        Asset asset = assetClient.getAssetByPathAndRepositoryId("en.json", repository.getId());
        importTranslations(asset.getId(), "source-xliff_", "fr-FR");
        importTranslations(asset.getId(), "source-xliff_", "ja-JP");

        getL10nJCommander().run("pull", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source").getAbsolutePath(),
                "-t", getTargetTestDir("target").getAbsolutePath(),
                "-ft", "JSON_NOBASENAME");

        getL10nJCommander().run("pull", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source_modified").getAbsolutePath(),
                "-t", getTargetTestDir("target_modified").getAbsolutePath(),
                "-ft", "JSON_NOBASENAME");

        checkExpectedGeneratedResources();
    }

    @Test
    public void pullJsonWithNote() throws Exception {

        Repository repository = createTestRepoUsingRepoService();

        getL10nJCommander().run("push", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source").getAbsolutePath(),
                "-fo", "noteKeyPattern=note", "extractAllPairs=false", "exceptions=string",
                "-ft", "JSON");

        Asset asset = assetClient.getAssetByPathAndRepositoryId("demo.json", repository.getId());
        importTranslations(asset.getId(), "source-xliff_", "fr-FR");
        importTranslations(asset.getId(), "source-xliff_", "ja-JP");

        getL10nJCommander().run("pull", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source").getAbsolutePath(),
                "-t", getTargetTestDir("target").getAbsolutePath(),
                "-fo", "noteKeyPattern=note", "extractAllPairs=false", "exceptions=string",
                "-ft", "JSON");

        getL10nJCommander().run("pull", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source_modified").getAbsolutePath(),
                "-t", getTargetTestDir("target_modified").getAbsolutePath(),
                "-fo", "noteKeyPattern=note", "extractAllPairs=false", "exceptions=string",
                "-ft", "JSON");

        checkExpectedGeneratedResources();
    }

    @Test
    public void pullJsonDefaultFormatJs() throws Exception {

        Repository repository = createTestRepoUsingRepoService();

        getL10nJCommander().run("push", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source").getAbsolutePath(),
                "-fo", "noteKeyPattern=description", "extractAllPairs=false", "exceptions=defaultMessage",
                "-ft", "JSON_NOBASENAME");

        Asset asset = assetClient.getAssetByPathAndRepositoryId("en.json", repository.getId());
        importTranslations(asset.getId(), "source-xliff_", "fr-FR");
        importTranslations(asset.getId(), "source-xliff_", "ja-JP");

        getL10nJCommander().run("pull", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source").getAbsolutePath(),
                "-t", getTargetTestDir("target").getAbsolutePath(),
                "-fo", "noteKeyPattern=description", "extractAllPairs=false", "exceptions=defaultMessage",
                "-ft", "JSON_NOBASENAME");

        getL10nJCommander().run("pull", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source_modified").getAbsolutePath(),
                "-t", getTargetTestDir("target_modified").getAbsolutePath(),
                "-fo", "noteKeyPattern=description", "extractAllPairs=false", "exceptions=defaultMessage",
                "-ft", "JSON_NOBASENAME");

        checkExpectedGeneratedResources();
    }

    @Test
    public void pullJsonDefaultFormatJsRemoveUntranslated() throws Exception {
        Repository repository = createTestRepoUsingRepoService();

        getL10nJCommander().run("push", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source").getAbsolutePath(),
                "-fo", "noteKeyPattern=description", "extractAllPairs=false", "exceptions=defaultMessage",
                "-ft", "JSON_NOBASENAME");

        Asset asset = assetClient.getAssetByPathAndRepositoryId("en.json", repository.getId());
        importTranslations(asset.getId(), "source-xliff_", "fr-FR");
        importTranslations(asset.getId(), "source-xliff_", "ja-JP");

        getL10nJCommander().run("pull", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source").getAbsolutePath(),
                "-t", getTargetTestDir("target").getAbsolutePath(),
                "-fo", "noteKeyPattern=description", "extractAllPairs=false", "exceptions=defaultMessage",
                "-ft", "JSON_NOBASENAME",
                "--inheritance-mode", "REMOVE_UNTRANSLATED");

        checkExpectedGeneratedResources();
    }

    @Test
    public void pullJsonFromChromeExtension() throws Exception {

        Repository repository = createTestRepoUsingRepoService();

        getL10nJCommander().run("push", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source").getAbsolutePath(),
                "-ft", "CHROME_EXT_JSON");

        Asset asset = assetClient.getAssetByPathAndRepositoryId("_locales/en/messages.json", repository.getId());
        importTranslations(asset.getId(), "source-xliff_", "fr-FR");
        importTranslations(asset.getId(), "source-xliff_", "ja-JP");

        getL10nJCommander().run("pull", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source").getAbsolutePath(),
                "-t", getTargetTestDir("target").getAbsolutePath(),
                "-ft", "CHROME_EXT_JSON");

        getL10nJCommander().run("pull", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source_modified").getAbsolutePath(),
                "-t", getTargetTestDir("target_modified").getAbsolutePath(),
                "-ft", "CHROME_EXT_JSON");

        checkExpectedGeneratedResources();
    }

    @Test
    public void pullJsonI18NextParser() throws Exception {

        Repository repository = createTestRepoUsingRepoService();

        getL10nJCommander().run("push", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source").getAbsolutePath(),
                "-ft", "I18NEXT_PARSER_JSON");

        Asset asset = assetClient.getAssetByPathAndRepositoryId("locales/en/demo.json", repository.getId());
        importTranslations(asset.getId(), "source-xliff_", "fr-FR");
        importTranslations(asset.getId(), "source-xliff_", "ja-JP");

        getL10nJCommander().run("pull", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source").getAbsolutePath(),
                "-t", getTargetTestDir("target").getAbsolutePath(),
                "-ft", "I18NEXT_PARSER_JSON");

        getL10nJCommander().run("pull", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source_modified").getAbsolutePath(),
                "-t", getTargetTestDir("target_modified").getAbsolutePath(),
                "-ft", "I18NEXT_PARSER_JSON");

        checkExpectedGeneratedResources();
    }

    @Test
    public void pullFullyTranslated() throws Exception {

        Repository repository = createTestRepoUsingRepoService();
        repositoryService.addRepositoryLocale(repository, "en-AU", null, false);

        getL10nJCommander().run("push", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source").getAbsolutePath());

        Asset asset = assetClient.getAssetByPathAndRepositoryId("demo.properties", repository.getId());
        importTranslations(asset.getId(), "source-xliff_", "fr-FR");
        importTranslations(asset.getId(), "source-xliff_", "ja-JP");

        getL10nJCommander().run("pull", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source").getAbsolutePath(),
                "-t", getTargetTestDir("target").getAbsolutePath());

        waitForCondition("repo stats must be updated - wait for jp to be fully translated and others to be untranslated", () -> {
            com.box.l10n.mojito.rest.entity.Repository repo = repositoryClient.getRepositoryById(repository.getId());
            RepositoryStatistic repositoryStatistic = repo.getRepositoryStatistic();

            boolean statsReady = repositoryStatistic.getRepositoryLocaleStatistics().stream()
                    .allMatch(repositoryLocaleStatistic -> {
                        if ("ja-JP".equals(repositoryLocaleStatistic.getLocale().getBcp47Tag())) {
                            return repositoryLocaleStatistic.getForTranslationCount() == 0;
                        } else {
                            return repositoryLocaleStatistic.getForTranslationCount() > 0;
                        }
                    });

            return !repositoryStatistic.getRepositoryLocaleStatistics().isEmpty() && statsReady;
        });

        getL10nJCommander().run("pull", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source").getAbsolutePath(),
                "-t", getTargetTestDir("target_fully_translated").getAbsolutePath(),
                "--fully-translated");

        checkExpectedGeneratedResources();
    }

    @Test
    public void pullFormattedUntranslated() throws Exception {
        Repository repository = createTestRepoUsingRepoService();

        getL10nJCommander().run("push", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source").getAbsolutePath());

        Asset asset = assetClient.getAssetByPathAndRepositoryId("demo.properties", repository.getId());
        importTranslations(asset.getId(), "source-xliff_", "fr-FR");
        importTranslations(asset.getId(), "source-xliff_", "ja-JP");

        getL10nJCommander().run("pull", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source").getAbsolutePath(),
                "-t", getTargetTestDir("target").getAbsolutePath(),
                "--untranslated-format", "\uD83D\uDE04{source}");

        checkExpectedGeneratedResources();
    }
}

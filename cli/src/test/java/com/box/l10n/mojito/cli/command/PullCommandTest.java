package com.box.l10n.mojito.cli.command;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.box.l10n.mojito.cli.CLITestBase;
import com.box.l10n.mojito.entity.Locale;
import com.box.l10n.mojito.entity.PullRun;
import com.box.l10n.mojito.entity.PushRun;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.TMTextUnit;
import com.box.l10n.mojito.entity.TMTextUnitVariant;
import com.box.l10n.mojito.rest.client.AssetClient;
import com.box.l10n.mojito.rest.client.RepositoryClient;
import com.box.l10n.mojito.rest.entity.Asset;
import com.box.l10n.mojito.rest.entity.RepositoryStatistic;
import com.box.l10n.mojito.service.commit.CommitService;
import com.box.l10n.mojito.service.delta.DeltaService;
import com.box.l10n.mojito.service.delta.DeltaType;
import com.box.l10n.mojito.service.delta.dtos.DeltaResponseDTO;
import com.box.l10n.mojito.service.locale.LocaleService;
import com.box.l10n.mojito.service.pullrun.PullRunRepository;
import com.box.l10n.mojito.service.pullrun.PullRunTextUnitVariantRepository;
import com.box.l10n.mojito.service.tm.TMService;
import com.box.l10n.mojito.service.tm.TMTextUnitCurrentVariantRepository;
import com.box.l10n.mojito.service.tm.TMTextUnitCurrentVariantService;
import com.box.l10n.mojito.service.tm.TMTextUnitRepository;
import com.box.l10n.mojito.service.tm.TMTextUnitVariantRepository;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import org.assertj.core.api.Assertions;
import org.joda.time.DateTime;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Batch rename when adding a test: find . -name "*properties" -exec rename "s/properties/json/" {}
 * \;
 */
public class PullCommandTest extends CLITestBase {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(PullCommandTest.class);

  @Autowired RepositoryClient repositoryClient;

  @Autowired AssetClient assetClient;

  @Autowired TMTextUnitVariantRepository tmTextUnitVariantRepository;

  @Autowired CommitService commitService;

  @Autowired DeltaService deltaService;

  @Autowired TMService tmService;

  @Autowired LocaleService localeService;

  @Autowired TMTextUnitCurrentVariantRepository tmTextUnitCurrentVariantRepository;

  @Autowired TMTextUnitCurrentVariantService tmTextUnitCurrentVariantService;

  @Autowired TMTextUnitRepository tmTextUnitRepository;

  @Autowired PullRunRepository pullRunRepository;

  @Autowired PullRunTextUnitVariantRepository pullRunTextUnitVariantRepository;

  @Test
  public void pull() throws Exception {

    Repository repository = createTestRepoUsingRepoService();

    getL10nJCommander()
        .run(
            "push",
            "-r",
            repository.getName(),
            "-s",
            getInputResourcesTestDir("source").getAbsolutePath());

    Asset asset =
        assetClient.getAssetByPathAndRepositoryId("source-xliff.xliff", repository.getId());
    importTranslations(asset.getId(), "source-xliff_", "fr-FR");
    importTranslations(asset.getId(), "source-xliff_", "ja-JP");

    Asset asset2 =
        assetClient.getAssetByPathAndRepositoryId("source2-xliff.xliff", repository.getId());
    importTranslations(asset2.getId(), "source2-xliff_", "fr-FR");
    importTranslations(asset2.getId(), "source2-xliff_", "ja-JP");

    getL10nJCommander()
        .run(
            "pull",
            "-r",
            repository.getName(),
            "-s",
            getInputResourcesTestDir("source").getAbsolutePath(),
            "-t",
            getTargetTestDir("target").getAbsolutePath());

    getL10nJCommander()
        .run(
            "pull",
            "-r",
            repository.getName(),
            "-s",
            getInputResourcesTestDir("source_modified").getAbsolutePath(),
            "-t",
            getTargetTestDir("target_modified").getAbsolutePath());

    checkExpectedGeneratedResources();
  }

  @Test
  public void pullWithAsyncWS() throws Exception {

    Repository repository = createTestRepoUsingRepoService();

    getL10nJCommander()
        .run(
            "push",
            "-r",
            repository.getName(),
            "-s",
            getInputResourcesTestDir("source").getAbsolutePath());

    Asset asset =
        assetClient.getAssetByPathAndRepositoryId("source-xliff.xliff", repository.getId());
    importTranslations(asset.getId(), "source-xliff_", "fr-FR");
    importTranslations(asset.getId(), "source-xliff_", "ja-JP");

    Asset asset2 =
        assetClient.getAssetByPathAndRepositoryId("source2-xliff.xliff", repository.getId());
    importTranslations(asset2.getId(), "source2-xliff_", "fr-FR");
    importTranslations(asset2.getId(), "source2-xliff_", "ja-JP");

    getL10nJCommander()
        .run(
            "pull",
            "-r",
            repository.getName(),
            "-s",
            getInputResourcesTestDir("source").getAbsolutePath(),
            "-t",
            getTargetTestDir("target").getAbsolutePath(),
            "--async-ws");

    getL10nJCommander()
        .run(
            "pull",
            "-r",
            repository.getName(),
            "-s",
            getInputResourcesTestDir("source_modified").getAbsolutePath(),
            "-t",
            getTargetTestDir("target_modified").getAbsolutePath(),
            "--async-ws");

    checkExpectedGeneratedResources();
  }

  @Test
  public void pullWithDuplicatedTextUnits() throws Exception {

    Repository repository = createTestRepoUsingRepoService();

    getL10nJCommander()
        .run(
            "push",
            "-r",
            repository.getName(),
            "-s",
            getInputResourcesTestDir("source").getAbsolutePath(),
            "-ft",
            "PROPERTIES_NOBASENAME");

    Asset asset = assetClient.getAssetByPathAndRepositoryId("en.properties", repository.getId());
    importTranslations(asset.getId(), "source-xliff_", "fr-FR");
    importTranslations(asset.getId(), "source-xliff_", "ja-JP");

    getL10nJCommander()
        .run(
            "pull",
            "-r",
            repository.getName(),
            "-s",
            getInputResourcesTestDir("source").getAbsolutePath(),
            "-t",
            getTargetTestDir("target").getAbsolutePath(),
            "-ft",
            "PROPERTIES_NOBASENAME");

    checkExpectedGeneratedResources();
  }

  @Test
  public void pullPropertiesNoBasename() throws Exception {

    Repository repository = createTestRepoUsingRepoService();

    getL10nJCommander()
        .run(
            "push",
            "-r",
            repository.getName(),
            "-s",
            getInputResourcesTestDir("source").getAbsolutePath(),
            "-ft",
            "PROPERTIES_NOBASENAME");

    Asset asset = assetClient.getAssetByPathAndRepositoryId("en.properties", repository.getId());
    importTranslations(asset.getId(), "source-xliff_", "fr-FR");
    importTranslations(asset.getId(), "source-xliff_", "ja-JP");

    getL10nJCommander()
        .run(
            "pull",
            "-r",
            repository.getName(),
            "-s",
            getInputResourcesTestDir("source").getAbsolutePath(),
            "-t",
            getTargetTestDir("target").getAbsolutePath(),
            "-ft",
            "PROPERTIES_NOBASENAME");

    getL10nJCommander()
        .run(
            "pull",
            "-r",
            repository.getName(),
            "-s",
            getInputResourcesTestDir("source_modified").getAbsolutePath(),
            "-t",
            getTargetTestDir("target_modified").getAbsolutePath(),
            "-ft",
            "PROPERTIES_NOBASENAME");

    checkExpectedGeneratedResources();
  }

  @Test
  public void pullProperties() throws Exception {

    Repository repository = createTestRepoUsingRepoService();

    getL10nJCommander()
        .run(
            "push",
            "-r",
            repository.getName(),
            "-s",
            getInputResourcesTestDir("source").getAbsolutePath());

    Asset asset = assetClient.getAssetByPathAndRepositoryId("demo.properties", repository.getId());
    importTranslations(asset.getId(), "source-xliff_", "fr-FR");
    importTranslations(asset.getId(), "source-xliff_", "ja-JP");

    getL10nJCommander()
        .run(
            "pull",
            "-r",
            repository.getName(),
            "-s",
            getInputResourcesTestDir("source").getAbsolutePath(),
            "-t",
            getTargetTestDir("target").getAbsolutePath());

    getL10nJCommander()
        .run(
            "pull",
            "-r",
            repository.getName(),
            "-s",
            getInputResourcesTestDir("source_modified").getAbsolutePath(),
            "-t",
            getTargetTestDir("target_modified").getAbsolutePath());

    checkExpectedGeneratedResources();
  }

  @Test
  public void pullPropertiesJava() throws Exception {

    Repository repository = createTestRepoUsingRepoService();

    getL10nJCommander()
        .run(
            "push",
            "-r",
            repository.getName(),
            "-s",
            getInputResourcesTestDir("source").getAbsolutePath(),
            "-ft",
            "PROPERTIES_JAVA");

    Asset asset = assetClient.getAssetByPathAndRepositoryId("demo.properties", repository.getId());
    importTranslations(asset.getId(), "source-xliff_", "fr-FR");
    importTranslations(asset.getId(), "source-xliff_", "ja-JP");

    getL10nJCommander()
        .run(
            "pull",
            "-r",
            repository.getName(),
            "-s",
            getInputResourcesTestDir("source").getAbsolutePath(),
            "-t",
            getTargetTestDir("target").getAbsolutePath(),
            "-ft",
            "PROPERTIES_JAVA");

    getL10nJCommander()
        .run(
            "pull",
            "-r",
            repository.getName(),
            "-s",
            getInputResourcesTestDir("source_modified").getAbsolutePath(),
            "-t",
            getTargetTestDir("target_modified").getAbsolutePath(),
            "-ft",
            "PROPERTIES_JAVA");

    checkExpectedGeneratedResources();
  }

  @Test
  public void pullPropertiesNoBasenameEnUs() throws Exception {

    Repository repository = createTestRepoUsingRepoService();

    getL10nJCommander()
        .run(
            "push",
            "-r",
            repository.getName(),
            "-s",
            getInputResourcesTestDir("source").getAbsolutePath(),
            "-ft",
            "PROPERTIES_NOBASENAME",
            "-sl",
            "en-US");

    Asset asset = assetClient.getAssetByPathAndRepositoryId("en-US.properties", repository.getId());
    importTranslations(asset.getId(), "source-xliff_", "fr-FR");
    importTranslations(asset.getId(), "source-xliff_", "ja-JP");

    getL10nJCommander()
        .run(
            "pull",
            "-r",
            repository.getName(),
            "-s",
            getInputResourcesTestDir("source").getAbsolutePath(),
            "-t",
            getTargetTestDir("target").getAbsolutePath(),
            "-ft",
            "PROPERTIES_NOBASENAME",
            "-sl",
            "en-US");

    getL10nJCommander()
        .run(
            "pull",
            "-r",
            repository.getName(),
            "-s",
            getInputResourcesTestDir("source_modified").getAbsolutePath(),
            "-t",
            getTargetTestDir("target_modified").getAbsolutePath(),
            "-ft",
            "PROPERTIES_NOBASENAME",
            "-sl",
            "en-US");

    checkExpectedGeneratedResources();
  }

  @Test
  public void leveraging() throws Exception {

    Repository repository = createTestRepoUsingRepoService();

    getL10nJCommander()
        .run(
            "push",
            "-r",
            repository.getName(),
            "-s",
            getInputResourcesTestDir("source").getAbsolutePath(),
            "-ft",
            "PROPERTIES_NOBASENAME");

    Asset asset = assetClient.getAssetByPathAndRepositoryId("en.properties", repository.getId());
    importTranslations(asset.getId(), "source-xliff_", "fr-FR");
    importTranslations(asset.getId(), "source-xliff_", "ja-JP");

    getL10nJCommander()
        .run(
            "pull",
            "-r",
            repository.getName(),
            "-s",
            getInputResourcesTestDir("source").getAbsolutePath(),
            "-t",
            getTargetTestDir("target").getAbsolutePath(),
            "-ft",
            "PROPERTIES_NOBASENAME");

    logger.debug("Change text unit to test leveraging");

    getL10nJCommander()
        .run(
            "push",
            "-r",
            repository.getName(),
            "-s",
            getInputResourcesTestDir("source_modified").getAbsolutePath(),
            "-ft",
            "PROPERTIES_NOBASENAME");

    getL10nJCommander()
        .run(
            "pull",
            "-r",
            repository.getName(),
            "-s",
            getInputResourcesTestDir("source_modified").getAbsolutePath(),
            "-t",
            getTargetTestDir("target_modified").getAbsolutePath(),
            "-ft",
            "PROPERTIES_NOBASENAME");

    checkExpectedGeneratedResources();
  }

  @Test
  public void localeMapping() throws Exception {

    Repository repository = createTestRepoUsingRepoService();

    getL10nJCommander()
        .run(
            "push",
            "-r",
            repository.getName(),
            "-s",
            getInputResourcesTestDir("source").getAbsolutePath());

    Asset asset =
        assetClient.getAssetByPathAndRepositoryId("source-xliff.xliff", repository.getId());
    importTranslations(asset.getId(), "source-xliff_", "fr-FR");
    importTranslations(asset.getId(), "source-xliff_", "ja-JP");

    getL10nJCommander()
        .run(
            "pull",
            "-r",
            repository.getName(),
            "-s",
            getInputResourcesTestDir("source").getAbsolutePath(),
            "-t",
            getTargetTestDir("target").getAbsolutePath(),
            "-lm",
            "fr:fr-FR,fr-FR:fr-FR,ja:ja-JP");

    checkExpectedGeneratedResources();
  }

  @Test
  public void assetMapping() throws Exception {
    Repository repository = createTestRepoUsingRepoService();

    getL10nJCommander()
        .run(
            "push",
            "-r",
            repository.getName(),
            "-s",
            getInputResourcesTestDir("source").getAbsolutePath());

    Asset asset =
        assetClient.getAssetByPathAndRepositoryId("source-xliff.xliff", repository.getId());
    importTranslations(asset.getId(), "source-xliff_", "fr-FR");
    importTranslations(asset.getId(), "source-xliff_", "ja-JP");

    getL10nJCommander()
        .run(
            "pull",
            "-r",
            repository.getName(),
            "-s",
            getInputResourcesTestDir("mapping").getAbsolutePath(),
            "-t",
            getTargetTestDir("target").getAbsolutePath(),
            "-am",
            "mapping-xliff.xliff:source-xliff.xliff");

    checkExpectedGeneratedResources();
  }

  @Test
  public void pullAndroidStrings() throws Exception {

    Repository repository = createTestRepoUsingRepoService();

    getL10nJCommander()
        .run(
            "push",
            "-r",
            repository.getName(),
            "-s",
            getInputResourcesTestDir("source").getAbsolutePath());

    Asset asset =
        assetClient.getAssetByPathAndRepositoryId("res/values/strings.xml", repository.getId());
    importTranslations(asset.getId(), "source-xliff_", "fr-FR");
    importTranslations(asset.getId(), "source-xliff_", "ja-JP");

    getL10nJCommander()
        .run(
            "pull",
            "-r",
            repository.getName(),
            "-s",
            getInputResourcesTestDir("source").getAbsolutePath(),
            "-t",
            getTargetTestDir("target").getAbsolutePath());

    getL10nJCommander()
        .run(
            "pull",
            "-r",
            repository.getName(),
            "-s",
            getInputResourcesTestDir("source_modified").getAbsolutePath(),
            "-t",
            getTargetTestDir("target_modified").getAbsolutePath());

    checkExpectedGeneratedResources();
  }

  @Test
  public void pullMacStrings() throws Exception {

    Repository repository = createTestRepoUsingRepoService();

    getL10nJCommander()
        .run(
            "push",
            "-r",
            repository.getName(),
            "-s",
            getInputResourcesTestDir("source").getAbsolutePath());

    Asset asset =
        assetClient.getAssetByPathAndRepositoryId(
            "en.lproj/Localizable.strings", repository.getId());
    importTranslations(asset.getId(), "source-xliff_", "fr-FR");
    importTranslations(asset.getId(), "source-xliff_", "ja-JP");

    getL10nJCommander()
        .run(
            "pull",
            "-r",
            repository.getName(),
            "-s",
            getInputResourcesTestDir("source").getAbsolutePath(),
            "-t",
            getTargetTestDir("target").getAbsolutePath());

    getL10nJCommander()
        .run(
            "pull",
            "-r",
            repository.getName(),
            "-s",
            getInputResourcesTestDir("source_modified").getAbsolutePath(),
            "-t",
            getTargetTestDir("target_modified").getAbsolutePath());

    checkExpectedGeneratedResources();
  }

  @Test
  public void pullMacStringsdict() throws Exception {

    Repository repository = createTestRepoUsingRepoService();

    getL10nJCommander()
        .run(
            "push",
            "-r",
            repository.getName(),
            "-s",
            getInputResourcesTestDir("source").getAbsolutePath());

    Asset asset =
        assetClient.getAssetByPathAndRepositoryId(
            "en.lproj/Localizable.stringsdict", repository.getId());
    importTranslations(asset.getId(), "source-xliff_", "fr-FR");
    importTranslations(asset.getId(), "source-xliff_", "ja-JP");

    getL10nJCommander()
        .run(
            "pull",
            "-r",
            repository.getName(),
            "-s",
            getInputResourcesTestDir("source").getAbsolutePath(),
            "-t",
            getTargetTestDir("target").getAbsolutePath());

    getL10nJCommander()
        .run(
            "pull",
            "-r",
            repository.getName(),
            "-s",
            getInputResourcesTestDir("source_modified").getAbsolutePath(),
            "-t",
            getTargetTestDir("target_modified").getAbsolutePath());

    checkExpectedGeneratedResources();
  }

  @Test
  public void pullResw() throws Exception {

    Repository repository = createTestRepoUsingRepoService();

    getL10nJCommander()
        .run(
            "push",
            "-r",
            repository.getName(),
            "-s",
            getInputResourcesTestDir("source").getAbsolutePath());

    Asset asset =
        assetClient.getAssetByPathAndRepositoryId("en/Resources.resw", repository.getId());
    importTranslations(asset.getId(), "source-xliff_", "fr-FR");
    importTranslations(asset.getId(), "source-xliff_", "ja-JP");

    getL10nJCommander()
        .run(
            "pull",
            "-r",
            repository.getName(),
            "-s",
            getInputResourcesTestDir("source").getAbsolutePath(),
            "-t",
            getTargetTestDir("target").getAbsolutePath(),
            "-lm",
            "fr:fr-FR,fr-CA:fr-CA,ja:ja-JP");

    getL10nJCommander()
        .run(
            "pull",
            "-r",
            repository.getName(),
            "-s",
            getInputResourcesTestDir("source_modified").getAbsolutePath(),
            "-t",
            getTargetTestDir("target_modified").getAbsolutePath(),
            "-lm",
            "fr:fr-FR,fr-CA:fr-CA,ja:ja-JP");

    checkExpectedGeneratedResources();
  }

  @Test
  public void pullResx() throws Exception {

    Repository repository = createTestRepoUsingRepoService();

    getL10nJCommander()
        .run(
            "push",
            "-r",
            repository.getName(),
            "-s",
            getInputResourcesTestDir("source").getAbsolutePath());

    Asset asset = assetClient.getAssetByPathAndRepositoryId("Test.resx", repository.getId());
    importTranslations(asset.getId(), "source-xliff_", "fr-FR");
    importTranslations(asset.getId(), "source-xliff_", "ja-JP");

    getL10nJCommander()
        .run(
            "pull",
            "-r",
            repository.getName(),
            "-s",
            getInputResourcesTestDir("source").getAbsolutePath(),
            "-t",
            getTargetTestDir("target").getAbsolutePath());

    getL10nJCommander()
        .run(
            "pull",
            "-r",
            repository.getName(),
            "-s",
            getInputResourcesTestDir("source_modified").getAbsolutePath(),
            "-t",
            getTargetTestDir("target_modified").getAbsolutePath());

    checkExpectedGeneratedResources();
  }

  @Test
  public void pullResxSourceRegex() throws Exception {

    Repository repository = createTestRepoUsingRepoService();

    getL10nJCommander()
        .run(
            "push",
            "-r",
            repository.getName(),
            "-s",
            getInputResourcesTestDir("source").getAbsolutePath(),
            "-sr",
            "Localization\\.resx|Test\\.resx");

    Asset asset =
        assetClient.getAssetByPathAndRepositoryId("Localization.resx", repository.getId());
    importTranslations(asset.getId(), "source-xliff_Localization_", "fr-FR");
    importTranslations(asset.getId(), "source-xliff_Localization_", "ja-JP");
    asset = assetClient.getAssetByPathAndRepositoryId("Test.resx", repository.getId());
    importTranslations(asset.getId(), "source-xliff_Test_", "fr-FR");
    importTranslations(asset.getId(), "source-xliff_Test_", "ja-JP");

    getL10nJCommander()
        .run(
            "pull",
            "-r",
            repository.getName(),
            "-s",
            getInputResourcesTestDir("source").getAbsolutePath(),
            "-t",
            getTargetTestDir("target").getAbsolutePath(),
            "-sr",
            "Localization\\.resx|Test\\.resx");

    getL10nJCommander()
        .run(
            "pull",
            "-r",
            repository.getName(),
            "-s",
            getInputResourcesTestDir("source_modified").getAbsolutePath(),
            "-t",
            getTargetTestDir("target_modified").getAbsolutePath(),
            "-sr",
            "Localization\\.resx|Test\\.resx");

    checkExpectedGeneratedResources();
  }

  @Test
  public void pullDirectoryIncludeExcludePatterns() throws Exception {

    Repository repository = createTestRepoUsingRepoService();

    getL10nJCommander()
        .run(
            "push",
            "-r",
            repository.getName(),
            "-s",
            getInputResourcesTestDir("source").getAbsolutePath(),
            "--dir-path-include-patterns",
            "*/resources",
            "other",
            "--dir-path-exclude-patterns",
            "b/resources");

    Asset asset =
        assetClient.getAssetByPathAndRepositoryId(
            "a/resources/demo.properties", repository.getId());
    importTranslations(asset.getId(), "source-xliff_", "fr-FR");
    importTranslations(asset.getId(), "source-xliff_", "ja-JP");
    asset =
        assetClient.getAssetByPathAndRepositoryId(
            "c/resources/demo.properties", repository.getId());
    importTranslations(asset.getId(), "source-xliff_", "fr-FR");
    importTranslations(asset.getId(), "source-xliff_", "ja-JP");
    asset = assetClient.getAssetByPathAndRepositoryId("other/demo.properties", repository.getId());
    importTranslations(asset.getId(), "source-xliff_", "fr-FR");
    importTranslations(asset.getId(), "source-xliff_", "ja-JP");

    getL10nJCommander()
        .run(
            "pull",
            "-r",
            repository.getName(),
            "-s",
            getInputResourcesTestDir("source").getAbsolutePath(),
            "-t",
            getTargetTestDir("target").getAbsolutePath(),
            "--dir-path-include-patterns",
            "*/resources",
            "other",
            "--dir-path-exclude-patterns",
            "b/resources");

    checkExpectedGeneratedResources();
  }

  @Test
  public void testLatestTMTextUnitVariant() throws Exception {
    Repository repository1 = createTestRepoUsingRepoService("repo1", false);
    Repository repository2 = createTestRepoUsingRepoService("repo2", false);

    getL10nJCommander()
        .run(
            "push",
            "-r",
            repository1.getName(),
            "-s",
            getInputResourcesTestDir("source").getAbsolutePath());
    getL10nJCommander()
        .run(
            "push",
            "-r",
            repository2.getName(),
            "-s",
            getInputResourcesTestDir("source").getAbsolutePath());

    Asset asset1 =
        assetClient.getAssetByPathAndRepositoryId("source-xliff.xliff", repository1.getId());
    importTranslations(asset1.getId(), "source-xliff_", "fr-FR");

    logger.debug("Test findLatestTMTextUnitVariant");
    TMTextUnitVariant latestTmTextUnitVariantOfRepository1 =
        tmTextUnitVariantRepository.findTopByTmTextUnitTmIdOrderByCreatedDateDesc(
            repository1.getTm().getId());
    assertNotNull(
        "should have TMTextUnitVariant from imports above", latestTmTextUnitVariantOfRepository1);

    logger.debug("Test findLatestTMTextUnitVariant again after a translation is added to other TM");
    Asset asset2 =
        assetClient.getAssetByPathAndRepositoryId("source-xliff.xliff", repository2.getId());
    importTranslations(asset2.getId(), "source-xliff_", "fr-FR");
    TMTextUnitVariant tmTextUnitVariant =
        tmTextUnitVariantRepository.findTopByTmTextUnitTmIdOrderByCreatedDateDesc(
            repository1.getTm().getId());
    assertEquals(
        "should have returned the same TMTextUnitVariant as above latestTmTextUnitVariantOfRepository1",
        latestTmTextUnitVariantOfRepository1.getId(),
        tmTextUnitVariant.getId());
  }

  @Test
  public void pullXcodeXliff() throws Exception {

    Repository repository = createTestRepoUsingRepoService();

    getL10nJCommander()
        .run(
            "push",
            "-r",
            repository.getName(),
            "-s",
            getInputResourcesTestDir("source").getAbsolutePath(),
            "-ft",
            "XCODE_XLIFF");

    Asset asset = assetClient.getAssetByPathAndRepositoryId("en.xliff", repository.getId());
    importTranslations(asset.getId(), "source-xliff_", "fr-FR");
    importTranslations(asset.getId(), "source-xliff_", "ja-JP");

    getL10nJCommander()
        .run(
            "pull",
            "-r",
            repository.getName(),
            "-s",
            getInputResourcesTestDir("source").getAbsolutePath(),
            "-t",
            getTargetTestDir("target").getAbsolutePath(),
            "-lm",
            "fr:fr-FR,fr-CA:fr-CA,ja:ja-JP",
            "-ft",
            "XCODE_XLIFF");

    getL10nJCommander()
        .run(
            "pull",
            "-r",
            repository.getName(),
            "-s",
            getInputResourcesTestDir("source_modified").getAbsolutePath(),
            "-t",
            getTargetTestDir("target_modified").getAbsolutePath(),
            "-lm",
            "fr:fr-FR,fr-CA:fr-CA,ja:ja-JP",
            "-ft",
            "XCODE_XLIFF");

    checkExpectedGeneratedResources();
  }

  @Test
  public void pullPo() throws Exception {

    Repository repository = createTestRepoUsingRepoService();

    getL10nJCommander()
        .run(
            "push",
            "-r",
            repository.getName(),
            "-s",
            getInputResourcesTestDir("source").getAbsolutePath());

    Asset asset =
        assetClient.getAssetByPathAndRepositoryId("LC_MESSAGES/messages.pot", repository.getId());
    importTranslations(asset.getId(), "source-xliff_", "fr-FR");
    importTranslations(asset.getId(), "source-xliff_", "ja-JP");

    getL10nJCommander()
        .run(
            "pull",
            "-r",
            repository.getName(),
            "-s",
            getInputResourcesTestDir("source").getAbsolutePath(),
            "-t",
            getTargetTestDir("target").getAbsolutePath(),
            "-lm",
            "fr:fr-FR,fr-CA:fr-CA,ja:ja-JP");

    getL10nJCommander()
        .run(
            "pull",
            "-r",
            repository.getName(),
            "-s",
            getInputResourcesTestDir("source_modified").getAbsolutePath(),
            "-t",
            getTargetTestDir("target_modified").getAbsolutePath(),
            "-lm",
            "fr:fr-FR,fr-CA:fr-CA,ja:ja-JP");

    checkExpectedGeneratedResources();
  }

  @Test
  public void pullHtml() throws Exception {

    Repository repository = createTestRepoUsingRepoService();

    getL10nJCommander()
        .run(
            "push",
            "-r",
            repository.getName(),
            "-s",
            getInputResourcesTestDir("source").getAbsolutePath(),
            "-ft",
            "HTML_ALPHA");

    Asset asset = assetClient.getAssetByPathAndRepositoryId("demo.html", repository.getId());
    importTranslations(asset.getId(), "source-xliff_", "fr-FR");
    importTranslations(asset.getId(), "source-xliff_", "ja-JP");

    getL10nJCommander()
        .run(
            "pull",
            "-r",
            repository.getName(),
            "-s",
            getInputResourcesTestDir("source").getAbsolutePath(),
            "-t",
            getTargetTestDir("target").getAbsolutePath(),
            "-lm",
            "fr:fr-FR,fr-CA:fr-CA,ja:ja-JP",
            "-ft",
            "HTML_ALPHA");

    getL10nJCommander()
        .run(
            "pull",
            "-r",
            repository.getName(),
            "-s",
            getInputResourcesTestDir("source_modified").getAbsolutePath(),
            "-t",
            getTargetTestDir("target_modified").getAbsolutePath(),
            "-lm",
            "fr:fr-FR,fr-CA:fr-CA,ja:ja-JP",
            "-ft",
            "HTML_ALPHA");

    checkExpectedGeneratedResources();
  }

  @Test
  public void removeUntranslated() throws Exception {

    Repository repository = createTestRepoUsingRepoService();

    getL10nJCommander()
        .run(
            "push",
            "-r",
            repository.getName(),
            "-s",
            getInputResourcesTestDir("source").getAbsolutePath());

    Asset asset =
        assetClient.getAssetByPathAndRepositoryId("LC_MESSAGES/messages.pot", repository.getId());
    importTranslations(asset.getId(), "source-xliff_", "fr-FR");
    importTranslations(asset.getId(), "source-xliff_", "ja-JP");

    getL10nJCommander()
        .run(
            "pull",
            "-r",
            repository.getName(),
            "-s",
            getInputResourcesTestDir("source").getAbsolutePath(),
            "-t",
            getTargetTestDir("target").getAbsolutePath(),
            "-lm",
            "fr:fr-FR,ja:ja-JP",
            "--inheritance-mode",
            "REMOVE_UNTRANSLATED");

    getL10nJCommander()
        .run(
            "pull",
            "-r",
            repository.getName(),
            "-s",
            getInputResourcesTestDir("source_modified").getAbsolutePath(),
            "-t",
            getTargetTestDir("target_modified").getAbsolutePath(),
            "-lm",
            "fr:fr-FR,ja:ja-JP",
            "--inheritance-mode",
            "REMOVE_UNTRANSLATED");

    checkExpectedGeneratedResources();
  }

  @Test
  public void onlyApproved() throws Exception {

    Repository repository = createTestRepoUsingRepoService();

    getL10nJCommander()
        .run(
            "push",
            "-r",
            repository.getName(),
            "-s",
            getInputResourcesTestDir("source").getAbsolutePath());

    Asset asset = assetClient.getAssetByPathAndRepositoryId("test.xliff", repository.getId());
    importTranslations(asset.getId(), "source-xliff_", "fr-FR");
    importTranslations(asset.getId(), "source-xliff_", "ja-JP");

    updateTranslationsStatus(asset.getId(), TMTextUnitVariant.Status.REVIEW_NEEDED, "fr-FR");
    updateTranslationsStatus(asset.getId(), TMTextUnitVariant.Status.REVIEW_NEEDED, "ja-JP");

    getL10nJCommander()
        .run(
            "pull",
            "-r",
            repository.getName(),
            "-s",
            getInputResourcesTestDir("source").getAbsolutePath(),
            "-t",
            getTargetTestDir("target").getAbsolutePath(),
            "-lm",
            "fr:fr-FR,fr-CA:fr-CA,ja:ja-JP",
            "--inheritance-mode",
            "REMOVE_UNTRANSLATED",
            "--status",
            "ACCEPTED");

    updateTranslationsStatus(asset.getId(), TMTextUnitVariant.Status.APPROVED, "fr-FR");
    updateTranslationsStatus(asset.getId(), TMTextUnitVariant.Status.APPROVED, "ja-JP");

    getL10nJCommander()
        .run(
            "pull",
            "-r",
            repository.getName(),
            "-s",
            getInputResourcesTestDir("source_modified").getAbsolutePath(),
            "-t",
            getTargetTestDir("target_modified").getAbsolutePath(),
            "-lm",
            "fr:fr-FR,fr-CA:fr-CA,ja:ja-JP",
            "--inheritance-mode",
            "REMOVE_UNTRANSLATED",
            "--status",
            "ACCEPTED");

    checkExpectedGeneratedResources();
  }

  @Test
  public void pullXtb() throws Exception {
    Repository repository = createTestRepoUsingRepoService();

    getL10nJCommander()
        .run(
            "push",
            "-r",
            repository.getName(),
            "-s",
            getInputResourcesTestDir("source").getAbsolutePath(),
            "-sl",
            "en-US");

    Asset asset =
        assetClient.getAssetByPathAndRepositoryId("Resources-en-US.xtb", repository.getId());
    importTranslations(asset.getId(), "source-xliff_", "fr-FR");
    importTranslations(asset.getId(), "source-xliff_", "ja-JP");

    getL10nJCommander()
        .run(
            "pull",
            "-r",
            repository.getName(),
            "-s",
            getInputResourcesTestDir("source").getAbsolutePath(),
            "-t",
            getTargetTestDir("target").getAbsolutePath(),
            "-sl",
            "en-US");

    getL10nJCommander()
        .run(
            "pull",
            "-r",
            repository.getName(),
            "-s",
            getInputResourcesTestDir("source_modified").getAbsolutePath(),
            "-t",
            getTargetTestDir("target_modified").getAbsolutePath(),
            "-sl",
            "en-US");

    checkExpectedGeneratedResources();
  }

  @Test
  public void pullCsv() throws Exception {
    Repository repository = createTestRepoUsingRepoService();

    getL10nJCommander()
        .run(
            "push",
            "-r",
            repository.getName(),
            "-s",
            getInputResourcesTestDir("source").getAbsolutePath());

    Asset asset = assetClient.getAssetByPathAndRepositoryId("demo.csv", repository.getId());

    importTranslations(asset.getId(), "source-xliff_", "fr-FR");
    importTranslations(asset.getId(), "source-xliff_", "ja-JP");

    getL10nJCommander()
        .run(
            "pull",
            "-r",
            repository.getName(),
            "-s",
            getInputResourcesTestDir("source").getAbsolutePath(),
            "-t",
            getTargetTestDir("target").getAbsolutePath());

    getL10nJCommander()
        .run(
            "pull",
            "-r",
            repository.getName(),
            "-s",
            getInputResourcesTestDir("source_modified").getAbsolutePath(),
            "-t",
            getTargetTestDir("target_modified").getAbsolutePath());

    checkExpectedGeneratedResources();
  }

  @Test
  public void pullCsvAdobeMagento() throws Exception {
    Repository repository = createTestRepoUsingRepoService();

    getL10nJCommander()
        .run(
            "push",
            "-r",
            repository.getName(),
            "-s",
            getInputResourcesTestDir("source").getAbsolutePath(),
            "-ft",
            "CSV_ADOBE_MAGENTO",
            "-sl",
            "en_US");

    Asset asset = assetClient.getAssetByPathAndRepositoryId("i18n/en_US.csv", repository.getId());

    importTranslations(asset.getId(), "source-xliff_", "fr-FR");
    importTranslations(asset.getId(), "source-xliff_", "ja-JP");

    getL10nJCommander()
        .run(
            "pull",
            "-r",
            repository.getName(),
            "-s",
            getInputResourcesTestDir("source").getAbsolutePath(),
            "-t",
            getTargetTestDir("target").getAbsolutePath(),
            "-ft",
            "CSV_ADOBE_MAGENTO",
            "-sl",
            "en_US");

    getL10nJCommander()
        .run(
            "pull",
            "-r",
            repository.getName(),
            "-s",
            getInputResourcesTestDir("source_modified").getAbsolutePath(),
            "-t",
            getTargetTestDir("target_modified").getAbsolutePath(),
            "-ft",
            "CSV_ADOBE_MAGENTO",
            "-sl",
            "en_US");

    checkExpectedGeneratedResources();
  }

  @Test
  public void pullJS() throws Exception {
    Repository repository = createTestRepoUsingRepoService();

    getL10nJCommander()
        .run(
            "push",
            "-r",
            repository.getName(),
            "-s",
            getInputResourcesTestDir("source").getAbsolutePath());

    Asset asset = assetClient.getAssetByPathAndRepositoryId("en.js", repository.getId());

    importTranslations(asset.getId(), "source-xliff_", "fr-FR");
    importTranslations(asset.getId(), "source-xliff_", "ja-JP");

    getL10nJCommander()
        .run(
            "pull",
            "-r",
            repository.getName(),
            "-s",
            getInputResourcesTestDir("source").getAbsolutePath(),
            "-t",
            getTargetTestDir("target").getAbsolutePath(),
            "-lm",
            "fr:fr-FR,ja:ja-JP");

    getL10nJCommander()
        .run(
            "pull",
            "-r",
            repository.getName(),
            "-s",
            getInputResourcesTestDir("source_modified").getAbsolutePath(),
            "-t",
            getTargetTestDir("target_modified").getAbsolutePath(),
            "-lm",
            "fr:fr-FR,ja:ja-JP");

    checkExpectedGeneratedResources();
  }

  @Test
  public void pullTS() throws Exception {
    Repository repository = createTestRepoUsingRepoService();

    getL10nJCommander()
        .run(
            "push",
            "-r",
            repository.getName(),
            "-s",
            getInputResourcesTestDir("source").getAbsolutePath());

    Asset asset = assetClient.getAssetByPathAndRepositoryId("en.ts", repository.getId());

    importTranslations(asset.getId(), "source-xliff_", "fr-FR");
    importTranslations(asset.getId(), "source-xliff_", "ja-JP");

    getL10nJCommander()
        .run(
            "pull",
            "-r",
            repository.getName(),
            "-s",
            getInputResourcesTestDir("source").getAbsolutePath(),
            "-t",
            getTargetTestDir("target").getAbsolutePath(),
            "-lm",
            "fr:fr-FR,ja:ja-JP");

    getL10nJCommander()
        .run(
            "pull",
            "-r",
            repository.getName(),
            "-s",
            getInputResourcesTestDir("source_modified").getAbsolutePath(),
            "-t",
            getTargetTestDir("target_modified").getAbsolutePath(),
            "-lm",
            "fr:fr-FR,ja:ja-JP");

    checkExpectedGeneratedResources();
  }

  @Test
  public void pullJson() throws Exception {

    Repository repository = createTestRepoUsingRepoService();

    getL10nJCommander()
        .run(
            "push",
            "-r",
            repository.getName(),
            "-s",
            getInputResourcesTestDir("source").getAbsolutePath(),
            "-ft",
            "JSON");

    Asset asset = assetClient.getAssetByPathAndRepositoryId("demo.json", repository.getId());
    importTranslations(asset.getId(), "source-xliff_", "fr-FR");
    importTranslations(asset.getId(), "source-xliff_", "ja-JP");

    getL10nJCommander()
        .run(
            "pull",
            "-r",
            repository.getName(),
            "-s",
            getInputResourcesTestDir("source").getAbsolutePath(),
            "-t",
            getTargetTestDir("target").getAbsolutePath(),
            "-ft",
            "JSON");

    getL10nJCommander()
        .run(
            "pull",
            "-r",
            repository.getName(),
            "-s",
            getInputResourcesTestDir("source_modified").getAbsolutePath(),
            "-t",
            getTargetTestDir("target_modified").getAbsolutePath(),
            "-ft",
            "JSON");

    checkExpectedGeneratedResources();
  }

  @Test
  public void pullJsonNobasename() throws Exception {

    Repository repository = createTestRepoUsingRepoService();

    getL10nJCommander()
        .run(
            "push",
            "-r",
            repository.getName(),
            "-s",
            getInputResourcesTestDir("source").getAbsolutePath(),
            "-ft",
            "JSON_NOBASENAME");

    Asset asset = assetClient.getAssetByPathAndRepositoryId("en.json", repository.getId());
    importTranslations(asset.getId(), "source-xliff_", "fr-FR");
    importTranslations(asset.getId(), "source-xliff_", "ja-JP");

    getL10nJCommander()
        .run(
            "pull",
            "-r",
            repository.getName(),
            "-s",
            getInputResourcesTestDir("source").getAbsolutePath(),
            "-t",
            getTargetTestDir("target").getAbsolutePath(),
            "-ft",
            "JSON_NOBASENAME");

    getL10nJCommander()
        .run(
            "pull",
            "-r",
            repository.getName(),
            "-s",
            getInputResourcesTestDir("source_modified").getAbsolutePath(),
            "-t",
            getTargetTestDir("target_modified").getAbsolutePath(),
            "-ft",
            "JSON_NOBASENAME");

    checkExpectedGeneratedResources();
  }

  @Test
  public void pullJsonWithNote() throws Exception {

    Repository repository = createTestRepoUsingRepoService();

    getL10nJCommander()
        .run(
            "push",
            "-r",
            repository.getName(),
            "-s",
            getInputResourcesTestDir("source").getAbsolutePath(),
            "-fo",
            "noteKeyPattern=note",
            "extractAllPairs=false",
            "exceptions=string",
            "-ft",
            "JSON");

    Asset asset = assetClient.getAssetByPathAndRepositoryId("demo.json", repository.getId());
    importTranslations(asset.getId(), "source-xliff_", "fr-FR");
    importTranslations(asset.getId(), "source-xliff_", "ja-JP");

    getL10nJCommander()
        .run(
            "pull",
            "-r",
            repository.getName(),
            "-s",
            getInputResourcesTestDir("source").getAbsolutePath(),
            "-t",
            getTargetTestDir("target").getAbsolutePath(),
            "-fo",
            "noteKeyPattern=note",
            "extractAllPairs=false",
            "exceptions=string",
            "-ft",
            "JSON");

    getL10nJCommander()
        .run(
            "pull",
            "-r",
            repository.getName(),
            "-s",
            getInputResourcesTestDir("source_modified").getAbsolutePath(),
            "-t",
            getTargetTestDir("target_modified").getAbsolutePath(),
            "-fo",
            "noteKeyPattern=note",
            "extractAllPairs=false",
            "exceptions=string",
            "-ft",
            "JSON");

    checkExpectedGeneratedResources();
  }

  @Test
  public void pullJsonDefaultFormatJs() throws Exception {

    Repository repository = createTestRepoUsingRepoService();

    getL10nJCommander()
        .run(
            "push",
            "-r",
            repository.getName(),
            "-s",
            getInputResourcesTestDir("source").getAbsolutePath(),
            "-fo",
            "noteKeyPattern=description",
            "extractAllPairs=false",
            "exceptions=defaultMessage",
            "-ft",
            "JSON_NOBASENAME");

    Asset asset = assetClient.getAssetByPathAndRepositoryId("en.json", repository.getId());
    importTranslations(asset.getId(), "source-xliff_", "fr-FR");
    importTranslations(asset.getId(), "source-xliff_", "ja-JP");

    getL10nJCommander()
        .run(
            "pull",
            "-r",
            repository.getName(),
            "-s",
            getInputResourcesTestDir("source").getAbsolutePath(),
            "-t",
            getTargetTestDir("target").getAbsolutePath(),
            "-fo",
            "noteKeyPattern=description",
            "extractAllPairs=false",
            "exceptions=defaultMessage",
            "-ft",
            "JSON_NOBASENAME");

    getL10nJCommander()
        .run(
            "pull",
            "-r",
            repository.getName(),
            "-s",
            getInputResourcesTestDir("source_modified").getAbsolutePath(),
            "-t",
            getTargetTestDir("target_modified").getAbsolutePath(),
            "-fo",
            "noteKeyPattern=description",
            "extractAllPairs=false",
            "exceptions=defaultMessage",
            "-ft",
            "JSON_NOBASENAME");

    checkExpectedGeneratedResources();
  }

  @Test
  public void pullJsonDefaultFormatJsRemoveUntranslated() throws Exception {
    Repository repository = createTestRepoUsingRepoService();

    getL10nJCommander()
        .run(
            "push",
            "-r",
            repository.getName(),
            "-s",
            getInputResourcesTestDir("source").getAbsolutePath(),
            "-fo",
            "noteKeyPattern=description",
            "extractAllPairs=false",
            "exceptions=defaultMessage",
            "-ft",
            "JSON_NOBASENAME");

    Asset asset = assetClient.getAssetByPathAndRepositoryId("en.json", repository.getId());
    importTranslations(asset.getId(), "source-xliff_", "fr-FR");
    importTranslations(asset.getId(), "source-xliff_", "ja-JP");

    getL10nJCommander()
        .run(
            "pull",
            "-r",
            repository.getName(),
            "-s",
            getInputResourcesTestDir("source").getAbsolutePath(),
            "-t",
            getTargetTestDir("target").getAbsolutePath(),
            "-fo",
            "noteKeyPattern=description",
            "extractAllPairs=false",
            "exceptions=defaultMessage",
            "-ft",
            "JSON_NOBASENAME",
            "--inheritance-mode",
            "REMOVE_UNTRANSLATED");

    checkExpectedGeneratedResources();
  }

  @Test
  public void pullJsonFromChromeExtension() throws Exception {

    Repository repository = createTestRepoUsingRepoService();

    getL10nJCommander()
        .run(
            "push",
            "-r",
            repository.getName(),
            "-s",
            getInputResourcesTestDir("source").getAbsolutePath(),
            "-ft",
            "CHROME_EXT_JSON");

    Asset asset =
        assetClient.getAssetByPathAndRepositoryId("_locales/en/messages.json", repository.getId());
    importTranslations(asset.getId(), "source-xliff_", "fr-FR");
    importTranslations(asset.getId(), "source-xliff_", "ja-JP");

    getL10nJCommander()
        .run(
            "pull",
            "-r",
            repository.getName(),
            "-s",
            getInputResourcesTestDir("source").getAbsolutePath(),
            "-t",
            getTargetTestDir("target").getAbsolutePath(),
            "-ft",
            "CHROME_EXT_JSON");

    getL10nJCommander()
        .run(
            "pull",
            "-r",
            repository.getName(),
            "-s",
            getInputResourcesTestDir("source_modified").getAbsolutePath(),
            "-t",
            getTargetTestDir("target_modified").getAbsolutePath(),
            "-ft",
            "CHROME_EXT_JSON");

    checkExpectedGeneratedResources();
  }

  @Test
  public void pullJsonI18NextParser() throws Exception {

    Repository repository = createTestRepoUsingRepoService();

    getL10nJCommander()
        .run(
            "push",
            "-r",
            repository.getName(),
            "-s",
            getInputResourcesTestDir("source").getAbsolutePath(),
            "-ft",
            "I18NEXT_PARSER_JSON");

    Asset asset =
        assetClient.getAssetByPathAndRepositoryId("locales/en/demo.json", repository.getId());
    importTranslations(asset.getId(), "source-xliff_", "fr-FR");
    importTranslations(asset.getId(), "source-xliff_", "ja-JP");

    getL10nJCommander()
        .run(
            "pull",
            "-r",
            repository.getName(),
            "-s",
            getInputResourcesTestDir("source").getAbsolutePath(),
            "-t",
            getTargetTestDir("target").getAbsolutePath(),
            "-ft",
            "I18NEXT_PARSER_JSON");

    getL10nJCommander()
        .run(
            "pull",
            "-r",
            repository.getName(),
            "-s",
            getInputResourcesTestDir("source_modified").getAbsolutePath(),
            "-t",
            getTargetTestDir("target_modified").getAbsolutePath(),
            "-ft",
            "I18NEXT_PARSER_JSON");

    checkExpectedGeneratedResources();
  }

  @Test
  public void pullFullyTranslated() throws Exception {

    Repository repository = createTestRepoUsingRepoService();
    repositoryService.addRepositoryLocale(repository, "en-AU", null, false);

    getL10nJCommander()
        .run(
            "push",
            "-r",
            repository.getName(),
            "-s",
            getInputResourcesTestDir("source").getAbsolutePath());

    Asset asset = assetClient.getAssetByPathAndRepositoryId("demo.properties", repository.getId());
    importTranslations(asset.getId(), "source-xliff_", "fr-FR");
    importTranslations(asset.getId(), "source-xliff_", "ja-JP");

    getL10nJCommander()
        .run(
            "pull",
            "-r",
            repository.getName(),
            "-s",
            getInputResourcesTestDir("source").getAbsolutePath(),
            "-t",
            getTargetTestDir("target").getAbsolutePath());

    waitForCondition(
        "repo stats must be updated - wait for jp to be fully translated and others to be untranslated",
        () -> {
          com.box.l10n.mojito.rest.entity.Repository repo =
              repositoryClient.getRepositoryById(repository.getId());
          RepositoryStatistic repositoryStatistic = repo.getRepositoryStatistic();

          boolean statsReady =
              repositoryStatistic.getRepositoryLocaleStatistics().stream()
                  .allMatch(
                      repositoryLocaleStatistic -> {
                        if ("ja-JP".equals(repositoryLocaleStatistic.getLocale().getBcp47Tag())) {
                          return repositoryLocaleStatistic.getForTranslationCount() == 0;
                        } else {
                          return repositoryLocaleStatistic.getForTranslationCount() > 0;
                        }
                      });

          return !repositoryStatistic.getRepositoryLocaleStatistics().isEmpty() && statsReady;
        });

    getL10nJCommander()
        .run(
            "pull",
            "-r",
            repository.getName(),
            "-s",
            getInputResourcesTestDir("source").getAbsolutePath(),
            "-t",
            getTargetTestDir("target_fully_translated").getAbsolutePath(),
            "--fully-translated");

    checkExpectedGeneratedResources();
  }

  @Test
  public void recordPullPoPlural() throws Exception {
    Repository repository = createTestRepoUsingRepoService();
    repositoryService.addRepositoryLocale(repository, "ru-RU", null, true);

    String pushCommitHash = "ccaa11";

    logger.debug("Create the base commit that correspond to the initial push");
    getL10nJCommander()
        .run(
            "commit-create",
            "-r",
            repository.getName(),
            "--commit-hash",
            pushCommitHash,
            "--author-email",
            "coder@mail.com",
            "--author-name",
            "coder",
            "--creation-date",
            DateTime.now().toString());

    logger.debug("Initial push");
    getL10nJCommander()
        .run(
            "push",
            "-r",
            repository.getName(),
            "-s",
            getInputResourcesTestDir("source").getAbsolutePath(),
            "--record-push-run",
            "--commit-hash",
            pushCommitHash);

    logger.debug("Get pushRun for the initial push to test delta later on");
    PushRun pushRun =
        commitService
            .getLastPushRun(ImmutableList.of(pushCommitHash), repository.getId())
            .orElseThrow(() -> new RuntimeException("There must be a push run"));

    logger.debug("Record a first pull run to generate the baseline");
    getL10nJCommander()
        .run(
            "pull",
            "-r",
            repository.getName(),
            "-s",
            getInputResourcesTestDir("source").getAbsolutePath(),
            "-t",
            getTargetTestDir("target_baseline").getAbsolutePath(),
            "-lm",
            "ru-RU:ru-RU",
            "--record-pull-run");

    getL10nJCommander()
        .run(
            "import",
            "-r",
            repository.getName(),
            "-s",
            getInputResourcesTestDir("source").getAbsolutePath(),
            "-t",
            getInputResourcesTestDir("translations").getAbsolutePath(),
            "-lm",
            "ru-RU:ru-RU");

    logger.debug("Record a second pull run after translation import");
    getL10nJCommander()
        .run(
            "pull",
            "-r",
            repository.getName(),
            "-s",
            getInputResourcesTestDir("source").getAbsolutePath(),
            "-t",
            getTargetTestDir("target_translated").getAbsolutePath(),
            "-lm",
            "ru-RU:ru-RU",
            "--record-pull-run");

    logger.debug("Simulate commit and linked to pull-run");
    String pullRunHash1 = "ddaa11";
    getL10nJCommander()
        .run(
            "commit-create",
            "-r",
            repository.getName(),
            "--commit-hash",
            pullRunHash1,
            "--author-email",
            "coder@mail.com",
            "--author-name",
            "coder",
            "--creation-date",
            DateTime.now().toString());
    getL10nJCommander()
        .run(
            "commit-to-pull-run",
            "-r",
            repository.getName(),
            "-i",
            getTargetTestDir("target_translated").getAbsolutePath(),
            "--commit-hash",
            pullRunHash1);

    // For language like cs-CZ or ru-RU, PO file use only 3 plural forms while Mojito store 4 form
    // (CLDR).
    // This leads to the number of recorded entry in the pull run to lower that the number of
    // current translations.
    // This might look strange but is expected
    Locale ruRU = localeService.findByBcp47Tag("ru-RU");
    List<TMTextUnit> tmTextUnits = tmTextUnitRepository.findByTm_id(repository.getTm().getId());

    long countOfCurrentTranslationForRuRU =
        tmTextUnits.stream()
            .map(
                tmTextUnit ->
                    tmTextUnitCurrentVariantRepository.findByLocale_IdAndTmTextUnit_Id(
                        ruRU.getId(), tmTextUnit.getId()))
            .filter(Objects::nonNull)
            .count();
    Assertions.assertThat(countOfCurrentTranslationForRuRU).isEqualTo(4);

    long countOfPullRunTextUnitVariantForRuRU =
        tmTextUnits.stream()
            .flatMap(
                tmTextUnit ->
                    pullRunTextUnitVariantRepository
                        .findByTmTextUnitVariant_TmTextUnitIdAndLocaleId(
                            tmTextUnit.getId(), ruRU.getId())
                        .stream())
            .count();
    Assertions.assertThat(countOfPullRunTextUnitVariantForRuRU).isEqualTo(3);

    PullRun pullRun =
        commitService
            .getLastPullRun(ImmutableList.of(pullRunHash1), repository.getId())
            .orElseThrow(() -> new RuntimeException("There must be a pull run"));

    // Advance the date of the PullRun to make sure the translation is older than the PullRun
    // creation date
    pullRun.setCreatedDate(pullRun.getCreatedDate().plusDays(1));
    pullRunRepository.save(pullRun);

    DeltaResponseDTO deltas =
        deltaService.getDeltasForRuns(
            repository, null, ImmutableList.of(pushRun), ImmutableList.of(pullRun));

    // As long as the translation for the other plural form wasn't changed
    // after the pull recording was done, it shouldn't be included in the
    // delta results.
    Assertions.assertThat(deltas.getTranslationsPerLocale()).isEmpty();

    pullRunNamesToNormalizedValueForTests();
    checkExpectedGeneratedResources();
  }

  @Test
  public void recordPullRunAndOTA() throws Exception {
    Repository repository = createTestRepoUsingRepoService();

    String pushCommitHash = "ccaa11";

    getL10nJCommander()
        .run(
            "commit-create",
            "-r",
            repository.getName(),
            "--commit-hash",
            pushCommitHash,
            "--author-email",
            "coder@mail.com",
            "--author-name",
            "coder",
            "--creation-date",
            DateTime.now().toString());

    logger.debug("Initial push");
    getL10nJCommander()
        .run(
            "push",
            "-r",
            repository.getName(),
            "-s",
            getInputResourcesTestDir("source").getAbsolutePath(),
            "--record-push-run",
            "--commit-hash",
            pushCommitHash);

    logger.debug("Get pushRun for the initial push to test delta later on");
    PushRun pushRun =
        commitService
            .getLastPushRun(ImmutableList.of(pushCommitHash), repository.getId())
            .orElseThrow(() -> new RuntimeException("There must be a push run"));

    logger.debug("Record a first pull run to generate the baseline");
    getL10nJCommander()
        .run(
            "pull",
            "-r",
            repository.getName(),
            "-s",
            getInputResourcesTestDir("source").getAbsolutePath(),
            "-t",
            getTargetTestDir("target_baseline").getAbsolutePath(),
            "--record-pull-run");

    logger.debug("Simulate commit and linked to pull-run");
    String pullRunHash1 = "ddaa11";
    getL10nJCommander()
        .run(
            "commit-create",
            "-r",
            repository.getName(),
            "--commit-hash",
            pullRunHash1,
            "--author-email",
            "coder@mail.com",
            "--author-name",
            "coder",
            "--creation-date",
            DateTime.now().toString());

    getL10nJCommander()
        .run(
            "commit-to-pull-run",
            "-r",
            repository.getName(),
            "-i",
            getTargetTestDir("target_baseline").getAbsolutePath(),
            "--commit-hash",
            pullRunHash1);

    PullRun pullRunBaseline =
        commitService
            .getLastPullRun(ImmutableList.of(pullRunHash1), repository.getId())
            .orElseThrow(() -> new RuntimeException("There must be a pull run"));

    logger.debug(
        "Check that generating a delta yield empty result for the initial push run and baseline pull run");
    DeltaResponseDTO deltaForBaseline =
        deltaService.getDeltasForRuns(
            repository,
            null,
            // so empty() should basically process nothing
            ImmutableList.of(pushRun),
            ImmutableList.of(pullRunBaseline));

    Assertions.assertThat(deltaForBaseline.getTranslationsPerLocale()).isEmpty();

    logger.debug("Import French translations to test delta generation");
    Asset asset = assetClient.getAssetByPathAndRepositoryId("demo.properties", repository.getId());
    importTranslations(asset.getId(), "source-xliff_", "fr-FR");

    logger.debug(
        "Generating a delta for the initial push run and baseline pull run should now return the new French translations");
    DeltaResponseDTO deltaForBaselineToFrenchImport =
        deltaService.getDeltasForRuns(
            repository, null, ImmutableList.of(pushRun), ImmutableList.of(pullRunBaseline));

    Assertions.assertThat(deltaForBaselineToFrenchImport.getTranslationsPerLocale()).hasSize(1);
    checkFrenchTranslationsInDelta(deltaForBaselineToFrenchImport);

    logger.debug("Record a pull run after the French import for later delta testing");
    getL10nJCommander()
        .run(
            "pull",
            "-r",
            repository.getName(),
            "-s",
            getInputResourcesTestDir("source").getAbsolutePath(),
            "-t",
            getTargetTestDir("target_after_french_import").getAbsolutePath(),
            "--record-pull-run");

    logger.debug("Simulate commit and linked to pull-run");
    String pullRunHash2 = "ddaa22";
    getL10nJCommander()
        .run(
            "commit-create",
            "-r",
            repository.getName(),
            "--commit-hash",
            pullRunHash2,
            "--author-email",
            "coder@mail.com",
            "--author-name",
            "coder",
            "--creation-date",
            DateTime.now().toString());
    getL10nJCommander()
        .run(
            "commit-to-pull-run",
            "-r",
            repository.getName(),
            "-i",
            getTargetTestDir("target_after_french_import").getAbsolutePath(),
            "--commit-hash",
            pullRunHash2);

    PullRun pullRunAfterFrenchImport =
        commitService
            .getLastPullRun(ImmutableList.of(pullRunHash2), repository.getId())
            .orElseThrow(() -> new RuntimeException("There must be a pull run"));

    logger.debug("Add Japanese translations to test delta generation");
    importTranslations(asset.getId(), "source-xliff_", "ja-JP");

    logger.debug(
        "Generating a delta for the initial push run and baseline pull run should now return the new French and Japanese translations");
    DeltaResponseDTO deltaForBaselineToFrenchAndJapaneseImport =
        deltaService.getDeltasForRuns(
            repository, null, ImmutableList.of(pushRun), ImmutableList.of(pullRunBaseline));

    Assertions.assertThat(deltaForBaselineToFrenchAndJapaneseImport.getTranslationsPerLocale())
        .hasSize(2);
    checkFrenchTranslationsInDelta(deltaForBaselineToFrenchAndJapaneseImport);
    checkJapaneseTranslationsInDelta(deltaForBaselineToFrenchAndJapaneseImport);

    logger.debug(
        "Check that the delta for the pull run generated after the French import only return Japanese translations");
    DeltaResponseDTO deltaForAfterFrenchImportToFrenchAndJapaneseImport =
        deltaService.getDeltasForRuns(
            repository,
            null,
            ImmutableList.of(pushRun),
            ImmutableList.of(pullRunAfterFrenchImport));

    Assertions.assertThat(
            deltaForAfterFrenchImportToFrenchAndJapaneseImport.getTranslationsPerLocale())
        .hasSize(1);
    checkJapaneseTranslationsInDelta(deltaForAfterFrenchImportToFrenchAndJapaneseImport);

    logger.debug("Record a pull run after the French and Japanese imports for later delta testing");
    getL10nJCommander()
        .run(
            "pull",
            "-r",
            repository.getName(),
            "-s",
            getInputResourcesTestDir("source").getAbsolutePath(),
            "-t",
            getTargetTestDir("target_after_french_and_japanese_imports").getAbsolutePath(),
            "--record-pull-run");

    logger.debug("Simulate commit and linked to pull-run");
    String pullRunHash3 = "ddaa33";
    getL10nJCommander()
        .run(
            "commit-create",
            "-r",
            repository.getName(),
            "--commit-hash",
            pullRunHash3,
            "--author-email",
            "coder@mail.com",
            "--author-name",
            "coder",
            "--creation-date",
            DateTime.now().toString());
    getL10nJCommander()
        .run(
            "commit-to-pull-run",
            "-r",
            repository.getName(),
            "-i",
            getTargetTestDir("target_after_french_and_japanese_imports").getAbsolutePath(),
            "--commit-hash",
            pullRunHash3);

    PullRun pullRunAfterFrenchAndJapaneseImports =
        commitService
            .getLastPullRun(ImmutableList.of(pullRunHash3), repository.getId())
            .orElseThrow(() -> new RuntimeException("There must be a pull run"));

    logger.debug("Update one translation in French and remove one in Japanese");
    Locale frFR = localeService.findByBcp47Tag("fr-FR");
    Locale jaJP = localeService.findByBcp47Tag("ja-JP");
    TMTextUnit tmTextUnit =
        tmTextUnitRepository.findFirstByAssetIdAndName(asset.getId(), "1_month_duration");
    tmService.addCurrentTMTextUnitVariant(tmTextUnit.getId(), frFR.getId(), "1 mois -- update");
    tmTextUnitCurrentVariantService.removeCurrentVariant(
        tmTextUnitCurrentVariantRepository
            .findByLocale_IdAndTmTextUnit_Id(jaJP.getId(), tmTextUnit.getId())
            .getId());

    logger.debug("Check that the delta for the pull run generated returns 1 modifications");
    DeltaResponseDTO deltaForAfterFrenchAndJapaneseImportsToSmallChanges =
        deltaService.getDeltasForRuns(
            repository,
            null,
            ImmutableList.of(pushRun),
            ImmutableList.of(pullRunAfterFrenchAndJapaneseImports));

    Assertions.assertThat(
            deltaForAfterFrenchImportToFrenchAndJapaneseImport.getTranslationsPerLocale())
        .hasSize(1);
    Assertions.assertThat(
            deltaForAfterFrenchAndJapaneseImportsToSmallChanges
                .getTranslationsPerLocale()
                .get("fr-FR")
                .getTranslationsByTextUnitName())
        .hasSize(1);
    Assertions.assertThat(
            deltaForAfterFrenchAndJapaneseImportsToSmallChanges
                .getTranslationsPerLocale()
                .get("fr-FR")
                .getTranslationsByTextUnitName())
        .extractingByKey("1_month_duration")
        .hasFieldOrPropertyWithValue("text", "1 mois -- update")
        .hasFieldOrPropertyWithValue("deltaType", DeltaType.UPDATED_TRANSLATION);

    logger.debug("Create a second commit that correspond to the second push");
    String pushCommitHashModified = "ccaa22";
    getL10nJCommander()
        .run(
            "commit-create",
            "-r",
            repository.getName(),
            "--commit-hash",
            pushCommitHashModified,
            "--author-email",
            "coder@mail.com",
            "--author-name",
            "coder",
            "--creation-date",
            DateTime.now().toString());

    getL10nJCommander()
        .run(
            "push",
            "-r",
            repository.getName(),
            "-s",
            getInputResourcesTestDir("source_modified").getAbsolutePath(),
            "--record-push-run",
            "--commit-hash",
            pushCommitHashModified);

    logger.debug("Get pushRun for the initial push to test delta later on");
    PushRun pushRunModified =
        commitService
            .getLastPushRun(ImmutableList.of(pushCommitHashModified), repository.getId())
            .orElseThrow(() -> new RuntimeException("There must be a push run"));

    DeltaResponseDTO deltaWithPushModifiedForAfterFrenchAndJapaneseImportsToSmallChanges =
        deltaService.getDeltasForRuns(
            repository,
            null,
            ImmutableList.of(pushRunModified),
            ImmutableList.of(pullRunAfterFrenchAndJapaneseImports));

    Assertions.assertThat(
            deltaWithPushModifiedForAfterFrenchAndJapaneseImportsToSmallChanges
                .getTranslationsPerLocale())
        .hasSize(1);
    Assertions.assertThat(
            deltaWithPushModifiedForAfterFrenchAndJapaneseImportsToSmallChanges
                .getTranslationsPerLocale()
                .get("fr-FR")
                .getTranslationsByTextUnitName())
        .hasSize(1);
    Assertions.assertThat(
            deltaWithPushModifiedForAfterFrenchAndJapaneseImportsToSmallChanges
                .getTranslationsPerLocale()
                .get("fr-FR")
                .getTranslationsByTextUnitName())
        .extractingByKey("1_month_duration")
        .hasFieldOrPropertyWithValue("text", "1 mois -- update")
        .hasFieldOrPropertyWithValue("deltaType", DeltaType.UPDATED_TRANSLATION);

    getL10nJCommander()
        .run(
            "pull",
            "-r",
            repository.getName(),
            "-s",
            getInputResourcesTestDir("source_modified").getAbsolutePath(),
            "-t",
            getTargetTestDir("target_modified").getAbsolutePath(),
            "--record-pull-run");

    logger.debug("Simulate commit and linked to pull-run");
    String pullRunHash4 = "ddaa44";
    getL10nJCommander()
        .run(
            "commit-create",
            "-r",
            repository.getName(),
            "--commit-hash",
            pullRunHash4,
            "--author-email",
            "coder@mail.com",
            "--author-name",
            "coder",
            "--creation-date",
            DateTime.now().toString());
    getL10nJCommander()
        .run(
            "commit-to-pull-run",
            "-r",
            repository.getName(),
            "-i",
            getTargetTestDir("target_modified").getAbsolutePath(),
            "--commit-hash",
            pullRunHash4);

    PullRun pullRunModified =
        commitService
            .getLastPullRun(ImmutableList.of(pullRunHash4), repository.getId())
            .orElseThrow(() -> new RuntimeException("There must be a pull run"));

    DeltaResponseDTO deltaWithPushModifiedForPullModified =
        deltaService.getDeltasForRuns(
            repository, null, ImmutableList.of(pushRunModified), ImmutableList.of(pullRunModified));

    Assertions.assertThat(deltaWithPushModifiedForPullModified.getTranslationsPerLocale())
        .hasSize(0);

    pullRunNamesToNormalizedValueForTests();
    checkExpectedGeneratedResources();
  }

  @Test
  public void pullYaml() throws Exception {
    Repository repository = createTestRepoUsingRepoService();

    getL10nJCommander()
        .run(
            "push",
            "-r",
            repository.getName(),
            "-s",
            getInputResourcesTestDir("source").getAbsolutePath());

    Asset asset = assetClient.getAssetByPathAndRepositoryId("demo.yaml", repository.getId());

    importTranslations(asset.getId(), "source-xliff_", "fr-FR");
    importTranslations(asset.getId(), "source-xliff_", "ja-JP");

    getL10nJCommander()
        .run(
            "pull",
            "-r",
            repository.getName(),
            "-s",
            getInputResourcesTestDir("source").getAbsolutePath(),
            "-t",
            getTargetTestDir("target").getAbsolutePath(),
            "-lm",
            "fr:fr-FR,ja:ja-JP");

    getL10nJCommander()
        .run(
            "pull",
            "-r",
            repository.getName(),
            "-s",
            getInputResourcesTestDir("source_modified").getAbsolutePath(),
            "-t",
            getTargetTestDir("target_modified").getAbsolutePath(),
            "-lm",
            "fr:fr-FR,ja:ja-JP");

    checkExpectedGeneratedResources();
  }

  @Test
  public void pullYamlWithFilterOptions() throws Exception {
    Repository repository = createTestRepoUsingRepoService();

    getL10nJCommander()
        .run(
            "push",
            "-r",
            repository.getName(),
            "-s",
            getInputResourcesTestDir("source").getAbsolutePath(),
            "-fo",
            "extractAllPairs=false",
            "exceptions=1_day_duration|1_year_duration");

    Asset asset = assetClient.getAssetByPathAndRepositoryId("demo.yaml", repository.getId());

    importTranslations(asset.getId(), "source-xliff_", "fr-FR");
    importTranslations(asset.getId(), "source-xliff_", "ja-JP");

    getL10nJCommander()
        .run(
            "pull",
            "-r",
            repository.getName(),
            "-s",
            getInputResourcesTestDir("source").getAbsolutePath(),
            "-t",
            getTargetTestDir("target").getAbsolutePath(),
            "-lm",
            "fr:fr-FR,ja:ja-JP",
            "-fo",
            "extractAllPairs=false",
            "exceptions=1_day_duration|1_year_duration");

    getL10nJCommander()
        .run(
            "pull",
            "-r",
            repository.getName(),
            "-s",
            getInputResourcesTestDir("source_modified").getAbsolutePath(),
            "-t",
            getTargetTestDir("target_modified").getAbsolutePath(),
            "-lm",
            "fr:fr-FR,ja:ja-JP",
            "-fo",
            "extractAllPairs=false",
            "exceptions=1_day_duration|1_year_duration");

    checkExpectedGeneratedResources();
  }

  private void pullRunNamesToNormalizedValueForTests() throws IOException {
    modifyFilesInTargetTestDirectory(
        input -> {
          return input.replaceAll("\\d", "1").replaceAll("[a-z]", "1");
        },
        "pull-run-name.txt");
  }

  private void printDelta(DeltaResponseDTO delta) {
    delta
        .getTranslationsPerLocale()
        .forEach(
            (locale, deltaLocaleDataDTO) -> {
              deltaLocaleDataDTO
                  .getTranslationsByTextUnitName()
                  .forEach(
                      (textUnitName, deltaTranslationDTO) -> {
                        logger.info(locale);
                        logger.info(textUnitName);
                        logger.info(deltaTranslationDTO.getText());
                        logger.info(deltaTranslationDTO.getDeltaType().toString());
                        logger.info("---");
                      });
            });
  }

  private void checkJapaneseTranslationsInDelta(DeltaResponseDTO delta) {
    Assertions.assertThat(
            delta.getTranslationsPerLocale().get("ja-JP").getTranslationsByTextUnitName())
        .hasSize(5);
    Assertions.assertThat(
            delta.getTranslationsPerLocale().get("ja-JP").getTranslationsByTextUnitName())
        .extractingByKey("100_character_description_")
        .hasFieldOrPropertyWithValue("text", "100文字の説明:")
        .hasFieldOrPropertyWithValue("deltaType", DeltaType.NEW_TRANSLATION);
    Assertions.assertThat(
            delta.getTranslationsPerLocale().get("ja-JP").getTranslationsByTextUnitName())
        .extractingByKey("1_hour_duration")
        .hasFieldOrPropertyWithValue("text", "1時間")
        .hasFieldOrPropertyWithValue("deltaType", DeltaType.NEW_TRANSLATION);
    Assertions.assertThat(
            delta.getTranslationsPerLocale().get("ja-JP").getTranslationsByTextUnitName())
        .extractingByKey("1_day_duration")
        .hasFieldOrPropertyWithValue("text", "1日")
        .hasFieldOrPropertyWithValue("deltaType", DeltaType.NEW_TRANSLATION);
    Assertions.assertThat(
            delta.getTranslationsPerLocale().get("ja-JP").getTranslationsByTextUnitName())
        .extractingByKey("1_month_duration")
        .hasFieldOrPropertyWithValue("text", "1か月")
        .hasFieldOrPropertyWithValue("deltaType", DeltaType.NEW_TRANSLATION);
    Assertions.assertThat(
            delta.getTranslationsPerLocale().get("ja-JP").getTranslationsByTextUnitName())
        .extractingByKey("15_min_duration")
        .hasFieldOrPropertyWithValue("text", "15分")
        .hasFieldOrPropertyWithValue("deltaType", DeltaType.NEW_TRANSLATION);
  }

  private void checkFrenchTranslationsInDelta(DeltaResponseDTO delta) {
    Assertions.assertThat(
            delta.getTranslationsPerLocale().get("fr-FR").getTranslationsByTextUnitName())
        .hasSize(5);
    Assertions.assertThat(
            delta.getTranslationsPerLocale().get("fr-FR").getTranslationsByTextUnitName())
        .extractingByKey("100_character_description_")
        .hasFieldOrPropertyWithValue("text", "Description de 100 caractères :")
        .hasFieldOrPropertyWithValue("deltaType", DeltaType.NEW_TRANSLATION);
    Assertions.assertThat(
            delta.getTranslationsPerLocale().get("fr-FR").getTranslationsByTextUnitName())
        .extractingByKey("1_hour_duration")
        .hasFieldOrPropertyWithValue("text", "1 heure")
        .hasFieldOrPropertyWithValue("deltaType", DeltaType.NEW_TRANSLATION);
    Assertions.assertThat(
            delta.getTranslationsPerLocale().get("fr-FR").getTranslationsByTextUnitName())
        .extractingByKey("1_day_duration")
        .hasFieldOrPropertyWithValue("text", "1 jour")
        .hasFieldOrPropertyWithValue("deltaType", DeltaType.NEW_TRANSLATION);
    Assertions.assertThat(
            delta.getTranslationsPerLocale().get("fr-FR").getTranslationsByTextUnitName())
        .extractingByKey("1_month_duration")
        .hasFieldOrPropertyWithValue("text", "1 mois")
        .hasFieldOrPropertyWithValue("deltaType", DeltaType.NEW_TRANSLATION);
    Assertions.assertThat(
            delta.getTranslationsPerLocale().get("fr-FR").getTranslationsByTextUnitName())
        .extractingByKey("15_min_duration")
        .hasFieldOrPropertyWithValue("text", "15 min")
        .hasFieldOrPropertyWithValue("deltaType", DeltaType.NEW_TRANSLATION);
  }
}

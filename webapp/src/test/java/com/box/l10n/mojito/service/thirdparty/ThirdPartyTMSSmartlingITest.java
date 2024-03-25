package com.box.l10n.mojito.service.thirdparty;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.box.l10n.mojito.entity.PollableTask;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.service.asset.AssetRepository;
import com.box.l10n.mojito.service.asset.AssetService;
import com.box.l10n.mojito.service.assetExtraction.ServiceTestBase;
import com.box.l10n.mojito.service.image.ImageService;
import com.box.l10n.mojito.service.pollableTask.PollableTaskService;
import com.box.l10n.mojito.service.repository.RepositoryService;
import com.box.l10n.mojito.service.screenshot.ScreenshotRepository;
import com.box.l10n.mojito.service.screenshot.ScreenshotService;
import com.box.l10n.mojito.service.tm.TMTextUnitRepository;
import com.box.l10n.mojito.smartling.AssetPathAndTextUnitNameKeys;
import com.box.l10n.mojito.smartling.SmartlingClient;
import com.box.l10n.mojito.smartling.SmartlingTestConfig;
import com.box.l10n.mojito.smartling.response.File;
import com.box.l10n.mojito.test.TestIdWatcher;
import com.google.common.collect.ImmutableList;
import java.util.List;
import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration test that needs Smartling account and verfication on the screenshot upload done
 * manually in Smartling
 */
public class ThirdPartyTMSSmartlingITest extends ServiceTestBase {

  static Logger logger = LoggerFactory.getLogger(SmartlingClient.class);

  @Rule public TestIdWatcher testIdWatcher = new TestIdWatcher();

  @Autowired(required = false)
  SmartlingClient smartlingClient;

  @Autowired AssetPathAndTextUnitNameKeys assetPathAndTextUnitNameKeys;

  @Autowired ThirdPartyService thirdPartyService;

  @Autowired RepositoryService repositoryService;

  @Autowired AssetService assetService;

  @Autowired AssetRepository assetRepository;

  @Autowired PollableTaskService pollableTaskService;

  @Autowired ScreenshotService screenshotService;

  @Autowired TMTextUnitRepository tmTextUnitRepository;

  @Autowired ScreenshotRepository screenshotRepository;

  @Autowired ThirdPartyScreenshotRepository thirdPartyScreenshotRepository;

  @Autowired ImageService imageService;

  @Autowired SmartlingTestConfig testConfig;

  @Test
  public void testMappingAndScreenshot() throws Exception {
    Assume.assumeNotNull(smartlingClient);
    Assume.assumeNotNull(testConfig.projectId);

    ThirdPartyServiceTestData thirdPartyServiceTestData =
        new ThirdPartyServiceTestData(testIdWatcher);
    Repository repository = thirdPartyServiceTestData.repository;

    String smartlingContent = getAndroidFileContent();

    String smartlingFileUri = repository.getName() + "/0000_singular_source.xml";
    smartlingClient.uploadFile(
        testConfig.projectId, smartlingFileUri, "android", smartlingContent, null, null, null);

    String pollableTaskName = "testThirdPartyPollableTask";
    PollableTask parentTask =
        pollableTaskService.createPollableTask(null, pollableTaskName, null, 0);

    logger.debug("First mapping");
    thirdPartyService.mapMojitoAndThirdPartyTextUnits(
        repository, testConfig.projectId, "_", ImmutableList.of(), parentTask);

    logger.debug("Second mapping");
    thirdPartyService.mapMojitoAndThirdPartyTextUnits(
        repository, testConfig.projectId, "_", ImmutableList.of(), parentTask);

    thirdPartyService.uploadScreenshotsAndCreateMappings(
        repository, testConfig.projectId, parentTask);
  }

  @Test
  public void testFileUploadAndDeletion() throws Exception {
    Assume.assumeNotNull(smartlingClient);
    Assume.assumeNotNull(testConfig.projectId);

    // Upload a file
    ThirdPartyServiceTestData thirdPartyServiceTestData =
        new ThirdPartyServiceTestData(testIdWatcher);
    Repository repository = thirdPartyServiceTestData.repository;
    String smartlingFileUri = repository.getName() + "/0000_deletion_test.xml";
    smartlingClient.uploadFile(
        testConfig.projectId,
        smartlingFileUri,
        "android",
        getAndroidFileContent(),
        null,
        null,
        null);

    // Verify the file was uploaded
    List<File> files = smartlingClient.getFiles(testConfig.projectId).getItems();
    assertTrue(files.stream().anyMatch(file -> file.getFileUri().equals(smartlingFileUri)));

    // Delete the file
    smartlingClient.deleteFile(testConfig.projectId, smartlingFileUri);

    // Verify the file was deleted
    files = smartlingClient.getFiles(testConfig.projectId).getItems();
    assertFalse(files.stream().anyMatch(file -> file.getFileUri().equals(smartlingFileUri)));
  }

  private static String getAndroidFileContent() {
    String smartlingContent =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<resources>\n"
            + "    <!--comment 1-->\n"
            + "    <string name=\"src/main/res/values/strings.xml#@#hello\" tmTextUnitId=\"946852\">Hello</string>\n"
            + "    <!-- twice the same name in the 3rd party tms shouldn't break the mapping -->\n"
            + "    <string name=\"src/main/res/values/strings.xml#@#hello\" tmTextUnitId=\"8464561\">Hello-samename</string>\n"
            + "    <!--comment 2-->\n"
            + "    <string name=\"src/main/res/values/strings.xml#@#bye\" tmTextUnitId=\"946853\">Bye</string>\n"
            + "    <plurals name=\"src/main/res/values/strings.xml#@#plural_things\">\n"
            + "        <item quantity=\"one\">One thing</item>\n"
            + "        <item quantity=\"other\">Multiple things</item>\n"
            + "    </plurals>"
            + "</resources>";
    return smartlingContent;
  }
}

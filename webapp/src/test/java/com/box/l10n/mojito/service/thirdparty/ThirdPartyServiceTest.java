package com.box.l10n.mojito.service.thirdparty;

import static com.box.l10n.mojito.entity.Screenshot.Status.ACCEPTED;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;
import static org.assertj.core.groups.Tuple.tuple;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.box.l10n.mojito.LocaleMappingHelper;
import com.box.l10n.mojito.entity.Asset;
import com.box.l10n.mojito.entity.AssetContent;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.Screenshot;
import com.box.l10n.mojito.entity.ScreenshotRun;
import com.box.l10n.mojito.entity.TMTextUnit;
import com.box.l10n.mojito.entity.ThirdPartyScreenshot;
import com.box.l10n.mojito.okapi.TextUnitUtils;
import com.box.l10n.mojito.okapi.asset.UnsupportedAssetFilterTypeException;
import com.box.l10n.mojito.rest.thirdparty.ThirdPartySync;
import com.box.l10n.mojito.rest.thirdparty.ThirdPartySyncAction;
import com.box.l10n.mojito.service.asset.AssetRepository;
import com.box.l10n.mojito.service.asset.AssetService;
import com.box.l10n.mojito.service.assetExtraction.AssetExtractionService;
import com.box.l10n.mojito.service.assetExtraction.ServiceTestBase;
import com.box.l10n.mojito.service.assetcontent.AssetContentService;
import com.box.l10n.mojito.service.image.ImageService;
import com.box.l10n.mojito.service.pollableTask.PollableTaskService;
import com.box.l10n.mojito.service.repository.RepositoryNameAlreadyUsedException;
import com.box.l10n.mojito.service.repository.RepositoryService;
import com.box.l10n.mojito.service.screenshot.ScreenshotRepository;
import com.box.l10n.mojito.service.screenshot.ScreenshotService;
import com.box.l10n.mojito.service.tm.TMTextUnitRepository;
import com.box.l10n.mojito.test.TestIdWatcher;
import com.google.common.io.ByteStreams;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;

public class ThirdPartyServiceTest extends ServiceTestBase {

  static Logger logger = LoggerFactory.getLogger(ThirdPartyServiceTest.class);

  @Rule public TestIdWatcher testIdWatcher = new TestIdWatcher();

  @Autowired ThirdPartyService thirdPartyService;

  @Autowired RepositoryService repositoryService;

  @Autowired AssetService assetService;

  @Autowired AssetContentService assetContentService;

  @Autowired AssetExtractionService assetExtractionService;

  @Autowired AssetRepository assetRepository;

  @Autowired PollableTaskService pollableTaskService;

  @Autowired ScreenshotService screenshotService;

  @Autowired TMTextUnitRepository tmTextUnitRepository;

  @Autowired ScreenshotRepository screenshotRepository;

  @Autowired ThirdPartyScreenshotRepository thirdPartyScreenshotRepository;

  @Autowired ThirdPartyTextUnitRepository thirdPartyTextUnitRepository;

  @Autowired ImageService imageService;

  @Autowired LocaleMappingHelper localeMappingHelper;

  @Mock ThirdPartyTMS thirdPartyTMSMock;

  @Captor ArgumentCaptor<List<ThirdPartyImageToTextUnit>> thirdPartyImageToTextUnitsArgumentCaptor;

  @Captor ArgumentCaptor<List<String>> optionsArgumentCaptor;

  @Captor ArgumentCaptor<Map<String, String>> localeMappingArgumentCaptor;

  @Before
  public void before() {
    MockitoAnnotations.initMocks(this);
    initThirdPartyTMSMock();
  }

  void initThirdPartyTMSMock() {
    thirdPartyService.thirdPartyTMS = thirdPartyTMSMock;

    logger.debug(
        "When uploading an image in the ThirdPartyTMS just return an ThirdPartyTMSImage with random id");
    doAnswer(
            (invocation) -> {
              ThirdPartyTMSImage thirdPartyTMSImage = new ThirdPartyTMSImage();
              thirdPartyTMSImage.setId("img-" + invocation.getArgument(1));
              return thirdPartyTMSImage;
            })
        .when(thirdPartyTMSMock)
        .uploadImage(any(), any(), any());

    doThrow(new RuntimeException("test must stub this"))
        .when(thirdPartyTMSMock)
        .getThirdPartyTextUnits(any(), any(), any());
    doThrow(new RuntimeException("test must stub this"))
        .when(thirdPartyTMSMock)
        .createImageToTextUnitMappings(any(), any());
  }

  @Test
  public void mapMojitoAndThirdPartyTextUnits() throws InterruptedException, ExecutionException {

    ThirdPartyServiceTestData thirdPartyServiceTestData =
        new ThirdPartyServiceTestData(testIdWatcher);
    Repository repository = thirdPartyServiceTestData.repository;
    Asset asset = thirdPartyServiceTestData.asset;
    String projectId = "someProjectIdForTest";

    logger.debug("Create mocks and data for tests");
    doAnswer(
            invocation ->
                Arrays.asList(
                    createThirdPartyTextUnit(asset.getPath(), "3rd-hello", "hello"),
                    createThirdPartyTextUnit(asset.getPath(), "3rd-bye", "bye"),
                    createThirdPartyTextUnit(
                        asset.getPath(), "3rd-plural_things", "plural_things", true)))
        .when(thirdPartyTMSMock)
        .getThirdPartyTextUnits(any(), any(), any());

    doNothing().when(thirdPartyTMSMock).createImageToTextUnitMappings(any(), any());

    logger.debug("Invoke function to test");

    ThirdPartySync thirdPartySync = new ThirdPartySync();
    thirdPartySync.setRepositoryId(repository.getId());
    thirdPartySync.setProjectId(projectId);
    thirdPartySync.setActions(
        Arrays.asList(ThirdPartySyncAction.MAP_TEXTUNIT, ThirdPartySyncAction.PUSH_SCREENSHOT));
    thirdPartySync.setPluralSeparator("_");
    thirdPartySync.setOptions(new ArrayList<>());

    thirdPartyService.asyncSyncMojitoWithThirdPartyTMS(thirdPartySync).get();

    logger.debug("Verify states");
    List<com.box.l10n.mojito.entity.ThirdPartyTextUnit> thirdPartyTextUnits =
        thirdPartyTextUnitRepository.findAll().stream()
            .filter(
                thirdPartyTextUnit -> thirdPartyTextUnit.getAsset().getId().equals(asset.getId()))
            .peek(
                t ->
                    logger.debug(
                        "id:{}, asset: {}, ttuid: {}, ttuname:{}, tpid:{}",
                        t.getId(),
                        t.getAsset().getPath(),
                        t.getTmTextUnit().getId(),
                        t.getTmTextUnit().getName(),
                        t.getThirdPartyId()))
            .collect(toList());

    assertThat(thirdPartyTextUnits)
        .as("Should have mappping for the normal and plural text units")
        .extracting(
            t -> t.getAsset().getId(),
            com.box.l10n.mojito.entity.ThirdPartyTextUnit::getThirdPartyId,
            t -> t.getTmTextUnit().getId())
        .containsExactly(
            tuple(asset.getId(), "3rd-hello", thirdPartyServiceTestData.tmTextUnitHello.getId()),
            tuple(asset.getId(), "3rd-bye", thirdPartyServiceTestData.tmTextUnitBye.getId()),
            tuple(
                asset.getId(),
                "3rd-plural_things",
                thirdPartyServiceTestData.tmTextUnitPluralThingsZero.getId()),
            tuple(
                asset.getId(),
                "3rd-plural_things",
                thirdPartyServiceTestData.tmTextUnitPluralThingsOne.getId()),
            tuple(
                asset.getId(),
                "3rd-plural_things",
                thirdPartyServiceTestData.tmTextUnitPluralThingsTwo.getId()),
            tuple(
                asset.getId(),
                "3rd-plural_things",
                thirdPartyServiceTestData.tmTextUnitPluralThingsFew.getId()),
            tuple(
                asset.getId(),
                "3rd-plural_things",
                thirdPartyServiceTestData.tmTextUnitPluralThingsMany.getId()),
            tuple(
                asset.getId(),
                "3rd-plural_things",
                thirdPartyServiceTestData.tmTextUnitPluralThingsOther.getId()));

    logger.debug("Verify behavior");
    verify(thirdPartyTMSMock, times(3))
        .createImageToTextUnitMappings(
            eq(projectId), thirdPartyImageToTextUnitsArgumentCaptor.capture());

    List<List<ThirdPartyImageToTextUnit>> allThirdPartyImageToTextUnits =
        thirdPartyImageToTextUnitsArgumentCaptor.getAllValues();
    List<ThirdPartyImageToTextUnit> thirdPartyImageToTextUnits =
        allThirdPartyImageToTextUnits.get(0);
    assertEquals(2L, thirdPartyImageToTextUnits.size());
    assertEquals("img-image1a.png", thirdPartyImageToTextUnits.get(0).getImageId());
    assertEquals("3rd-bye", thirdPartyImageToTextUnits.get(0).getTextUnitId());
    assertEquals("img-image1a.png", thirdPartyImageToTextUnits.get(1).getImageId());
    assertEquals("3rd-hello", thirdPartyImageToTextUnits.get(1).getTextUnitId());

    thirdPartyImageToTextUnits = allThirdPartyImageToTextUnits.get(1);
    assertEquals(1L, thirdPartyImageToTextUnits.size());
    assertEquals("img-image2a.png", thirdPartyImageToTextUnits.get(0).getImageId());
    assertEquals("3rd-hello", thirdPartyImageToTextUnits.get(0).getTextUnitId());

    thirdPartyImageToTextUnits = allThirdPartyImageToTextUnits.get(2);
    assertEquals(2L, thirdPartyImageToTextUnits.size());
    assertEquals("img-image3a.png", thirdPartyImageToTextUnits.get(0).getImageId());
    assertEquals("3rd-plural_things", thirdPartyImageToTextUnits.get(0).getTextUnitId());
    assertEquals("img-image3a.png", thirdPartyImageToTextUnits.get(1).getImageId());
    assertEquals("3rd-plural_things", thirdPartyImageToTextUnits.get(1).getTextUnitId());
  }

  @Test
  public void updateTextUnitComment()
      throws InterruptedException, ExecutionException, UnsupportedAssetFilterTypeException {

    ThirdPartyServiceTestData thirdPartyServiceTestData =
        new ThirdPartyServiceTestData(testIdWatcher);
    Repository repository = thirdPartyServiceTestData.repository;
    Asset asset = thirdPartyServiceTestData.asset;
    String projectId = "someProjectIdForTest";

    logger.debug("Just update a comment");
    String assetContent =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<resources>\n"
            + "    <!--comment 1 updated-->\n"
            + "    <string name=\"hello\">Hello</string>\n"
            + "    <!--comment 2-->\n"
            + "    <string name=\"bye\">Bye</string>\n"
            + "    <plurals name=\"plural_things\">\n"
            + "        <item quantity=\"one\">One thing</item>\n"
            + "        <item quantity=\"other\">Multiple things</item>\n"
            + "    </plurals>"
            + "</resources>";

    AssetContent assetContentEntity = assetContentService.createAssetContent(asset, assetContent);
    assetExtractionService
        .processAssetAsync(assetContentEntity.getId(), null, null, null, null)
        .get();

    TMTextUnit tmTextUnitHelloCommentUpdated =
        tmTextUnitRepository.findFirstByTmAndMd5(
            asset.getRepository().getTm(),
            new TextUnitUtils().computeTextUnitMD5("hello", "Hello", "comment 1 updated"));

    logger.debug("Create mocks and data for tests");
    doAnswer(
            invocation ->
                Arrays.asList(
                    createThirdPartyTextUnit(asset.getPath(), "3rd-hello", "hello"),
                    createThirdPartyTextUnit(asset.getPath(), "3rd-bye", "bye"),
                    createThirdPartyTextUnit(
                        asset.getPath(), "3rd-plural_things", "plural_things", true)))
        .when(thirdPartyTMSMock)
        .getThirdPartyTextUnits(any(), any(), any());

    doNothing().when(thirdPartyTMSMock).createImageToTextUnitMappings(any(), any());

    logger.debug("Invoke function to test");

    ThirdPartySync thirdPartySync = new ThirdPartySync();
    thirdPartySync.setRepositoryId(repository.getId());
    thirdPartySync.setProjectId(projectId);
    thirdPartySync.setActions(Arrays.asList(ThirdPartySyncAction.MAP_TEXTUNIT));
    thirdPartySync.setPluralSeparator("_");
    thirdPartySync.setOptions(new ArrayList<>());

    thirdPartyService.asyncSyncMojitoWithThirdPartyTMS(thirdPartySync).get();

    logger.debug("Verify states");
    List<com.box.l10n.mojito.entity.ThirdPartyTextUnit> thirdPartyTextUnits =
        thirdPartyTextUnitRepository.findAll().stream()
            .filter(
                thirdPartyTextUnit -> thirdPartyTextUnit.getAsset().getId().equals(asset.getId()))
            .peek(
                t -> {
                  logger.debug(
                      "id:{}, asset: {}, ttuid: {}, ttuname:{}, tpid:{}",
                      t.getId(),
                      t.getAsset().getPath(),
                      t.getTmTextUnit().getId(),
                      t.getTmTextUnit().getName(),
                      t.getThirdPartyId());
                })
            .collect(toList());

    assertThat(thirdPartyTextUnits)
        .as("both the used and unsed text units with the same name must be mapped")
        .extracting(
            t -> t.getAsset().getId(),
            com.box.l10n.mojito.entity.ThirdPartyTextUnit::getThirdPartyId,
            t -> t.getTmTextUnit().getId())
        .containsExactly(
            tuple(asset.getId(), "3rd-hello", tmTextUnitHelloCommentUpdated.getId()),
            tuple(asset.getId(), "3rd-hello", thirdPartyServiceTestData.tmTextUnitHello.getId()),
            tuple(asset.getId(), "3rd-bye", thirdPartyServiceTestData.tmTextUnitBye.getId()),
            tuple(
                asset.getId(),
                "3rd-plural_things",
                thirdPartyServiceTestData.tmTextUnitPluralThingsZero.getId()),
            tuple(
                asset.getId(),
                "3rd-plural_things",
                thirdPartyServiceTestData.tmTextUnitPluralThingsOne.getId()),
            tuple(
                asset.getId(),
                "3rd-plural_things",
                thirdPartyServiceTestData.tmTextUnitPluralThingsTwo.getId()),
            tuple(
                asset.getId(),
                "3rd-plural_things",
                thirdPartyServiceTestData.tmTextUnitPluralThingsFew.getId()),
            tuple(
                asset.getId(),
                "3rd-plural_things",
                thirdPartyServiceTestData.tmTextUnitPluralThingsMany.getId()),
            tuple(
                asset.getId(),
                "3rd-plural_things",
                thirdPartyServiceTestData.tmTextUnitPluralThingsOther.getId()));
  }

  @Test
  public void duplicatedNamesSubSequentMapping() throws ExecutionException, InterruptedException {
    ThirdPartyServiceTestData thirdPartyServiceTestData =
        new ThirdPartyServiceTestData(testIdWatcher);
    Repository repository = thirdPartyServiceTestData.repository;
    Asset asset = thirdPartyServiceTestData.asset;
    String projectId = "someProjectIdForTest";

    logger.debug("Create mocks and data for tests");
    doAnswer(
            invocation ->
                Arrays.asList(createThirdPartyTextUnit(asset.getPath(), "3rd-hello", "hello")))
        .doAnswer(
            invocation ->
                Arrays.asList(
                    createThirdPartyTextUnit(asset.getPath(), "3rd-hello-duplicate", "hello")))
        .when(thirdPartyTMSMock)
        .getThirdPartyTextUnits(any(), any(), any());

    doNothing().when(thirdPartyTMSMock).createImageToTextUnitMappings(any(), any());

    logger.debug("Invoke function to test");

    ThirdPartySync thirdPartySync = new ThirdPartySync();
    thirdPartySync.setRepositoryId(repository.getId());
    thirdPartySync.setProjectId(projectId);
    thirdPartySync.setActions(Arrays.asList(ThirdPartySyncAction.MAP_TEXTUNIT));
    thirdPartySync.setPluralSeparator(" _");
    thirdPartySync.setOptions(new ArrayList<>());

    thirdPartyService.asyncSyncMojitoWithThirdPartyTMS(thirdPartySync).get();

    logger.debug("Verify states");
    thirdPartyTextUnitRepository.findAll().stream()
        .filter(thirdPartyTextUnit -> thirdPartyTextUnit.getAsset().getId().equals(asset.getId()))
        .forEach(
            t -> {
              logger.debug(
                  "id:{}, asset: {}, ttuid: {}, ttuname:{}, tpid:{}",
                  t.getId(),
                  t.getAsset().getPath(),
                  t.getTmTextUnit().getId(),
                  t.getTmTextUnit().getName(),
                  t.getThirdPartyId());
            });

    List<com.box.l10n.mojito.entity.ThirdPartyTextUnit> thirdPartyTextUnits =
        thirdPartyTextUnitRepository.findAll().stream()
            .filter(
                thirdPartyTextUnit -> thirdPartyTextUnit.getAsset().getId().equals(asset.getId()))
            .collect(toList());
    assertEquals(1, thirdPartyTextUnits.size());
    thirdPartyTextUnits.forEach(t -> assertEquals(asset.getId(), t.getAsset().getId()));

    assertEquals(
        thirdPartyServiceTestData.tmTextUnitHello.getId(),
        thirdPartyTextUnits.get(0).getTmTextUnit().getId());
    assertEquals("3rd-hello", thirdPartyTextUnits.get(0).getThirdPartyId());

    logger.debug("Invoke function to test - duplicate name");
    thirdPartySync = new ThirdPartySync();
    thirdPartySync.setRepositoryId(repository.getId());
    thirdPartySync.setProjectId(projectId);
    thirdPartySync.setActions(Arrays.asList(ThirdPartySyncAction.MAP_TEXTUNIT));
    thirdPartySync.setPluralSeparator(" _");
    thirdPartySync.setOptions(new ArrayList<>());

    logger.debug("Verify states - duplicate name");
    thirdPartyTextUnits =
        thirdPartyTextUnitRepository.findAll().stream()
            .filter(
                thirdPartyTextUnit -> thirdPartyTextUnit.getAsset().getId().equals(asset.getId()))
            .collect(toList());
    assertEquals(1, thirdPartyTextUnits.size());
    thirdPartyTextUnits.forEach(t -> assertEquals(asset.getId(), t.getAsset().getId()));

    assertEquals(
        thirdPartyServiceTestData.tmTextUnitHello.getId(),
        thirdPartyTextUnits.get(0).getTmTextUnit().getId());
    assertEquals("3rd-hello", thirdPartyTextUnits.get(0).getThirdPartyId());
  }

  @Test
  public void findUnmappedScreenshots() throws RepositoryNameAlreadyUsedException, IOException {
    Repository repository =
        repositoryService.createRepository(testIdWatcher.getEntityName("repository"));

    byte[] content =
        ByteStreams.toByteArray(
            new ClassPathResource("/com/box/l10n/mojito/img/1.png").getInputStream());
    imageService.uploadImage("image1.png", content);

    ScreenshotRun screenshotRun = repository.getManualScreenshotRun();
    Screenshot screen1 = new Screenshot();
    screen1.setName("screen1");
    screen1.setSrc("api/images/image1.png");
    screen1.setStatus(ACCEPTED);

    Screenshot screen2 = new Screenshot();
    screen2.setName("screen2");
    screen1.setSrc("api/images/image1.png");
    screen2.setStatus(ACCEPTED);

    Screenshot screen3 = new Screenshot();
    screen3.setName("screen3");
    screen1.setSrc("api/images/image1.png");
    screen3.setStatus(Screenshot.Status.ACCEPTED);

    screenshotRun.getScreenshots().add(screen1);
    screenshotRun.getScreenshots().add(screen2);
    screenshotRun.getScreenshots().add(screen3);
    screenshotService.createOrAddToScreenshotRun(screenshotRun, false);

    List<Screenshot> unmappedScreenshots = screenshotRepository.findUnmappedScreenshots(repository);
    assertEquals(3L, unmappedScreenshots.size());

    unmappedScreenshots.stream()
        .limit(2)
        .forEach(
            screenshot -> {
              ThirdPartyScreenshot thirdPartyScreenshot = new ThirdPartyScreenshot();
              thirdPartyScreenshot.setScreenshot(screenshot);
              thirdPartyScreenshot.setThirdPartyId("thirdparty-" + screenshot.getName());

              thirdPartyScreenshotRepository.save(thirdPartyScreenshot);
            });

    List<Screenshot> unmappedScreenshotsAfterSave =
        screenshotRepository.findUnmappedScreenshots(repository);
    assertEquals(1L, unmappedScreenshotsAfterSave.size());
    Screenshot screenshot =
        screenshotRepository.findUnmappedScreenshots(repository).stream().findFirst().get();
    assertEquals("screen3", screenshot.getName());
  }

  @Test
  public void testPushArguments()
      throws RepositoryNameAlreadyUsedException, ExecutionException, InterruptedException {
    Repository repository =
        repositoryService.createRepository(testIdWatcher.getEntityName("repository"));

    ThirdPartySync thirdPartySync = new ThirdPartySync();
    thirdPartySync.setRepositoryId(repository.getId());
    thirdPartySync.setProjectId("projectId");
    thirdPartySync.setActions(Arrays.asList(ThirdPartySyncAction.PUSH));
    thirdPartySync.setPluralSeparator(" _");
    thirdPartySync.setSkipTextUnitsWithPattern("text_unit_pattern");
    thirdPartySync.setSkipAssetsWithPathPattern("asset_path_pattern");
    thirdPartySync.setOptions(Arrays.asList("option1=value1", "option2=value2"));
    ArgumentCaptor<Repository> repoCaptor = ArgumentCaptor.forClass(Repository.class);

    thirdPartyService.asyncSyncMojitoWithThirdPartyTMS(thirdPartySync).get();

    verify(thirdPartyTMSMock, only())
        .push(
            repoCaptor.capture(),
            eq("projectId"),
            eq(" _"),
            eq("text_unit_pattern"),
            eq("asset_path_pattern"),
            optionsArgumentCaptor.capture());

    assertThat(repoCaptor.getValue().getId()).isEqualTo(repository.getId());
    assertThat(optionsArgumentCaptor.getValue()).contains("option1=value1", "option2=value2");
  }

  @Test
  public void testPushArgumentsWithSpacePlaceholder()
      throws RepositoryNameAlreadyUsedException, ExecutionException, InterruptedException {
    Repository repository =
        repositoryService.createRepository(testIdWatcher.getEntityName("repository"));

    ThirdPartySync thirdPartySync = new ThirdPartySync();
    thirdPartySync.setRepositoryId(repository.getId());
    thirdPartySync.setProjectId("projectId");
    thirdPartySync.setActions(Arrays.asList(ThirdPartySyncAction.PUSH));
    thirdPartySync.setPluralSeparator(" _");
    thirdPartySync.setSkipTextUnitsWithPattern("text_unit_pattern");
    thirdPartySync.setSkipAssetsWithPathPattern("asset_path_pattern");
    thirdPartySync.setOptions(Arrays.asList("option1=value1", "option2=value2"));
    ArgumentCaptor<Repository> repoCaptor = ArgumentCaptor.forClass(Repository.class);

    thirdPartyService.asyncSyncMojitoWithThirdPartyTMS(thirdPartySync).get();

    verify(thirdPartyTMSMock, only())
        .push(
            repoCaptor.capture(),
            eq("projectId"),
            eq(" _"),
            eq("text_unit_pattern"),
            eq("asset_path_pattern"),
            optionsArgumentCaptor.capture());

    assertThat(repoCaptor.getValue().getId()).isEqualTo(repository.getId());
    assertThat(optionsArgumentCaptor.getValue()).contains("option1=value1", "option2=value2");
  }

  @Test
  public void testPushTranslationArguments()
      throws RepositoryNameAlreadyUsedException, ExecutionException, InterruptedException {
    Repository repository =
        repositoryService.createRepository(testIdWatcher.getEntityName("repository"));

    String localeMapping = "ja:ja-JP";
    ThirdPartySync thirdPartySync = new ThirdPartySync();
    thirdPartySync.setRepositoryId(repository.getId());
    thirdPartySync.setProjectId("projectId");
    thirdPartySync.setActions(Arrays.asList(ThirdPartySyncAction.PUSH_TRANSLATION));
    thirdPartySync.setPluralSeparator(" _");
    thirdPartySync.setLocaleMapping(localeMapping);
    thirdPartySync.setSkipTextUnitsWithPattern("text_unit_pattern");
    thirdPartySync.setSkipAssetsWithPathPattern("asset_path_pattern");
    thirdPartySync.setIncludeTextUnitsWithPattern("include_text_unit_pattern");
    thirdPartySync.setOptions(Arrays.asList("option1=value1", "option2=value2"));
    ArgumentCaptor<Repository> repoCaptor = ArgumentCaptor.forClass(Repository.class);

    thirdPartyService.asyncSyncMojitoWithThirdPartyTMS(thirdPartySync).get();

    verify(thirdPartyTMSMock, only())
        .pushTranslations(
            repoCaptor.capture(),
            eq("projectId"),
            eq(" _"),
            localeMappingArgumentCaptor.capture(),
            eq("text_unit_pattern"),
            eq("asset_path_pattern"),
            eq("include_text_unit_pattern"),
            optionsArgumentCaptor.capture());

    assertThat(repoCaptor.getValue().getId()).isEqualTo(repository.getId());
    assertThat(localeMappingArgumentCaptor.getValue()).contains(entry("ja-JP", "ja"));
    assertThat(optionsArgumentCaptor.getValue()).contains("option1=value1", "option2=value2");
  }

  @Test
  public void testPushTranslationArgumentsWithSpacePlaceholder()
      throws RepositoryNameAlreadyUsedException, ExecutionException, InterruptedException {
    Repository repository =
        repositoryService.createRepository(testIdWatcher.getEntityName("repository"));

    String localeMapping = "ja:ja-JP";
    ThirdPartySync thirdPartySync = new ThirdPartySync();
    thirdPartySync.setRepositoryId(repository.getId());
    thirdPartySync.setProjectId("projectId");
    thirdPartySync.setActions(Arrays.asList(ThirdPartySyncAction.PUSH_TRANSLATION));
    thirdPartySync.setPluralSeparator(" _");
    thirdPartySync.setLocaleMapping(localeMapping);
    thirdPartySync.setSkipTextUnitsWithPattern("text_unit_pattern");
    thirdPartySync.setSkipAssetsWithPathPattern("asset_path_pattern");
    thirdPartySync.setIncludeTextUnitsWithPattern("include_text_unit_pattern");
    thirdPartySync.setOptions(Arrays.asList("option1=value1", "option2=value2"));
    ArgumentCaptor<Repository> repoCaptor = ArgumentCaptor.forClass(Repository.class);

    thirdPartyService.asyncSyncMojitoWithThirdPartyTMS(thirdPartySync).get();

    verify(thirdPartyTMSMock, only())
        .pushTranslations(
            repoCaptor.capture(),
            eq("projectId"),
            eq(" _"),
            localeMappingArgumentCaptor.capture(),
            eq("text_unit_pattern"),
            eq("asset_path_pattern"),
            eq("include_text_unit_pattern"),
            optionsArgumentCaptor.capture());

    assertThat(repoCaptor.getValue().getId()).isEqualTo(repository.getId());
    assertThat(localeMappingArgumentCaptor.getValue()).contains(entry("ja-JP", "ja"));
    assertThat(optionsArgumentCaptor.getValue()).contains("option1=value1", "option2=value2");
  }

  @Test
  public void testPullArguments()
      throws RepositoryNameAlreadyUsedException, ExecutionException, InterruptedException {
    Repository repository =
        repositoryService.createRepository(testIdWatcher.getEntityName("repository"));

    String localeMapping = "ja:ja-JP";
    ThirdPartySync thirdPartySync = new ThirdPartySync();
    thirdPartySync.setRepositoryId(repository.getId());
    thirdPartySync.setProjectId("projectId");
    thirdPartySync.setActions(Arrays.asList(ThirdPartySyncAction.PULL));
    thirdPartySync.setPluralSeparator("_");
    thirdPartySync.setLocaleMapping(localeMapping);
    thirdPartySync.setSkipTextUnitsWithPattern("text_unit_pattern");
    thirdPartySync.setSkipAssetsWithPathPattern("asset_path_pattern");
    thirdPartySync.setOptions(Arrays.asList("option1=value1", "option2=value2"));
    ArgumentCaptor<Repository> repoCaptor = ArgumentCaptor.forClass(Repository.class);

    thirdPartyService.asyncSyncMojitoWithThirdPartyTMS(thirdPartySync).get();

    verify(thirdPartyTMSMock, only())
        .pull(
            repoCaptor.capture(),
            eq("projectId"),
            eq("_"),
            localeMappingArgumentCaptor.capture(),
            eq("text_unit_pattern"),
            eq("asset_path_pattern"),
            optionsArgumentCaptor.capture());

    assertThat(repoCaptor.getValue().getId()).isEqualTo(repository.getId());
    assertThat(localeMappingArgumentCaptor.getValue()).contains(entry("ja-JP", "ja"));
    assertThat(optionsArgumentCaptor.getValue()).contains("option1=value1", "option2=value2");
  }

  @Test
  public void testPullArgumentsWithSpacePlaceholder()
      throws RepositoryNameAlreadyUsedException, ExecutionException, InterruptedException {
    Repository repository =
        repositoryService.createRepository(testIdWatcher.getEntityName("repository"));

    String localeMapping = "ja:ja-JP";
    ThirdPartySync thirdPartySync = new ThirdPartySync();
    thirdPartySync.setRepositoryId(repository.getId());
    thirdPartySync.setProjectId("projectId");
    thirdPartySync.setActions(Arrays.asList(ThirdPartySyncAction.PULL));
    thirdPartySync.setPluralSeparator(" _");
    thirdPartySync.setLocaleMapping(localeMapping);
    thirdPartySync.setSkipTextUnitsWithPattern("text_unit_pattern");
    thirdPartySync.setSkipAssetsWithPathPattern("asset_path_pattern");
    thirdPartySync.setOptions(Arrays.asList("option1=value1", "option2=value2"));
    ArgumentCaptor<Repository> repoCaptor = ArgumentCaptor.forClass(Repository.class);

    thirdPartyService.asyncSyncMojitoWithThirdPartyTMS(thirdPartySync).get();

    verify(thirdPartyTMSMock, only())
        .pull(
            repoCaptor.capture(),
            eq("projectId"),
            eq(" _"),
            localeMappingArgumentCaptor.capture(),
            eq("text_unit_pattern"),
            eq("asset_path_pattern"),
            optionsArgumentCaptor.capture());

    assertThat(repoCaptor.getValue().getId()).isEqualTo(repository.getId());
    assertThat(localeMappingArgumentCaptor.getValue()).contains(entry("ja-JP", "ja"));
    assertThat(optionsArgumentCaptor.getValue()).contains("option1=value1", "option2=value2");
  }

  @Test
  public void testParseLocaleMapping() {
    assertThat(thirdPartyService.parseLocaleMapping(null)).isEmpty();
    assertThat(thirdPartyService.parseLocaleMapping("")).isEmpty();
    assertThat(thirdPartyService.parseLocaleMapping("es:es-MX"))
        .containsKey("es-MX")
        .containsValue("es");
    assertThat(thirdPartyService.parseLocaleMapping("es:es-MX,fr:fr-FR"))
        .containsKeys("es-MX", "fr-FR")
        .containsValues("es", "fr");
    assertThatThrownBy(() -> thirdPartyService.parseLocaleMapping("unparseable"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  ThirdPartyTextUnit createThirdPartyTextUnit(String assetPath, String id, String name) {
    return createThirdPartyTextUnit(assetPath, id, name, false);
  }

  ThirdPartyTextUnit createThirdPartyTextUnit(
      String assetPath, String id, String name, boolean isNamePluralPrefix) {
    ThirdPartyTextUnit thirdPartyTextUnit = new ThirdPartyTextUnit();
    thirdPartyTextUnit.setAssetPath(assetPath);
    thirdPartyTextUnit.setId(id);
    thirdPartyTextUnit.setName(name);
    thirdPartyTextUnit.setNamePluralPrefix(isNamePluralPrefix);
    return thirdPartyTextUnit;
  }
}

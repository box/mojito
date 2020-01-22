package com.box.l10n.mojito.service.thirdparty;

import com.box.l10n.mojito.entity.Asset;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.Screenshot;
import com.box.l10n.mojito.entity.ScreenshotRun;
import com.box.l10n.mojito.entity.ThirdPartyScreenshot;
import com.box.l10n.mojito.service.asset.AssetRepository;
import com.box.l10n.mojito.service.asset.AssetService;
import com.box.l10n.mojito.service.assetExtraction.AssetExtractionService;
import com.box.l10n.mojito.service.assetExtraction.ServiceTestBase;
import com.box.l10n.mojito.okapi.asset.UnsupportedAssetFilterTypeException;
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static com.box.l10n.mojito.entity.Screenshot.Status.ACCEPTED;
import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class ThirdPartyServiceTest extends ServiceTestBase {

    static Logger logger = LoggerFactory.getLogger(ThirdPartyServiceTest.class);

    @Rule
    public TestIdWatcher testIdWatcher = new TestIdWatcher();

    @Autowired
    ThirdPartyService thirdPartyService;

    @Autowired
    RepositoryService repositoryService;

    @Autowired
    AssetService assetService;

    @Autowired
    AssetContentService assetContentService;

    @Autowired
    AssetExtractionService assetExtractionService;

    @Autowired
    AssetRepository assetRepository;

    @Autowired
    PollableTaskService pollableTaskService;

    @Autowired
    ScreenshotService screenshotService;

    @Autowired
    TMTextUnitRepository tmTextUnitRepository;

    @Autowired
    ScreenshotRepository screenshotRepository;

    @Autowired
    ThirdPartyScreenshotRepository thirdPartyScreenshotRepository;

    @Autowired
    ThirdPartyTextUnitRepository thirdPartyTextUnitRepository;

    @Autowired
    ImageService imageService;

    @Mock
    ThirdPartyTMS thirdPartyTMSMock;

    @Captor
    ArgumentCaptor<List<ThirdPartyImageToTextUnit>> thirdPartyImageToTextUnitsArgumentCaptor;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
        initThirdPartyTMSMock();
    }

    void initThirdPartyTMSMock() {
        thirdPartyService.thirdPartyTMS = thirdPartyTMSMock;

        logger.debug("When uploading an image in the ThirdPartyTMS just return an ThirdPartyTMSImage with random id");
        doAnswer((invocation) -> {
            ThirdPartyTMSImage thirdPartyTMSImage = new ThirdPartyTMSImage();
            thirdPartyTMSImage.setId("img-" + invocation.getArgumentAt(1, String.class));
            return thirdPartyTMSImage;
        }).when(thirdPartyTMSMock).uploadImage(any(), any(), any());

        doThrow(new RuntimeException("test must stub this")).when(thirdPartyTMSMock).getThirdPartyTextUnits(any(), any());
        doThrow(new RuntimeException("test must stub this")).when(thirdPartyTMSMock).createImageToTextUnitMappings(any(), any());
    }

    @Test
    public void mapMojitoAndThirdPartyTextUnits() throws RepositoryNameAlreadyUsedException, InterruptedException, ExecutionException, UnsupportedAssetFilterTypeException, IOException {

        ThirdPartyServiceTestData thirdPartyServiceTestData = new ThirdPartyServiceTestData(testIdWatcher);
        Repository repository = thirdPartyServiceTestData.repository;
        Asset asset = thirdPartyServiceTestData.asset;
        String projectId = "someProjectIdForTest";

        logger.debug("Create mocks and data for tests");
        doAnswer(invocation -> Arrays.asList(
                createThirdPartyTextUnit(asset.getPath(), "3rd-hello", "hello"),
                createThirdPartyTextUnit(asset.getPath(), "3rd-bye", "bye"),
                createThirdPartyTextUnit(asset.getPath(), "3rd-plural_things", "plural_things", true)
        )).when(thirdPartyTMSMock).getThirdPartyTextUnits(any(), any());

        doNothing().when(thirdPartyTMSMock).createImageToTextUnitMappings(any(), any());

        logger.debug("Invoke function to test");
        thirdPartyService.asyncSyncMojitoWithThirdPartyTMS(repository.getId(), projectId,
                Arrays.asList(ThirdPartyService.Action.MAP_TEXTUNIT, ThirdPartyService.Action.PUSH_SCREENSHOT),
                " _", null, new ArrayList<>()).get();

        logger.debug("Verify states");
        thirdPartyTextUnitRepository.findAll().stream()
                .filter(thirdPartyTextUnit -> thirdPartyTextUnit.getAsset().getId().equals(asset.getId()))
                .forEach(t -> {
                    logger.debug("id:{}, asset: {}, ttuid: {}, ttuname:{}, tpid:{}",
                            t.getId(), t.getAsset().getPath(), t.getTmTextUnit().getId(),
                            t.getTmTextUnit().getName(), t.getThirdPartyId());
                });

        List<com.box.l10n.mojito.entity.ThirdPartyTextUnit> thirdPartyTextUnits = thirdPartyTextUnitRepository.findAll().stream()
                .filter(thirdPartyTextUnit -> thirdPartyTextUnit.getAsset().getId().equals(asset.getId()))
                .collect(toList());
        assertEquals(8, thirdPartyTextUnits.size());
        thirdPartyTextUnits.forEach(t -> assertEquals(asset.getId(), t.getAsset().getId()));

        assertEquals(thirdPartyServiceTestData.tmTextUnitHello.getId(), thirdPartyTextUnits.get(0).getTmTextUnit().getId());
        assertEquals("3rd-hello", thirdPartyTextUnits.get(0).getThirdPartyId());

        assertEquals(thirdPartyServiceTestData.tmTextUnitBye.getId(), thirdPartyTextUnits.get(1).getTmTextUnit().getId());
        assertEquals("3rd-bye", thirdPartyTextUnits.get(1).getThirdPartyId());

        assertEquals(thirdPartyServiceTestData.tmTextUnitPluralThingsZero.getId(), thirdPartyTextUnits.get(2).getTmTextUnit().getId());
        assertEquals("3rd-plural_things", thirdPartyTextUnits.get(2).getThirdPartyId());

        assertEquals(thirdPartyServiceTestData.tmTextUnitPluralThingsOne.getId(), thirdPartyTextUnits.get(3).getTmTextUnit().getId());
        assertEquals("3rd-plural_things", thirdPartyTextUnits.get(3).getThirdPartyId());

        assertEquals(thirdPartyServiceTestData.tmTextUnitPluralThingsTwo.getId(), thirdPartyTextUnits.get(4).getTmTextUnit().getId());
        assertEquals("3rd-plural_things", thirdPartyTextUnits.get(4).getThirdPartyId());

        assertEquals(thirdPartyServiceTestData.tmTextUnitPluralThingsFew.getId(), thirdPartyTextUnits.get(5).getTmTextUnit().getId());
        assertEquals("3rd-plural_things", thirdPartyTextUnits.get(5).getThirdPartyId());

        assertEquals(thirdPartyServiceTestData.tmTextUnitPluralThingsMany.getId(), thirdPartyTextUnits.get(6).getTmTextUnit().getId());
        assertEquals("3rd-plural_things", thirdPartyTextUnits.get(6).getThirdPartyId());

        assertEquals(thirdPartyServiceTestData.tmTextUnitPluralThingsOther.getId(), thirdPartyTextUnits.get(7).getTmTextUnit().getId());
        assertEquals("3rd-plural_things", thirdPartyTextUnits.get(7).getThirdPartyId());

        logger.debug("Verify behavior");
        verify(thirdPartyTMSMock, times(3)).createImageToTextUnitMappings(
                eq(projectId),
                thirdPartyImageToTextUnitsArgumentCaptor.capture()
        );

        List<List<ThirdPartyImageToTextUnit>> allThirdPartyImageToTextUnits = thirdPartyImageToTextUnitsArgumentCaptor.getAllValues();
        List<ThirdPartyImageToTextUnit> thirdPartyImageToTextUnits = allThirdPartyImageToTextUnits.get(0);
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
    public void duplicatedNamesSubSequentMapping() throws ExecutionException, InterruptedException {
        ThirdPartyServiceTestData thirdPartyServiceTestData = new ThirdPartyServiceTestData(testIdWatcher);
        Repository repository = thirdPartyServiceTestData.repository;
        Asset asset = thirdPartyServiceTestData.asset;
        String projectId = "someProjectIdForTest";

        logger.debug("Create mocks and data for tests");
        doAnswer(invocation -> Arrays.asList(
                createThirdPartyTextUnit(asset.getPath(), "3rd-hello", "hello")
        )).doAnswer(invocation -> Arrays.asList(
                createThirdPartyTextUnit(asset.getPath(), "3rd-hello-duplicate", "hello")
        )).when(thirdPartyTMSMock).getThirdPartyTextUnits(any(), any());

        doNothing().when(thirdPartyTMSMock).createImageToTextUnitMappings(any(), any());

        logger.debug("Invoke function to test");
        thirdPartyService.asyncSyncMojitoWithThirdPartyTMS(repository.getId(), projectId,
                Arrays.asList(ThirdPartyService.Action.MAP_TEXTUNIT),
                " _", null, new ArrayList<>()).get();

        logger.debug("Verify states");
        thirdPartyTextUnitRepository.findAll().stream()
                .filter(thirdPartyTextUnit -> thirdPartyTextUnit.getAsset().getId().equals(asset.getId()))
                .forEach(t -> {
                    logger.debug("id:{}, asset: {}, ttuid: {}, ttuname:{}, tpid:{}",
                            t.getId(), t.getAsset().getPath(), t.getTmTextUnit().getId(),
                            t.getTmTextUnit().getName(), t.getThirdPartyId());
                });

        List<com.box.l10n.mojito.entity.ThirdPartyTextUnit> thirdPartyTextUnits = thirdPartyTextUnitRepository.findAll().stream()
                .filter(thirdPartyTextUnit -> thirdPartyTextUnit.getAsset().getId().equals(asset.getId()))
                .collect(toList());
        assertEquals(1, thirdPartyTextUnits.size());
        thirdPartyTextUnits.forEach(t -> assertEquals(asset.getId(), t.getAsset().getId()));

        assertEquals(thirdPartyServiceTestData.tmTextUnitHello.getId(), thirdPartyTextUnits.get(0).getTmTextUnit().getId());
        assertEquals("3rd-hello", thirdPartyTextUnits.get(0).getThirdPartyId());

        logger.debug("Invoke function to test - duplicate name");
        thirdPartyService.asyncSyncMojitoWithThirdPartyTMS(repository.getId(), projectId,
                Arrays.asList(ThirdPartyService.Action.MAP_TEXTUNIT),
                " _", null, new ArrayList<>()).get();

        logger.debug("Verify states - duplicate name");
        thirdPartyTextUnits = thirdPartyTextUnitRepository.findAll().stream()
                .filter(thirdPartyTextUnit -> thirdPartyTextUnit.getAsset().getId().equals(asset.getId()))
                .collect(toList());
        assertEquals(1, thirdPartyTextUnits.size());
        thirdPartyTextUnits.forEach(t -> assertEquals(asset.getId(), t.getAsset().getId()));

        assertEquals(thirdPartyServiceTestData.tmTextUnitHello.getId(), thirdPartyTextUnits.get(0).getTmTextUnit().getId());
        assertEquals("3rd-hello", thirdPartyTextUnits.get(0).getThirdPartyId());
    }

    @Test
    public void findUnmappedScreenshots() throws RepositoryNameAlreadyUsedException, IOException {
        Repository repository = repositoryService.createRepository(testIdWatcher.getEntityName("repository"));

        byte[] content = ByteStreams.toByteArray(new ClassPathResource("/com/box/l10n/mojito/img/1.png").getInputStream());
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
        screenshotService.createOrUpdateScreenshotRun(screenshotRun, false);

        List<Screenshot> unmappedScreenshots = screenshotRepository.findUnmappedScreenshots(repository);
        assertEquals(3L, unmappedScreenshots.size());

        unmappedScreenshots.stream().limit(2).forEach(screenshot -> {
            ThirdPartyScreenshot thirdPartyScreenshot = new ThirdPartyScreenshot();
            thirdPartyScreenshot.setScreenshot(screenshot);
            thirdPartyScreenshot.setThirdPartyId("thirdparty-" + screenshot.getName());

            thirdPartyScreenshotRepository.save(thirdPartyScreenshot);
        });

        List<Screenshot> unmappedScreenshotsAfterSave = screenshotRepository.findUnmappedScreenshots(repository);
        assertEquals(1L, unmappedScreenshotsAfterSave.size());
        Screenshot screenshot = screenshotRepository.findUnmappedScreenshots(repository).stream().findFirst().get();
        assertEquals("screen3", screenshot.getName());
    }

    ThirdPartyTextUnit createThirdPartyTextUnit(String assetPath, String id, String name) {
        return createThirdPartyTextUnit(assetPath, id, name, false);
    }

    ThirdPartyTextUnit createThirdPartyTextUnit(String assetPath, String id, String name, boolean isNamePluralPrefix) {
        ThirdPartyTextUnit thirdPartyTextUnit = new ThirdPartyTextUnit();
        thirdPartyTextUnit.setAssetPath(assetPath);
        thirdPartyTextUnit.setId(id);
        thirdPartyTextUnit.setName(name);
        thirdPartyTextUnit.setNamePluralPrefix(isNamePluralPrefix);
        return thirdPartyTextUnit;
    }

}
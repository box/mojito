package com.box.l10n.mojito.service.thirdparty;

import com.box.l10n.mojito.entity.Asset;
import com.box.l10n.mojito.entity.AssetContent;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.Screenshot;
import com.box.l10n.mojito.entity.ScreenshotRun;
import com.box.l10n.mojito.entity.ScreenshotTextUnit;
import com.box.l10n.mojito.entity.TMTextUnit;
import com.box.l10n.mojito.service.asset.AssetService;
import com.box.l10n.mojito.service.assetExtraction.AssetExtractionService;
import com.box.l10n.mojito.service.assetcontent.AssetContentService;
import com.box.l10n.mojito.service.image.ImageService;
import com.box.l10n.mojito.service.repository.RepositoryService;
import com.box.l10n.mojito.service.screenshot.ScreenshotService;
import com.box.l10n.mojito.service.tm.TMTextUnitRepository;
import com.box.l10n.mojito.test.TestIdWatcher;
import com.google.common.io.ByteStreams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.core.io.ClassPathResource;

import javax.annotation.PostConstruct;
import java.util.List;

import static com.box.l10n.mojito.entity.Screenshot.Status.ACCEPTED;
import static org.junit.Assert.assertEquals;

@Configurable
public class ThirdPartyServiceTestData {

    @Autowired
    RepositoryService repositoryService;

    @Autowired
    AssetService assetService;

    @Autowired
    AssetContentService assetContentService;

    @Autowired
    AssetExtractionService assetExtractionService;

    @Autowired
    TMTextUnitRepository tmTextUnitRepository;

    @Autowired
    ImageService imageService;

    @Autowired
    ScreenshotService screenshotService;

    TestIdWatcher testIdWatcher;

    Repository repository;
    Asset asset;
    TMTextUnit tmTextUnitHello;
    TMTextUnit tmTextUnitBye;

    TMTextUnit tmTextUnitPluralThingsZero;
    TMTextUnit tmTextUnitPluralThingsOne;
    TMTextUnit tmTextUnitPluralThingsTwo;
    TMTextUnit tmTextUnitPluralThingsFew;
    TMTextUnit tmTextUnitPluralThingsMany;
    TMTextUnit tmTextUnitPluralThingsOther;

    public ThirdPartyServiceTestData(TestIdWatcher testIdWatcher) {
        this.testIdWatcher = testIdWatcher;
    }

    @PostConstruct
    public ThirdPartyServiceTestData init() throws Exception {
        repository = repositoryService.createRepository(testIdWatcher.getEntityName("repository"));

        String assetContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<resources>\n" +
                "    <!--comment 1-->\n" +
                "    <string name=\"hello\">Hello</string>\n" +
                "    <!--comment 2-->\n" +
                "    <string name=\"bye\">Bye</string>\n" +
                "    <plurals name=\"plural_things\">\n" +
                "        <item quantity=\"one\">One thing</item>\n" +
                "        <item quantity=\"other\">Multiple things</item>\n" +
                "    </plurals>" +
                "</resources>";

        asset = assetService.createAssetWithContent(repository.getId(), "src/main/res/values/strings.xml", assetContent);
        AssetContent assetContentEntity = assetContentService.createAssetContent(asset, assetContent);
        assetExtractionService.processAssetAsync(assetContentEntity.getId(), null, null, null).get();

        byte[] content1 = ByteStreams.toByteArray(new ClassPathResource("/com/box/l10n/mojito/img/1.png").getInputStream());
        byte[] content2 = ByteStreams.toByteArray(new ClassPathResource("/com/box/l10n/mojito/img/2.png").getInputStream());

        imageService.uploadImage("image1a.png", content1);
        imageService.uploadImage("image2a.png", content2);
        imageService.uploadImage("image3a.png", content1);
        imageService.uploadImage("image4a.png", content1);

        Long tmId = repository.getTm().getId();
        List<TMTextUnit> tmTextUnits = tmTextUnitRepository.findByTm_id(tmId);
        assertEquals(8, tmTextUnits.size());
        tmTextUnitHello = tmTextUnits.stream().filter(t -> "hello".equals(t.getName())).findFirst().get();
        tmTextUnitBye = tmTextUnits.stream().filter(t -> "bye".equals(t.getName())).findFirst().get();
        tmTextUnitPluralThingsZero = tmTextUnits.stream().filter(t -> "plural_things_zero".equals(t.getName())).findFirst().get();
        tmTextUnitPluralThingsOne = tmTextUnits.stream().filter(t -> "plural_things_one".equals(t.getName())).findFirst().get();
        tmTextUnitPluralThingsTwo = tmTextUnits.stream().filter(t -> "plural_things_two".equals(t.getName())).findFirst().get();
        tmTextUnitPluralThingsFew = tmTextUnits.stream().filter(t -> "plural_things_few".equals(t.getName())).findFirst().get();
        tmTextUnitPluralThingsMany = tmTextUnits.stream().filter(t -> "plural_things_many".equals(t.getName())).findFirst().get();
        tmTextUnitPluralThingsOther = tmTextUnits.stream().filter(t -> "plural_things_other".equals(t.getName())).findFirst().get();

        ScreenshotRun screenshotRun = repository.getManualScreenshotRun();
        Screenshot screen1 = new Screenshot();
        screen1.setName("screen1");
        screen1.setSrc("api/images/image1a.png");
        screen1.setStatus(ACCEPTED);

        ScreenshotTextUnit screenshotTextUnit1 = new ScreenshotTextUnit();
        screenshotTextUnit1.setTmTextUnit(tmTextUnitHello);
        screen1.getScreenshotTextUnits().add(screenshotTextUnit1);

        ScreenshotTextUnit screenshotTextUnit2 = new ScreenshotTextUnit();
        screenshotTextUnit2.setTmTextUnit(tmTextUnitBye);
        screen1.getScreenshotTextUnits().add(screenshotTextUnit2);

        Screenshot screen2 = new Screenshot();
        screen2.setName("screen2");
        screen2.setSrc("api/images/image2a.png");
        screen2.setStatus(ACCEPTED);
        screenshotTextUnit1 = new ScreenshotTextUnit();
        screenshotTextUnit1.setTmTextUnit(tmTextUnitHello);
        screen2.getScreenshotTextUnits().add(screenshotTextUnit1);

        Screenshot screen3 = new Screenshot();
        screen3.setName("screen3");
        screen3.setSrc("api/images/image3a.png");
        screen3.setStatus(Screenshot.Status.ACCEPTED);
        ScreenshotTextUnit screenshotTextUnit3 = new ScreenshotTextUnit();
        screenshotTextUnit3.setTmTextUnit(tmTextUnitPluralThingsOther);
        screen3.getScreenshotTextUnits().add(screenshotTextUnit3);
        ScreenshotTextUnit screenshotTextUnit4 = new ScreenshotTextUnit();
        screenshotTextUnit4.setTmTextUnit(tmTextUnitPluralThingsOne);
        screen3.getScreenshotTextUnits().add(screenshotTextUnit4);

        Screenshot screen4 = new Screenshot();
        screen4.setName("screen4");
        screen4.setSrc("api/images/image4a.png");
        screen4.setStatus(Screenshot.Status.ACCEPTED);

        screenshotRun.getScreenshots().add(screen1);
        screenshotRun.getScreenshots().add(screen2);
        screenshotRun.getScreenshots().add(screen3);
        screenshotRun.getScreenshots().add(screen4);
        screenshotService.createOrUpdateScreenshotRun(screenshotRun, false);

        return this;
    }
}

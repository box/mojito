package com.box.l10n.mojito.rest.screenshot;

import com.box.l10n.mojito.entity.Locale;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.Screenshot;
import com.box.l10n.mojito.rest.WSTestBase;
import com.box.l10n.mojito.rest.WSTestDataFactory;
import com.box.l10n.mojito.rest.client.AssetClient;
import com.box.l10n.mojito.rest.client.PollableTaskClient;
import com.box.l10n.mojito.rest.client.ScreenshotClient;
import com.box.l10n.mojito.rest.entity.ScreenshotRun;
import com.box.l10n.mojito.service.asset.AssetRepository;
import com.box.l10n.mojito.service.assetTextUnit.AssetTextUnitRepository;
import com.box.l10n.mojito.service.locale.LocaleService;
import com.box.l10n.mojito.service.repository.RepositoryNameAlreadyUsedException;
import com.box.l10n.mojito.service.repository.RepositoryService;
import com.box.l10n.mojito.service.screenshot.ScreenshotService;
import com.box.l10n.mojito.test.TestIdWatcher;
import com.box.l10n.mojito.test.category.IntegrationTest;
import java.util.Arrays;
import java.util.List;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author aloison
 */
public class ScreenshotWSTest extends WSTestBase {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(ScreenshotWSTest.class);

    @Autowired
    WSTestDataFactory testDataFactory;

    @Autowired
    RepositoryService repositoryService;

    @Autowired
    AssetRepository assetRepository;

    @Autowired
    AssetTextUnitRepository assetTextUnitRepository;

    @Autowired
    LocaleService localeService;

    @Autowired
    AssetClient assetClient;

    @Autowired
    PollableTaskClient pollableTaskClient;

    @Autowired
    ScreenshotClient screenshotClient;

    @Autowired
    ScreenshotService screenshotService;

    @Rule
    public TestIdWatcher testIdWatcher = new TestIdWatcher();
 
    @Test
    @Category({IntegrationTest.class})
    public void testUploadScreenshots() throws RepositoryNameAlreadyUsedException {
        Repository repository = testDataFactory.createRepository(testIdWatcher);
        
        Locale localefrFR = localeService.findByBcp47Tag("fr-FR");
        com.box.l10n.mojito.rest.entity.Locale locale = new com.box.l10n.mojito.rest.entity.Locale();
        locale.setId(localefrFR.getId());
        locale.setBcp47Tag(localefrFR.getBcp47Tag());
        
        ScreenshotRun screenshotRun = new ScreenshotRun();
        com.box.l10n.mojito.rest.entity.Repository r = new com.box.l10n.mojito.rest.entity.Repository();
        r.setName(repository.getName());
        r.setId(repository.getId());
        screenshotRun.setRepository(r);
        
        com.box.l10n.mojito.rest.entity.Screenshot screenshot1 = new com.box.l10n.mojito.rest.entity.Screenshot();
        screenshot1.setName("screenshot1");
        screenshot1.setLocale(locale);
        screenshotRun.getScreenshots().add(screenshot1);
        
        com.box.l10n.mojito.rest.entity.Screenshot screenshot2 = new com.box.l10n.mojito.rest.entity.Screenshot();
        screenshot1.setName("screenshot2");
        screenshot2.setLocale(locale);
        screenshotRun.getScreenshots().add(screenshot2);
        
        screenshotClient.uploadScreenshots(screenshotRun);
        List<Screenshot> searchScreenshots = screenshotService.searchScreenshots(Arrays.asList(
                repository.getId()), 
                null, null, null, null, null, null, null, 10, 0);
        Assert.assertEquals(2, searchScreenshots.size());
    }
}

package com.box.l10n.mojito.rest.screenshot;

import com.box.l10n.mojito.entity.Locale;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.Screenshot;
import com.box.l10n.mojito.json.ObjectMapper;
import com.box.l10n.mojito.rest.WSTestBase;
import com.box.l10n.mojito.rest.WSTestDataFactory;
import com.box.l10n.mojito.rest.client.AssetClient;
import com.box.l10n.mojito.rest.client.PollableTaskClient;
import com.box.l10n.mojito.rest.client.RepositoryClient;
import com.box.l10n.mojito.rest.client.ScreenshotClient;
import com.box.l10n.mojito.rest.client.exception.RepositoryNotFoundException;
import com.box.l10n.mojito.rest.entity.ScreenshotRun;
import com.box.l10n.mojito.rest.entity.TmTextUnit;
import com.box.l10n.mojito.service.asset.AssetRepository;
import com.box.l10n.mojito.service.assetTextUnit.AssetTextUnitRepository;
import com.box.l10n.mojito.service.branch.BranchTestData;
import com.box.l10n.mojito.service.locale.LocaleService;
import com.box.l10n.mojito.service.repository.RepositoryNameAlreadyUsedException;
import com.box.l10n.mojito.service.repository.RepositoryService;
import com.box.l10n.mojito.service.screenshot.ScreenshotRunType;
import com.box.l10n.mojito.service.screenshot.ScreenshotService;
import com.box.l10n.mojito.test.TestIdWatcher;
import com.box.l10n.mojito.test.category.IntegrationTest;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author jeanaurambault
 */
public class ScreenshotWSTest extends WSTestBase {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(ScreenshotWSTest.class);

  @Autowired WSTestDataFactory testDataFactory;

  @Autowired RepositoryService repositoryService;

  @Autowired AssetRepository assetRepository;

  @Autowired AssetTextUnitRepository assetTextUnitRepository;

  @Autowired LocaleService localeService;

  @Autowired AssetClient assetClient;

  @Autowired PollableTaskClient pollableTaskClient;

  @Autowired ScreenshotClient screenshotClient;

  @Autowired ScreenshotService screenshotService;

  @Autowired RepositoryClient repositoryClient;

  @Rule public TestIdWatcher testIdWatcher = new TestIdWatcher();

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
    screenshotRun.setName(testIdWatcher.getEntityName("screenshotrun"));
    screenshotRun.setRepository(r);

    com.box.l10n.mojito.rest.entity.Screenshot screenshot1 =
        new com.box.l10n.mojito.rest.entity.Screenshot();
    screenshot1.setName("screenshot1");
    screenshot1.setLocale(locale);
    screenshotRun.getScreenshots().add(screenshot1);

    com.box.l10n.mojito.rest.entity.Screenshot screenshot2 =
        new com.box.l10n.mojito.rest.entity.Screenshot();
    screenshot1.setName("screenshot2");
    screenshot2.setLocale(locale);
    screenshotRun.getScreenshots().add(screenshot2);

    screenshotClient.uploadScreenshots(screenshotRun);
    List<Screenshot> searchScreenshots =
        screenshotService.searchScreenshots(
            Arrays.asList(repository.getId()),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            ScreenshotRunType.LAST_SUCCESSFUL_RUN,
            0,
            10);
    Assert.assertEquals(2, searchScreenshots.size());
  }

  @Test
  @Category({IntegrationTest.class})
  public void testAddManualScreenshot()
      throws RepositoryNameAlreadyUsedException, RepositoryNotFoundException {

    BranchTestData branchTestData = new BranchTestData(testIdWatcher);

    com.box.l10n.mojito.rest.entity.Branch branch1 = new com.box.l10n.mojito.rest.entity.Branch();
    branch1.setId(branchTestData.getBranch1().getId());

    ScreenshotRun screenshotRun = new ScreenshotRun();
    screenshotRun.setId(branchTestData.getRepository().getManualScreenshotRun().getId());

    com.box.l10n.mojito.rest.entity.Screenshot screenshot =
        new com.box.l10n.mojito.rest.entity.Screenshot();
    screenshot.setName(UUID.randomUUID().toString());
    screenshot.setLocale(new com.box.l10n.mojito.rest.entity.Locale());
    screenshot.getLocale().setId(branchTestData.getRepository().getSourceLocale().getId());
    screenshot.setSrc("http://localhost:8080/api/images/1");
    screenshot.setBranch(branch1);
    screenshotRun.getScreenshots().add(screenshot);

    com.box.l10n.mojito.rest.entity.ScreenshotTextUnit screenshotTextUnit =
        new com.box.l10n.mojito.rest.entity.ScreenshotTextUnit();
    screenshotTextUnit.setTmTextUnit(new TmTextUnit());
    screenshotTextUnit.getTmTextUnit().setId(branchTestData.getString1Branch1().getId());
    screenshot.getScreenshotTextUnits().add(screenshotTextUnit);
    screenshot.setBranch(branch1);

    com.box.l10n.mojito.rest.entity.ScreenshotTextUnit screenshotTextUnit2 =
        new com.box.l10n.mojito.rest.entity.ScreenshotTextUnit();
    screenshotTextUnit2.setTmTextUnit(new TmTextUnit());
    screenshotTextUnit2.getTmTextUnit().setId(branchTestData.getString2Branch1().getId());
    screenshot.getScreenshotTextUnits().add(screenshotTextUnit2);

    ObjectMapper objectMapper = new ObjectMapper();
    logger.debug(objectMapper.writeValueAsStringUnchecked(screenshotRun));

    screenshotClient.uploadScreenshots(screenshotRun);

    List<Screenshot> searchScreenshots =
        screenshotService.searchScreenshots(
            Arrays.asList(branchTestData.getRepository().getId()),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            ScreenshotRunType.MANUAL_RUN,
            0,
            10);
    Assert.assertEquals(1, searchScreenshots.size());

    logger.debug("Make sure no cyclical dependencies here and there");
    repositoryClient.getRepositoryByName(branchTestData.getRepository().getName());
    repositoryClient.getBranches(
        branchTestData.getRepository().getId(), branch1.getName(), null, null, null, false, null);
  }
}

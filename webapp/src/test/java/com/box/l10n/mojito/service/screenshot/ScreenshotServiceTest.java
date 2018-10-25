package com.box.l10n.mojito.service.screenshot;

import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.Screenshot;
import com.box.l10n.mojito.entity.ScreenshotRun;
import com.box.l10n.mojito.service.assetExtraction.ServiceTestBase;
import com.box.l10n.mojito.service.locale.LocaleService;
import com.box.l10n.mojito.service.repository.RepositoryNameAlreadyUsedException;
import com.box.l10n.mojito.service.repository.RepositoryService;
import com.box.l10n.mojito.service.tm.TMTestData;
import com.box.l10n.mojito.test.TestIdWatcher;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.junit.Assert;
import static org.junit.Assert.assertEquals;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import static org.slf4j.LoggerFactory.getLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 * @author jeanaurambault
 */
public class ScreenshotServiceTest extends ServiceTestBase {

    /**
     * logger
     */
    static Logger logger = getLogger(ScreenshotServiceTest.class);

    @Autowired
    RepositoryService repositoryService;

    @Autowired
    ScreenshotService screenshotService;

    @Autowired
    LocaleService localeService;

    @Autowired
    ScreenshotRunRepository screenshotRunRepository;

    @Autowired
    ScreenshotRepository screenshotRepository;

    @Rule
    public TestIdWatcher testIdWatcher = new TestIdWatcher();

    @Test
    public void testCreateScreenshotRun() throws Exception {

        Repository repository = repositoryService.createRepository(testIdWatcher.getEntityName("repository"));

        ScreenshotRun screenshotRun = new ScreenshotRun();
        screenshotRun.setRepository(repository);

        Screenshot screenshot1 = new Screenshot();
        screenshot1.setName("s1");
        Screenshot screenshot2 = new Screenshot();
        screenshot2.setName("s2");
        screenshotRun.getScreenshots().add(screenshot1);
        screenshotRun.getScreenshots().add(screenshot2);

        ScreenshotRun createScreenshotRun = screenshotService.createOrUpdateScreenshotRun(screenshotRun);

        ScreenshotRun createdFromDB = screenshotRunRepository.findOne(createScreenshotRun.getId());
        ArrayList<Screenshot> arrayList = new ArrayList<>(createdFromDB.getScreenshots());
        Assert.assertNotNull(arrayList.get(0).getId());
        Assert.assertNotNull(arrayList.get(1).getId());
    }
    
    @Test
    public void testUpdateScreenshotRun() throws Exception {

        Repository repository = repositoryService.createRepository(testIdWatcher.getEntityName("repository"));

        ScreenshotRun screenshotRun = new ScreenshotRun();
        screenshotRun.setRepository(repository);
        screenshotRun.setName(UUID.randomUUID().toString());

        Screenshot screenshot1 = new Screenshot();
        screenshot1.setName("s1");
        Screenshot screenshot2 = new Screenshot();
        screenshot2.setName("s2");
        screenshotRun.getScreenshots().add(screenshot1);
        screenshotRun.getScreenshots().add(screenshot2);
        screenshotService.createOrUpdateScreenshotRun(screenshotRun);

        ScreenshotRun forUpdate = new ScreenshotRun();
        forUpdate.setRepository(repository);
        forUpdate.setName(screenshotRun.getName());

        Screenshot screenshot3 = new Screenshot();
        screenshot3.setName("s3");
        forUpdate.getScreenshots().add(screenshot3);

        ScreenshotRun updatedScreenshotRun = screenshotService.createOrUpdateScreenshotRun(forUpdate);

        ScreenshotRun updatedFromDB = screenshotRunRepository.findOne(updatedScreenshotRun.getId());
        ArrayList<Screenshot> arrayList = new ArrayList<>(updatedFromDB.getScreenshots());
        Assert.assertNotNull(arrayList.get(0).getId());
        Assert.assertNotNull(arrayList.get(1).getId());
        Assert.assertNotNull(arrayList.get(2).getId());
    }

    @Test
    public void testSortScreenshotBySequence() {
        List<Screenshot> screenshots = new ArrayList<>();

        Screenshot screen1 = new Screenshot();
        screen1.setSequence(1L);
        screenshots.add(screen1);

        Screenshot screen2 = new Screenshot();
        screen2.setSequence(10L);
        screenshots.add(screen2);

        Screenshot screen3 = new Screenshot();
        screen3.setSequence(5L);
        screenshots.add(screen3);

        screenshotService.sortScreenshotBySequence(screenshots);

        assertEquals(screen1, screenshots.get(0));
        assertEquals(screen3, screenshots.get(1));
        assertEquals(screen2, screenshots.get(2));
    }

    @Transactional
    @Test
    public void testSearchScreenshotsByRepository() throws RepositoryNameAlreadyUsedException {
        Repository repository = createScreenshotData();
        List<Screenshot> searchScreenshots = screenshotService.searchScreenshots(
                Arrays.asList(repository.getId()), null, null, null, null, null, null, null, 10, 0);
        assertEquals("screen1", searchScreenshots.get(0).getName());
        assertEquals("screen3", searchScreenshots.get(1).getName());
        assertEquals("screen2", searchScreenshots.get(2).getName());
    }

    @Transactional
    @Test
    public void testSearchScreenshotsByLocale() throws RepositoryNameAlreadyUsedException {

        Repository repository = createScreenshotData();

        List<Screenshot> searchScreenshots = screenshotService.searchScreenshots(
                Arrays.asList(repository.getId()),
                Arrays.asList("fr-FR"),
                null, null, null, null, null, null, 10, 0);

        assertEquals("screen1", searchScreenshots.get(0).getName());
        assertEquals("screen2", searchScreenshots.get(1).getName());
        assertEquals(2, searchScreenshots.size());

        searchScreenshots = screenshotService.searchScreenshots(
                Arrays.asList(repository.getId()),
                Arrays.asList("ko-KR"),
                null, null, null, null, null, null, 10, 0);

        assertEquals("screen3", searchScreenshots.get(0).getName());
        assertEquals(1, searchScreenshots.size());
    }

    @Transactional
    @Test
    public void testSearchScreenshotsByName() throws RepositoryNameAlreadyUsedException {

        Repository repository = createScreenshotData();

        List<Screenshot> searchScreenshots = screenshotService.searchScreenshots(
                Arrays.asList(repository.getId()),
                null,
                "screen2", null, null, null, null, null, 10, 0);

        assertEquals("screen2", searchScreenshots.get(0).getName());
        assertEquals(1, searchScreenshots.size());
    }

    @Transactional
    @Test
    public void testSearchScreenshotsByStatus() throws RepositoryNameAlreadyUsedException {

        Repository repository = createScreenshotData();

        List<Screenshot> searchScreenshots = screenshotService.searchScreenshots(
                Arrays.asList(repository.getId()),
                null, null, Screenshot.Status.NEEDS_REVIEW, 
                null, null, null, null,10, 0);

        assertEquals("screen3", searchScreenshots.get(0).getName());
        assertEquals(1, searchScreenshots.size());

        searchScreenshots = screenshotService.searchScreenshots(
                Arrays.asList(repository.getId()),
                null, null, Screenshot.Status.REJECTED, 
                null, null, null, null, 10, 0);

        assertEquals("screen1", searchScreenshots.get(0).getName());
        assertEquals(1, searchScreenshots.size());

        searchScreenshots = screenshotService.searchScreenshots(
                Arrays.asList(repository.getId()),
                null, null, Screenshot.Status.ACCEPTED, 
                null, null, null, null, 10, 0);

        assertEquals("screen2", searchScreenshots.get(0).getName());
        assertEquals(1, searchScreenshots.size());
    }

    @Transactional
    @Test
    public void testSearchScreenshotsByPagination() throws RepositoryNameAlreadyUsedException {

        Repository repository = createScreenshotData();

        List<Screenshot> searchScreenshots = screenshotService.searchScreenshots(
                Arrays.asList(repository.getId()), null, null, null, 
                null, null, null, null, 1, 1);

        assertEquals("screen3", searchScreenshots.get(0).getName());
        assertEquals(1, searchScreenshots.size());
    }

    @Transactional
    @Test
    public void testLastRunSuccessful() throws RepositoryNameAlreadyUsedException {

        Repository repository = createScreenshotData();
        createScreenshotData(repository);

        List<Screenshot> searchScreenshots = screenshotService.searchScreenshots(
                Arrays.asList(repository.getId()), null, null, null, 
                null, null, null, null, 10, 0);

        assertEquals(3, searchScreenshots.size());
        
    }

    private Repository createScreenshotData() {
        TMTestData tmTestDataSource = new TMTestData(testIdWatcher);
        Repository repository = tmTestDataSource.repository;
        return createScreenshotData(repository);
    }

    @Transactional
    private Repository createScreenshotData(Repository repository) {

        Screenshot screen1 = new Screenshot();
        screen1.setName("screen1");
        screen1.setLocale(localeService.findByBcp47Tag("fr-FR"));
        screen1.setSequence(1L);
        screen1.setStatus(Screenshot.Status.REJECTED);

        Screenshot screen2 = new Screenshot();
        screen2.setName("screen2");
        screen2.setLocale(localeService.findByBcp47Tag("fr-FR"));
        screen2.setSequence(10L);
        screen2.setStatus(Screenshot.Status.ACCEPTED);

        Screenshot screen3 = new Screenshot();
        screen3.setName("screen3");
        screen3.setLocale(localeService.findByBcp47Tag("ko-KR"));
        screen3.setSequence(5L);
        screen3.setStatus(Screenshot.Status.NEEDS_REVIEW);

        ScreenshotRun screenshotRun = new ScreenshotRun();
        screenshotRun.setRepository(repository);
        screenshotRun.getScreenshots().add(screen1);
        screenshotRun.getScreenshots().add(screen2);
        screenshotRun.getScreenshots().add(screen3);
        screenshotService.createOrUpdateScreenshotRun(screenshotRun);

        return repository;
    }

}

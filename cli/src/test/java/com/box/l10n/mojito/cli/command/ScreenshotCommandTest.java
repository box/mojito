package com.box.l10n.mojito.cli.command;

import com.box.l10n.mojito.cli.CLITestBase;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.rest.client.RepositoryClient;
import com.box.l10n.mojito.rest.entity.Locale;
import java.nio.file.Path;
import java.nio.file.Paths;
import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class ScreenshotCommandTest extends CLITestBase {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(ScreenshotCommandTest.class);

    @Autowired
    RepositoryClient repositoryClient;

    @Autowired
    CommandHelper commandHelper;
   
    @Test
    public void execute() throws Exception {
        String repoName = testIdWatcher.getEntityName("screenshot_execute");
        Repository repository = repositoryService.createRepository(repoName, repoName + " description");

        repositoryService.addRepositoryLocale(repository, "fr-FR");
        repositoryService.addRepositoryLocale(repository, "ja-JP");
        repositoryService.addRepositoryLocale(repository, "en-US");  
        getL10nJCommander().run("push", "-r", repository.getName(), "-s", getInputResourcesTestDir().toString());
        getL10nJCommander().run("import", "-r", repository.getName(), "-s", getInputResourcesTestDir().toString());
        getL10nJCommander().run("screenshot", "-r", repository.getName(), "-s", getInputResourcesTestDir().toString());
        getL10nJCommander().run("screenshot", "-r", repository.getName(), "-rn", "test1", "-s", getInputResourcesTestDir().toString());
        getL10nJCommander().run("screenshot", "-r", repository.getName(), "-rn", "test1", "-s", getInputResourcesTestDir().toString());
    }
        
    @Test
    public void testGetMetadataFilenameForImage() throws Exception {
        ScreenshotCommand screenshotCommand = new ScreenshotCommand();
        Path path = Paths.get("a", "b", "img.png");
        Path expected = Paths.get("a", "b", "img.json");
        assertEquals(expected, screenshotCommand.getMetadataFilenameForImage(path));
    }

    @Test
    public void testGetFirstElementInPath() throws CommandException {
        ScreenshotCommand screenshotCommand = new ScreenshotCommand();
        String firstName = screenshotCommand.getFirstElementInPath(Paths.get("fr-FR/file.png"));
        assertEquals("fr-FR", firstName);
    }

    @Test
    public void testGetFirstElementInPathMultiLevel() throws CommandException {
        ScreenshotCommand screenshotCommand = new ScreenshotCommand();
        String firstName = screenshotCommand.getFirstElementInPath(Paths.get("fr-FR/directory/file.png"));
        assertEquals("fr-FR", firstName);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetFirstElementInPathNoDir() throws CommandException {
        ScreenshotCommand screenshotCommand = new ScreenshotCommand();
        screenshotCommand.getFirstElementInPath(Paths.get("file.png"));
    }

    @Test
    public void testGetLocaleFromImagePathNoMapping() throws Exception {
        Repository repository = createTestRepoUsingRepoService();
        com.box.l10n.mojito.rest.entity.Repository repositoryByName = repositoryClient.getRepositoryByName(repository.getName());
        ScreenshotCommand screenshotCommand = new ScreenshotCommand();
        screenshotCommand.repositoryLocales = commandHelper.getSortedRepositoryLocales(repositoryByName);

        Locale localeFromImagePath = screenshotCommand.getLocaleFromImagePath(Paths.get("fr-FR/sub/file.png"));
        assertEquals("fr-FR", localeFromImagePath.getBcp47Tag());
    }

    @Test(expected = ScreenshotCommand.InvalidLocaleException.class)
    public void testGetLocaleFromImagePathNoMappingUnsupportedLocale() throws Exception {
        Repository repository = createTestRepoUsingRepoService();
        com.box.l10n.mojito.rest.entity.Repository repositoryByName = repositoryClient.getRepositoryByName(repository.getName());
        ScreenshotCommand screenshotCommand = new ScreenshotCommand();
        screenshotCommand.repositoryLocales = commandHelper.getSortedRepositoryLocales(repositoryByName);

        Locale localeFromImagePath = screenshotCommand.getLocaleFromImagePath(Paths.get("en-ZA/sub/file.png"));
        assertEquals("fr-FR", localeFromImagePath.getBcp47Tag());
    }

    @Test
    public void testGetLocaleFromImagePathMappedLocale() throws Exception {
        Repository repository = createTestRepoUsingRepoService();
        com.box.l10n.mojito.rest.entity.Repository repositoryByName = repositoryClient.getRepositoryByName(repository.getName());
        ScreenshotCommand screenshotCommand = new ScreenshotCommand();
        screenshotCommand.localeMappings = commandHelper.getLocaleMapping("fr:fr-FR");
        screenshotCommand.repositoryLocales = commandHelper.getSortedRepositoryLocales(repositoryByName);

        Locale localeFromImagePath = screenshotCommand.getLocaleFromImagePath(Paths.get("fr/sub/file.png"));
        assertEquals("fr-FR", localeFromImagePath.getBcp47Tag());
    }

}

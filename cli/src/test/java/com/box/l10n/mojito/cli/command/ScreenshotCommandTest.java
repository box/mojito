package com.box.l10n.mojito.cli.command;

import static org.junit.Assert.assertEquals;

import com.box.l10n.mojito.LocaleMappingHelper;
import com.box.l10n.mojito.cli.CLITestBase;
import com.box.l10n.mojito.cli.apiclient.RepositoryWsApiProxy;
import com.box.l10n.mojito.cli.model.Locale;
import com.box.l10n.mojito.cli.model.RepositoryRepository;
import com.box.l10n.mojito.entity.Repository;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class ScreenshotCommandTest extends CLITestBase {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(ScreenshotCommandTest.class);

  @Autowired RepositoryWsApiProxy repositoryClient;

  @Autowired CommandHelper commandHelper;

  @Autowired LocaleMappingHelper localeMappingHelper;

  @Test
  public void execute() throws Exception {
    String repoName = testIdWatcher.getEntityName("screenshot_execute");
    Repository repository =
        repositoryService.createRepository(repoName, repoName + " description", null, false);

    repositoryService.addRepositoryLocale(repository, "fr-FR");
    repositoryService.addRepositoryLocale(repository, "ja-JP");
    repositoryService.addRepositoryLocale(repository, "en-US");
    getL10nJCommander()
        .run("push", "-r", repository.getName(), "-s", getInputResourcesTestDir().toString());
    getL10nJCommander()
        .run("import", "-r", repository.getName(), "-s", getInputResourcesTestDir().toString());
    getL10nJCommander()
        .run("screenshot", "-r", repository.getName(), "-s", getInputResourcesTestDir().toString());
    getL10nJCommander()
        .run(
            "screenshot",
            "-r",
            repository.getName(),
            "-rn",
            "test1",
            "-s",
            getInputResourcesTestDir().toString());
    getL10nJCommander()
        .run(
            "screenshot",
            "-r",
            repository.getName(),
            "-rn",
            "test1",
            "-s",
            getInputResourcesTestDir().toString());
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
    String firstName =
        screenshotCommand.getFirstElementInPath(Paths.get("fr-FR/directory/file.png"));
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
    RepositoryRepository repositoryByName =
        repositoryClient.getRepositoryByName(repository.getName());
    ScreenshotCommand screenshotCommand = new ScreenshotCommand();
    screenshotCommand.repositoryLocales =
        commandHelper.getSortedRepositoryLocales(repositoryByName);

    Locale localeFromImagePath =
        screenshotCommand.getLocaleFromImagePath(Paths.get("fr-FR/sub/file.png"));
    assertEquals("fr-FR", localeFromImagePath.getBcp47Tag());
  }

  @Test(expected = ScreenshotCommand.InvalidLocaleException.class)
  public void testGetLocaleFromImagePathNoMappingUnsupportedLocale() throws Exception {
    Repository repository = createTestRepoUsingRepoService();
    RepositoryRepository repositoryByName =
        repositoryClient.getRepositoryByName(repository.getName());
    ScreenshotCommand screenshotCommand = new ScreenshotCommand();
    screenshotCommand.repositoryLocales =
        commandHelper.getSortedRepositoryLocales(repositoryByName);

    Locale localeFromImagePath =
        screenshotCommand.getLocaleFromImagePath(Paths.get("en-ZA/sub/file.png"));
    assertEquals("fr-FR", localeFromImagePath.getBcp47Tag());
  }

  @Test
  public void testGetLocaleFromImagePathMappedLocale() throws Exception {
    Repository repository = createTestRepoUsingRepoService();
    RepositoryRepository repositoryByName =
        repositoryClient.getRepositoryByName(repository.getName());
    ScreenshotCommand screenshotCommand = new ScreenshotCommand();
    screenshotCommand.localeMappings = localeMappingHelper.getLocaleMapping("fr:fr-FR");
    screenshotCommand.repositoryLocales =
        commandHelper.getSortedRepositoryLocales(repositoryByName);

    Locale localeFromImagePath =
        screenshotCommand.getLocaleFromImagePath(Paths.get("fr/sub/file.png"));
    assertEquals("fr-FR", localeFromImagePath.getBcp47Tag());
  }
}

package com.box.l10n.mojito.cli;

import com.box.l10n.mojito.Application;
import com.box.l10n.mojito.cli.apiclient.ApiClientConfigurer;
import com.box.l10n.mojito.cli.command.L10nJCommander;
import com.box.l10n.mojito.entity.Locale;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.TMTextUnitVariant;
import com.box.l10n.mojito.rest.client.RepositoryClient;
import com.box.l10n.mojito.rest.resttemplate.AuthenticatedRestTemplate;
import com.box.l10n.mojito.rest.resttemplate.ResttemplateConfig;
import com.box.l10n.mojito.service.asset.AssetRepository;
import com.box.l10n.mojito.service.locale.LocaleService;
import com.box.l10n.mojito.service.repository.RepositoryService;
import com.box.l10n.mojito.service.tm.TMImportService;
import com.box.l10n.mojito.service.tm.TMService;
import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcher;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcherParameters;
import com.box.l10n.mojito.test.IOTestBase;
import com.box.l10n.mojito.xml.XmlParsingConfiguration;
import com.google.common.io.Files;
import jakarta.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.OutputCaptureRule;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Base class for CLI integration tests. Creates an in-memory instance of tomcat and setup the REST
 * client to use the port that was bound during container initialization.
 *
 * @author jaurambault
 */
@RunWith(SpringRunner.class)
@SpringBootTest(
    classes = {Application.class},
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class CLITestBase extends IOTestBase {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(CLITestBase.class);

  @Autowired AuthenticatedRestTemplate authenticatedRestTemplate;

  @Autowired protected RepositoryService repositoryService;

  @Autowired LocaleService localeService;

  @Autowired TMImportService tmImportService;

  @Autowired TMService tmService;

  @Autowired ResttemplateConfig resttemplateConfig;

  @Autowired RepositoryClient repositoryClient;

  @Autowired AssetRepository assetRepository;

  @Autowired TextUnitSearcher textUnitSearcher;

  @Rule public OutputCaptureRule outputCapture = new OutputCaptureRule();

  @LocalServerPort int port;

  @Autowired ApiClientConfigurer apiClientConfigurer;

  @PostConstruct
  public void setPort() {
    logger.debug("Saving port number = {}", port);
    resttemplateConfig.setPort(port);
    this.apiClientConfigurer.init();

    XmlParsingConfiguration.disableXPathLimits();
  }

  public void resetHost() {
    resttemplateConfig.setHost("localhost");
    this.apiClientConfigurer.init();
  }

  public void setNonExistentHost() {
    resttemplateConfig.setHost("nonExistentHostAddress");
    this.apiClientConfigurer.init();
  }

  public L10nJCommander getL10nJCommander() {
    L10nJCommander l10nJCommander = new L10nJCommander();
    l10nJCommander.setSystemExitEnabled(false);
    return l10nJCommander;
  }

  /**
   * Creates test repo. This uses the repo service directly so it is not meant to test the
   * repo-create command but purely to facilate other tests.
   *
   * @return
   */
  public Repository createTestRepoUsingRepoService() throws Exception {
    return createTestRepoUsingRepoService("repo", false);
  }

  public Repository createTestRepoUsingRepoService(String name, Boolean checkSLA) throws Exception {

    String repoName = testIdWatcher.getEntityName(name);
    Repository repository =
        repositoryService.createRepository(repoName, repoName + " description", null, checkSLA);

    repositoryService.addRepositoryLocale(repository, "fr-FR");
    repositoryService.addRepositoryLocale(repository, "fr-CA", "fr-FR", false);
    repositoryService.addRepositoryLocale(repository, "ja-JP");

    return repository;
  }

  public void importTranslations(Long assetId, String baseName, String bcp47Tag)
      throws IOException {
    File file = new File(getInputResourcesTestDir("translations"), baseName + bcp47Tag + ".xliff");
    String fileContent = Files.toString(file, StandardCharsets.UTF_8);
    try {
      tmImportService.importXLIFF(assetId, fileContent, true);
    } catch (RuntimeException re) {
      TextUnitSearcherParameters textUnitSearcherParameters = new TextUnitSearcherParameters();
      textUnitSearcherParameters.setAssetId(assetId);
      textUnitSearcherParameters.setLocaleTags(Arrays.asList("en"));

      List<TextUnitDTO> search = textUnitSearcher.search(textUnitSearcherParameters);
      for (TextUnitDTO textUnitDTO : search) {
        logger.info(
            "name[{}], source[{}], target[{}], comment[{}]",
            textUnitDTO.getName(),
            textUnitDTO.getSource(),
            textUnitDTO.getTarget(),
            textUnitDTO.getComment());
      }
    }
  }

  public void updateTranslationsStatus(
      Long assetId, TMTextUnitVariant.Status status, String bcp47Tag) throws IOException {
    TextUnitSearcherParameters textUnitSearcherParameters = new TextUnitSearcherParameters();
    textUnitSearcherParameters.setAssetId(assetId);
    textUnitSearcherParameters.setLocaleTags(Arrays.asList(bcp47Tag));

    Locale locale = localeService.findByBcp47Tag(bcp47Tag);

    List<TextUnitDTO> search = textUnitSearcher.search(textUnitSearcherParameters);
    for (TextUnitDTO textUnitDTO : search) {
      logger.debug(
          "name[{}], source[{}], target[{}], comment[{}]",
          textUnitDTO.getName(),
          textUnitDTO.getSource(),
          textUnitDTO.getTarget(),
          textUnitDTO.getComment());

      if (textUnitDTO.getTarget() != null) {
        tmService.addCurrentTMTextUnitVariant(
            textUnitDTO.getTmTextUnitId(), locale.getId(), textUnitDTO.getTarget(), status, true);
      }
    }
  }

  /**
   * Wait until a condition is true with timeout.
   *
   * @param failMessage
   * @param condition
   * @throws InterruptedException
   */
  protected void waitForCondition(String failMessage, Supplier<Boolean> condition)
      throws InterruptedException {
    int numberAttempt = 0;
    while (true) {
      numberAttempt++;

      boolean res;

      try {
        res = condition.get();
      } catch (Throwable t) {
        logger.warn("Throwable while waiting for condition", t);
        res = false;
      }

      if (res) {
        break;
      } else if (numberAttempt > 30) {
        Assert.fail(failMessage);
      }
      Thread.sleep(numberAttempt * 100);
    }
  }

  /** GitActions has an even differnt type of checkout than Travis, just skip for now */
  protected boolean isGitActions() {
    Boolean githubActions = Boolean.valueOf(System.getenv("GITHUB_ACTIONS"));
    logger.info("Github actions: {}", githubActions);
    return githubActions;
  }
}

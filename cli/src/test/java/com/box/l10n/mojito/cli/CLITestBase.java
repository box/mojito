package com.box.l10n.mojito.cli;

import com.box.l10n.mojito.Application;
import com.box.l10n.mojito.cli.command.L10nJCommander;
import com.box.l10n.mojito.entity.Locale;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.TMTextUnitVariant;
import com.box.l10n.mojito.rest.client.RepositoryClient;
import com.box.l10n.mojito.rest.entity.RepositoryLocaleStatistic;
import com.box.l10n.mojito.rest.entity.RepositoryStatistic;
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
import com.google.common.io.Files;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.OutputCaptureRule;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

/**
 * Base class for CLI integration tests. Creates an in-memory instance of tomcat
 * and setup the REST client to use the port that was bound during container
 * initialization.
 *
 * @author jaurambault
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {Application.class},
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class CLITestBase extends IOTestBase {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(CLITestBase.class);

    @Autowired
    AuthenticatedRestTemplate authenticatedRestTemplate;

    @Autowired
    protected RepositoryService repositoryService;

    @Autowired
    LocaleService localeService;

    @Autowired
    TMImportService tmImportService;

    @Autowired
    TMService tmService;

    @Autowired
    ResttemplateConfig resttemplateConfig;

    @Autowired
    RepositoryClient repositoryClient;

    @Autowired
    AssetRepository assetRepository;

    @Autowired
    TextUnitSearcher textUnitSearcher;

    @Rule
    public OutputCaptureRule outputCapture = new OutputCaptureRule();

    @LocalServerPort
    int port;

    @PostConstruct
    public void setPort() {
        logger.debug("Saving port number = {}", port);
        resttemplateConfig.setPort(port);
    }

    public L10nJCommander getL10nJCommander() {
        L10nJCommander l10nJCommander = new L10nJCommander();
        l10nJCommander.setSystemExitEnabled(false);
        return l10nJCommander;
    }

    /**
     * Creates test repo. This uses the repo service directly so it is not meant
     * to test the repo-create command but purely to facilate other tests.
     *
     * @return
     */
    public Repository createTestRepoUsingRepoService() throws Exception {
        return createTestRepoUsingRepoService("repo", false);
    }

    public Repository createTestRepoUsingRepoService(String name, Boolean checkSLA) throws Exception {

        String repoName = testIdWatcher.getEntityName(name);
        Repository repository = repositoryService.createRepository(repoName, repoName + " description", null, checkSLA);

        repositoryService.addRepositoryLocale(repository, "fr-FR");
        repositoryService.addRepositoryLocale(repository, "fr-CA", "fr-FR", false);
        repositoryService.addRepositoryLocale(repository, "ja-JP");

        return repository;
    }

    public void importTranslations(Long assetId, String baseName, String bcp47Tag) throws IOException {
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
                logger.info("name[{}], source[{}], target[{}], comment[{}]", textUnitDTO.getName(), textUnitDTO.getSource(), textUnitDTO.getTarget(), textUnitDTO.getComment());
            }
        }

    }

    public void updateTranslationsStatus(Long assetId, TMTextUnitVariant.Status status, String bcp47Tag) throws IOException {
        TextUnitSearcherParameters textUnitSearcherParameters = new TextUnitSearcherParameters();
        textUnitSearcherParameters.setAssetId(assetId);
        textUnitSearcherParameters.setLocaleTags(Arrays.asList(bcp47Tag));

        Locale locale = localeService.findByBcp47Tag(bcp47Tag);

        List<TextUnitDTO> search = textUnitSearcher.search(textUnitSearcherParameters);
        for (TextUnitDTO textUnitDTO : search) {
            logger.debug("name[{}], source[{}], target[{}], comment[{}]", textUnitDTO.getName(), textUnitDTO.getSource(), textUnitDTO.getTarget(), textUnitDTO.getComment());

            if (textUnitDTO.getTarget() != null) {
                tmService.addCurrentTMTextUnitVariant(textUnitDTO.getTmTextUnitId(), locale.getId(), textUnitDTO.getTarget(), status, true);
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
    protected void waitForCondition(String failMessage, Supplier<Boolean> condition) throws InterruptedException {
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

    protected void waitForRepositoryToHaveStringsForTranslations(Long repositoryId) throws InterruptedException {
        waitForCondition("wait for repository stats to show forTranslationCount > 0 before exporting a drop", () -> {
            com.box.l10n.mojito.rest.entity.Repository repository = repositoryClient.getRepositoryById(repositoryId);
            RepositoryStatistic repositoryStat = repository.getRepositoryStatistic();
            boolean forTranslation = false;
            for (RepositoryLocaleStatistic repositoryLocaleStat : repositoryStat.getRepositoryLocaleStatistics()) {
                if (repositoryLocaleStat.getForTranslationCount() > 0L) {
                    forTranslation = true;
                }
                break;
            }
            return forTranslation;
        });
    }

}

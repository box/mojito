package com.box.l10n.mojito.cli;

import com.box.l10n.mojito.cli.command.L10nJCommander;
import com.box.l10n.mojito.entity.Locale;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.RepositoryLocale;
import com.box.l10n.mojito.entity.TMTextUnitVariant;
import com.box.l10n.mojito.rest.resttemplate.AuthenticatedRestTemplate;
import com.box.l10n.mojito.rest.resttemplate.ResttemplateConfig;
import com.box.l10n.mojito.service.asset.AssetRepository;
import com.box.l10n.mojito.service.locale.LocaleService;
import com.box.l10n.mojito.service.repository.RepositoryLocaleCreationException;
import com.box.l10n.mojito.service.repository.RepositoryService;
import com.box.l10n.mojito.service.tm.TMImportService;
import com.box.l10n.mojito.service.tm.TMService;
import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcher;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcherParameters;
import com.box.l10n.mojito.test.IOTestBase;
import com.google.common.io.Files;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Rule;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.embedded.EmbeddedServletContainerInitializedEvent;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.OutputCapture;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.aspectj.EnableSpringConfigured;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

/**
 * Base class for CLI integration tests. Creates an in-memory instance of tomcat
 * and setup the REST client to use the port that was bound during container
 * initialization.
 *
 * @author jaurambault
 */
@EnableAutoConfiguration
@EnableSpringConfigured
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = {CLITestBase.class})
@ComponentScan("com.box.l10n.mojito")
@WebAppConfiguration
@IntegrationTest({"server.port=0", "l10n.consoleWriter.ansiCodeEnabled=false"})
@Configuration
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
    AssetRepository assetRepository;

    @Autowired
    TextUnitSearcher textUnitSearcher;

    @Rule
    public OutputCapture outputCapture = new OutputCapture();

    @Bean
    public ApplicationListener<EmbeddedServletContainerInitializedEvent> getApplicationListenerEmbeddedServletContainerInitializedEvent() {

        return new ApplicationListener<EmbeddedServletContainerInitializedEvent>() {

            @Override
            public void onApplicationEvent(EmbeddedServletContainerInitializedEvent event) {
                int serverPort = event.getEmbeddedServletContainer().getPort();
                logger.debug("Saving port number = {}", serverPort);
                resttemplateConfig.setPort(serverPort);
            }
        };
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
        return createTestRepoUsingRepoService("repo");
    }

    public Repository createTestRepoUsingRepoService(String name) throws Exception {

        String repoName = testIdWatcher.getEntityName(name);
        Repository repository = repositoryService.createRepository(repoName, repoName + " description");

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
}

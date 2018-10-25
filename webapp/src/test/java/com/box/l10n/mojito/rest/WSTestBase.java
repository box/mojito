package com.box.l10n.mojito.rest;

import com.box.l10n.mojito.Application;
import com.box.l10n.mojito.factory.XliffDataFactory;
import com.box.l10n.mojito.rest.annotation.WithDefaultTestUser;
import com.box.l10n.mojito.rest.client.LocaleClient;
import com.box.l10n.mojito.rest.client.exception.LocaleNotFoundException;
import com.box.l10n.mojito.rest.entity.RepositoryLocale;
import com.box.l10n.mojito.rest.resttemplate.AuthenticatedRestTemplate;
import com.box.l10n.mojito.rest.resttemplate.ResttemplateConfig;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.embedded.EmbeddedServletContainerInitializedEvent;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.WebIntegrationTest;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.security.test.context.support.WithSecurityContextTestExecutionListener;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;

import static org.mockito.Mockito.mock;

/**
 * Base class for WS integration tests. Creates an in-memory instance of tomcat
 * and setup the REST client to use the port that was bound during container
 * initialization.
 *
 * @author jaurambault
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = Application.class)
@WebIntegrationTest(randomPort = true, value = {"l10n.fileSystemDropExporter.path=target/test-output/fileSystemDropExporter"})
@TestExecutionListeners(
        listeners = {
            DependencyInjectionTestExecutionListener.class,
            DirtiesContextTestExecutionListener.class,
            TransactionalTestExecutionListener.class,
            WithSecurityContextTestExecutionListener.class
        }
)
@Configuration
@WithDefaultTestUser
//TODO(P1) see issue with DropServiceBoxTest  @DirtiesContext
public class WSTestBase {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(WSTestBase.class);

    @Autowired
    protected AuthenticatedRestTemplate authenticatedRestTemplate;

    @Autowired
    protected XliffDataFactory xliffDataFactory;

    @Autowired
    protected LocaleClient localeClient;
    
    @Autowired
    ResttemplateConfig resttemplateConfig;
    
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

    /**
     * Returns a list of {@link RepositoryLocale}s whose locales correspond to
     * the given tags
     *
     * @param bcp47Tags
     * @return
     */
    protected Set<RepositoryLocale> getRepositoryLocales(List<String> bcp47Tags) {

        Set<RepositoryLocale> repositoryLocales = new HashSet<>();

        for (String bcp47Tag : bcp47Tags) {
            try {
                RepositoryLocale repositoryLocale = new RepositoryLocale();
                repositoryLocale.setLocale(localeClient.getLocaleByBcp47Tag(bcp47Tag));
                repositoryLocales.add(repositoryLocale);
            } catch (LocaleNotFoundException e) {
                logger.error("Locale not found for BCP47 tag: {}. Skipping it.", bcp47Tag);
            }
        }

        return repositoryLocales;
    }
}

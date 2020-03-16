package com.box.l10n.mojito.rest;

import com.box.l10n.mojito.Application;
import com.box.l10n.mojito.factory.XliffDataFactory;
import com.box.l10n.mojito.rest.annotation.WithDefaultTestUser;
import com.box.l10n.mojito.rest.client.LocaleClient;
import com.box.l10n.mojito.rest.client.exception.LocaleNotFoundException;
import com.box.l10n.mojito.rest.entity.RepositoryLocale;
import com.box.l10n.mojito.rest.resttemplate.AuthenticatedRestTemplate;
import com.box.l10n.mojito.rest.resttemplate.ResttemplateConfig;
import org.junit.Assert;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.embedded.EmbeddedWebApplicationContext;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.test.context.support.WithSecurityContextTestExecutionListener;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;

import javax.annotation.PostConstruct;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Base class for WS integration tests. Creates an in-memory instance of tomcat
 * and setup the REST client to use the port that was bound during container
 * initialization.
 *
 * @author jaurambault
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"l10n.fileSystemDropExporter.path=target/test-output/fileSystemDropExporter"})
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

    @Autowired
    EmbeddedWebApplicationContext embeddedWebApplicationContext;

    @PostConstruct
    public void setServerPortOnResttemplate() {
        int port = embeddedWebApplicationContext.getEmbeddedServletContainer().getPort();
        logger.debug("Saving port number = {}", port);
        resttemplateConfig.setPort(port);
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

    /**
     * Wait until a condition is true with timeout.
     *
     * @param failMessage
     * @param condition
     * @throws InterruptedException
     */
    protected  void waitForCondition(String failMessage, Supplier<Boolean> condition) throws InterruptedException {
        int numberAttempt = 0;
        while (true) {
            numberAttempt++;

            boolean res;

            try {
                res = condition.get();
            } catch(Throwable t) {
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
}

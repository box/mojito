package com.box.l10n.mojito.rest;

import com.amazonaws.services.opsworks.model.App;
import com.box.l10n.mojito.Application;
import com.box.l10n.mojito.factory.XliffDataFactory;
import com.box.l10n.mojito.json.ObjectMapper;
import com.box.l10n.mojito.rest.annotation.WithDefaultTestUser;
import com.box.l10n.mojito.rest.client.LocaleClient;
import com.box.l10n.mojito.rest.client.exception.LocaleNotFoundException;
import com.box.l10n.mojito.rest.entity.RepositoryLocale;
import com.box.l10n.mojito.rest.resttemplate.AuthenticatedRestTemplate;
import com.box.l10n.mojito.rest.resttemplate.ResttemplateConfig;
import com.fasterxml.jackson.databind.DeserializationFeature;
import org.junit.Assert;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.quartz.QuartzAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.retry.backoff.ExponentialRandomBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.test.context.junit4.SpringRunner;

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






//        (
//        classes = Application.class,
//        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
//        properties = {"l10n.file-system-drop-exporter.path=target/test-output/fileSystemDropExporter"}
//        )
//@Configuration
//@TestExecutionListeners(
//        listeners = {
//                DependencyInjectionTestExecutionListener.class,
//                DirtiesContextTestExecutionListener.class,
//                TransactionalTestExecutionListener.class,
//                WithSecurityContextTestExecutionListener.class
//        }
//)
//TODO(spring2) why is is not picked up from the main config
//@EnableAutoConfiguration(exclude = QuartzAutoConfiguration.class)
//
//@SpringBootTest(classes = WSTestBase.TestConfig.class)

@RunWith(SpringRunner.class)
@SpringBootTest(
        classes = Application.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@WithDefaultTestUser
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
    @LocalServerPort
    int port;

    @PostConstruct
    public void setPort() {
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
    protected void waitForCondition(String failMessage, Supplier<Boolean> condition) throws InterruptedException {
        int numberAttempt = 0;
        while (true) {
            numberAttempt++;

            boolean res;

            try {
                res = condition.get();
            } catch (Throwable t) {
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

//    @SpringBootApplication(
//            scanBasePackageClasses = {
//                    TestConfig.class,
//                    TMService.class,
//                    PollableTaskService.class,
//                    ObjectMapper.class,
//                    PollableTaskBlobStorage.class,
//                    StructuredBlobStorage.class,
//                    RepositoryService.class,
//                    LocaleService.class,
//                    DropExporterConfig.class,
//                    AssetService.class,
//                    AssetExtractionService.class,
//                    AssetExtractor.class,
//                    AssetPathToFilterConfigMapper.class,
//                    XliffUtils.class,
//                    WordCountService.class,
//                    AuditorAwareImpl.class,
//                    QuartzPollableTaskScheduler.class,
//                    TextUnitUtils.class,
//                    XliffDataFactory.class,
//                    BootstrapConfig.class,
//                    BoxAPIConnectionProvider.class,
//                    MustacheTemplateEngine.class,
//                    DateTimeUtils.class,
//                    AsyncConfig.class,
//
//            },
//            exclude = QuartzAutoConfiguration.class)
//    @EnableJpaRepositories(basePackageClasses = WordCountServiceTest.class)
//    @EntityScan(basePackageClasses = Repository.class)
//    @Configuration
    public static class TestConfig {

        @Bean("bla")
        public String bla() {
            return "bla";
        }

        @Bean
        @Primary
        public ObjectMapper objectMapper() {
            return new ObjectMapper();
        }

        @Bean(name = "fail_on_unknown_properties_false")
        public ObjectMapper getObjectMapperFailOnUnknownPropertiesFalse() {
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            return objectMapper;
        }

        @Bean
        public RetryTemplate retryTemplate() {
            SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
            retryPolicy.setMaxAttempts(5);

            ExponentialRandomBackOffPolicy exponentialRandomBackOffPolicy = new ExponentialRandomBackOffPolicy();
            exponentialRandomBackOffPolicy.setInitialInterval(10);
            exponentialRandomBackOffPolicy.setMultiplier(3);
            exponentialRandomBackOffPolicy.setMaxInterval(5000);

            RetryTemplate template = new RetryTemplate();
            template.setRetryPolicy(retryPolicy);
            template.setBackOffPolicy(exponentialRandomBackOffPolicy);
            template.setThrowLastExceptionOnExhausted(true);

            return template;
        }

    }
}

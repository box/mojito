package com.box.l10n.mojito.rest;

import com.box.l10n.mojito.Application;
import com.box.l10n.mojito.apiclient.ApiClient;
import com.box.l10n.mojito.apiclient.LocaleClient;
import com.box.l10n.mojito.apiclient.exception.LocaleNotFoundException;
import com.box.l10n.mojito.apiclient.model.RepositoryLocale;
import com.box.l10n.mojito.factory.XliffDataFactory;
import com.box.l10n.mojito.rest.annotation.WithDefaultTestUser;
import com.box.l10n.mojito.resttemplate.AuthenticatedRestTemplate;
import com.box.l10n.mojito.resttemplate.ResttemplateConfig;
import com.box.l10n.mojito.xml.XmlParsingConfiguration;
import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import org.junit.Assert;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Base class for WS integration tests. Creates an in-memory instance of tomcat and setup the REST
 * client to use the port that was bound during container initialization.
 *
 * @author jaurambault
 */
@RunWith(SpringRunner.class)
@SpringBootTest(
    classes = Application.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@WithDefaultTestUser
public class WSTestBase {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(WSTestBase.class);

  @Autowired protected AuthenticatedRestTemplate authenticatedRestTemplate;

  @Autowired protected XliffDataFactory xliffDataFactory;

  @Autowired protected LocaleClient localeClient;

  @Autowired ResttemplateConfig resttemplateConfig;

  @LocalServerPort int port;

  @Autowired ApiClient apiClient;

  @PostConstruct
  public void setPort() {
    logger.debug("Saving port number = {}", port);
    resttemplateConfig.setPort(port);
    this.apiClient.setBasePath(
        String.format(
            "%s://%s:%d",
            this.resttemplateConfig.getScheme(),
            this.resttemplateConfig.getHost(),
            this.resttemplateConfig.getPort()));

    XmlParsingConfiguration.disableXPathLimits();
  }

  /**
   * Returns a list of {@link RepositoryLocale}s whose locales correspond to the given tags
   *
   * @param bcp47Tags
   * @return
   */
  protected List<RepositoryLocale> getRepositoryLocales(List<String> bcp47Tags) {

    List<RepositoryLocale> repositoryLocales = new ArrayList<>();

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
  protected void waitForCondition(String failMessage, Supplier<Boolean> condition)
      throws InterruptedException {
    waitForCondition(failMessage, condition, 30, 100);
  }

  protected void waitForCondition(
      String failMessage,
      Supplier<Boolean> condition,
      int maxNumberAttempt,
      int milisecondSleepTime)
      throws InterruptedException {
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
      } else if (numberAttempt > maxNumberAttempt) {
        Assert.fail(failMessage);
      }
      Thread.sleep(numberAttempt * milisecondSleepTime);
    }
  }
}

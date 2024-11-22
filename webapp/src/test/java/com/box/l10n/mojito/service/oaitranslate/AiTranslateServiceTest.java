package com.box.l10n.mojito.service.oaitranslate;

import com.box.l10n.mojito.service.assetExtraction.ServiceTestBase;
import com.box.l10n.mojito.service.repository.RepositoryNameAlreadyUsedException;
import com.box.l10n.mojito.service.repository.RepositoryService;
import com.box.l10n.mojito.service.tm.TMTestData;
import com.box.l10n.mojito.test.TestIdWatcher;
import java.util.concurrent.ExecutionException;
import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class AiTranslateServiceTest extends ServiceTestBase {

  static Logger logger = LoggerFactory.getLogger(AiTranslateServiceTest.class);

  @Rule public TestIdWatcher testIdWatcher = new TestIdWatcher();

  @Autowired AiTranslateService aiTranslateService;

  @Autowired AiTranslateConfigurationProperties aiTranslateConfigurationProperties;

  @Autowired RepositoryService repositoryService;

  @Test
  public void aiTranslateBatch() throws ExecutionException, InterruptedException {
    Assume.assumeNotNull(aiTranslateConfigurationProperties.getOpenaiClientToken());

    TMTestData tmTestData = new TMTestData(testIdWatcher);
    aiTranslateService
        .aiTranslateAsync(
            new AiTranslateService.AiTranslateInput(
                tmTestData.repository.getName(), null, 100, null, true))
        .get();
  }

  @Test
  public void aiTranslateNoBatch()
      throws ExecutionException, InterruptedException, RepositoryNameAlreadyUsedException {
    Assume.assumeNotNull(aiTranslateConfigurationProperties.getOpenaiClientToken());

    TMTestData tmTestData = new TMTestData(testIdWatcher);

    aiTranslateService
        .aiTranslateAsync(
            new AiTranslateService.AiTranslateInput(
                tmTestData.repository.getName(), null, 100, null, false))
        .get();
  }
}

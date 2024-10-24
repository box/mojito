package com.box.l10n.mojito.service.oaitranslate;

import com.box.l10n.mojito.service.assetExtraction.ServiceTestBase;
import com.box.l10n.mojito.service.tm.TMTestData;
import com.box.l10n.mojito.test.TestIdWatcher;
import java.util.concurrent.ExecutionException;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class AiTranslateServiceTest extends ServiceTestBase {

  static Logger logger = LoggerFactory.getLogger(AiTranslateServiceTest.class);

  @Rule public TestIdWatcher testIdWatcher = new TestIdWatcher();

  @Autowired AiTranslateService aiTranslateService;

  @Test
  public void aiTranslate() throws ExecutionException, InterruptedException {
    TMTestData tmTestData = new TMTestData(testIdWatcher);
    aiTranslateService
        .aiTranslateAsync(
            new AiTranslateService.AiTranslateInput(tmTestData.repository.getName(), null, 100))
        .get();
  }
}

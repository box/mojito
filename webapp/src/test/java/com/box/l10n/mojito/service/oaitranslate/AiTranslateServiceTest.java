package com.box.l10n.mojito.service.oaitranslate;

import static org.junit.jupiter.api.Assertions.*;

import com.box.l10n.mojito.entity.TMTextUnitVariant;
import com.box.l10n.mojito.openai.OpenAIClient;
import com.box.l10n.mojito.service.assetExtraction.ServiceTestBase;
import com.box.l10n.mojito.service.repository.RepositoryNameAlreadyUsedException;
import com.box.l10n.mojito.service.repository.RepositoryService;
import com.box.l10n.mojito.service.tm.TMTestData;
import com.box.l10n.mojito.service.tm.search.StatusFilter;
import com.box.l10n.mojito.test.TestIdWatcher;
import java.time.Duration;
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
  public void connectionTest() throws ExecutionException, InterruptedException {
    Assume.assumeNotNull(aiTranslateConfigurationProperties.getOpenaiClientToken());

    String model = aiTranslateConfigurationProperties.getModelName();

    var request =
        OpenAIClient.ResponsesRequest.builder()
            .model(model)
            .instructions("Reply with exactly the single word: pong")
            .addUserText("ping")
            .build();

    var response =
        aiTranslateService.openAIClient.getResponses(request, Duration.ofSeconds(60)).get();

    var responseText = response.outputText().trim();
    assertEquals("pong", responseText, "Should receive response from the model");
  }

  @Test
  public void aiTranslateBatch() throws ExecutionException, InterruptedException {
    Assume.assumeNotNull(aiTranslateConfigurationProperties.getOpenaiClientToken());

    TMTestData tmTestData = new TMTestData(testIdWatcher);
    aiTranslateService
        .aiTranslateAsync(
            new AiTranslateService.AiTranslateInput(
                tmTestData.repository.getName(),
                null,
                100,
                null,
                true,
                null,
                null,
                null,
                AiTranslateType.WITH_REVIEW.name(),
                StatusFilter.FOR_TRANSLATION.name(),
                TMTextUnitVariant.Status.REVIEW_NEEDED.name(),
                null,
                null,
                null,
                null,
                null,
                false,
                false,
                false,
                false,
                null))
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
                tmTestData.repository.getName(),
                null,
                100,
                null,
                false,
                null,
                null,
                null,
                AiTranslateType.WITH_REVIEW.name(),
                StatusFilter.FOR_TRANSLATION.name(),
                TMTextUnitVariant.Status.REVIEW_NEEDED.name(),
                null,
                null,
                null,
                null,
                null,
                false,
                false,
                false,
                false,
                null))
        .get();
  }
}

package com.box.l10n.mojito.service.ai.openai;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.box.l10n.mojito.entity.AIPrompt;
import com.box.l10n.mojito.entity.AIPromptContextMessage;
import com.box.l10n.mojito.entity.AIPromptType;
import com.box.l10n.mojito.entity.PluralForm;
import com.box.l10n.mojito.entity.PromptType;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.TMTextUnit;
import com.box.l10n.mojito.json.ObjectMapper;
import com.box.l10n.mojito.okapi.extractor.AssetExtractorTextUnit;
import com.box.l10n.mojito.openai.OpenAIClient;
import com.box.l10n.mojito.rest.ai.AICheckRequest;
import com.box.l10n.mojito.rest.ai.AICheckResponse;
import com.box.l10n.mojito.rest.ai.AIException;
import com.box.l10n.mojito.service.ai.AIStringCheckRepository;
import com.box.l10n.mojito.service.ai.LLMPromptService;
import com.box.l10n.mojito.service.repository.RepositoryRepository;
import com.box.l10n.mojito.service.thirdparty.smartling.glossary.GlossaryTerm;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

class OpenAILLMServiceTest {

  @Mock OpenAIClient openAIClient;

  @Mock RepositoryRepository repositoryRepository;

  @Mock AIStringCheckRepository aiStringCheckRepository;

  @Mock LLMPromptService promptService;

  @Mock MeterRegistry meterRegistry;

  @Spy ObjectMapper objectMapper;

  @Captor private ArgumentCaptor<OpenAIClient.ChatCompletionsRequest> requestCaptor;

  @InjectMocks OpenAILLMService openAILLMService;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    openAILLMService.persistResults = true;
    openAILLMService.retryMaxAttempts = 1;
    openAILLMService.retryMinDurationSeconds = 0;
    openAILLMService.retryMaxBackoffDurationSeconds = 0;
    openAILLMService.init();
    when(meterRegistry.counter(anyString(), any(String[].class)))
        .thenReturn(mock(io.micrometer.core.instrument.Counter.class));
    when(meterRegistry.counter(anyString(), any(Iterable.class)))
        .thenReturn(mock(io.micrometer.core.instrument.Counter.class));
  }

  @Test
  void executeAIChecksSuccessTest() {
    AIPrompt prompt = new AIPrompt();
    prompt.setId(1L);
    prompt.setUserPrompt("Check strings for spelling");
    prompt.setModelName("gtp-3.5-turbo");
    prompt.setPromptTemperature(0.0F);
    prompt.setContextMessages(new ArrayList<>());
    List<AIPrompt> prompts = List.of(prompt);
    AssetExtractorTextUnit assetExtractorTextUnit = new AssetExtractorTextUnit();
    assetExtractorTextUnit.setSource("A test string");
    assetExtractorTextUnit.setName("A test string --- A test context");
    assetExtractorTextUnit.setComments("A test comment");
    List<AssetExtractorTextUnit> textUnits = List.of(assetExtractorTextUnit);
    AICheckRequest AICheckRequest = new AICheckRequest();
    AICheckRequest.setRepositoryName("testRepo");
    AICheckRequest.setTextUnits(textUnits);
    Repository repository = new Repository();
    repository.setName("testRepo");
    repository.setId(1L);
    when(repositoryRepository.findByName("testRepo")).thenReturn(repository);
    when(promptService.getPromptsByRepositoryAndPromptType(
            repository, PromptType.SOURCE_STRING_CHECKER))
        .thenReturn(prompts);
    List<OpenAIClient.ChatCompletionsResponse.Choice> choices =
        List.of(
            new OpenAIClient.ChatCompletionsResponse.Choice(
                0,
                new OpenAIClient.ChatCompletionsResponse.Choice.Message(
                    "test", "{\"success\": true, \"suggestedFix\": \"\"}"),
                null));
    OpenAIClient.ChatCompletionsResponse chatCompletionsResponse =
        new OpenAIClient.ChatCompletionsResponse(null, null, null, null, choices, null, null);
    CompletableFuture<OpenAIClient.ChatCompletionsResponse> futureResponse =
        CompletableFuture.completedFuture(chatCompletionsResponse);
    when(openAIClient.getChatCompletions(any(OpenAIClient.ChatCompletionsRequest.class)))
        .thenReturn(futureResponse);

    AICheckResponse response = openAILLMService.executeAIChecks(AICheckRequest);
    assertNotNull(response);
    assertEquals(1, response.getResults().size());
    assertTrue(response.getResults().containsKey("A test string"));
    assertTrue(response.getResults().get("A test string").getFirst().isSuccess());
    verify(aiStringCheckRepository, times(1)).save(any());
    verify(meterRegistry, times(1))
        .counter("OpenAILLMService.checks.result", "success", "true", "repository", "testRepo");
  }

  @Test
  void executeAIChecksFailureTest() {
    AIPrompt prompt = new AIPrompt();
    prompt.setId(1L);
    prompt.setUserPrompt("Check strings for spelling");
    prompt.setModelName("gtp-3.5-turbo");
    prompt.setPromptTemperature(0.0F);
    prompt.setContextMessages(new ArrayList<>());
    List<AIPrompt> prompts = List.of(prompt);
    AssetExtractorTextUnit assetExtractorTextUnit = new AssetExtractorTextUnit();
    assetExtractorTextUnit.setSource("A tst string");
    assetExtractorTextUnit.setName("A tst string --- A test context");
    assetExtractorTextUnit.setComments("A test comment");
    List<AssetExtractorTextUnit> textUnits = List.of(assetExtractorTextUnit);
    AICheckRequest AICheckRequest = new AICheckRequest();
    AICheckRequest.setRepositoryName("testRepo");
    AICheckRequest.setTextUnits(textUnits);
    Repository repository = new Repository();
    repository.setName("testRepo");
    repository.setId(1L);
    when(repositoryRepository.findByName("testRepo")).thenReturn(repository);
    when(promptService.getPromptsByRepositoryAndPromptType(
            repository, PromptType.SOURCE_STRING_CHECKER))
        .thenReturn(prompts);
    List<OpenAIClient.ChatCompletionsResponse.Choice> choices =
        List.of(
            new OpenAIClient.ChatCompletionsResponse.Choice(
                0,
                new OpenAIClient.ChatCompletionsResponse.Choice.Message(
                    "test",
                    "{\"success\": false, \"suggestedFix\": \"The word test is spelt wrong\"}"),
                null));
    OpenAIClient.ChatCompletionsResponse chatCompletionsResponse =
        new OpenAIClient.ChatCompletionsResponse(null, null, null, null, choices, null, null);
    CompletableFuture<OpenAIClient.ChatCompletionsResponse> futureResponse =
        CompletableFuture.completedFuture(chatCompletionsResponse);
    when(openAIClient.getChatCompletions(any(OpenAIClient.ChatCompletionsRequest.class)))
        .thenReturn(futureResponse);

    AICheckResponse response = openAILLMService.executeAIChecks(AICheckRequest);
    assertNotNull(response);
    assertEquals(1, response.getResults().size());
    assertTrue(response.getResults().containsKey("A tst string"));
    assertFalse(response.getResults().get("A tst string").getFirst().isSuccess());
    assertEquals(
        "The word test is spelt wrong",
        response.getResults().get("A tst string").getFirst().getSuggestedFix());
    verify(aiStringCheckRepository, times(1)).save(any());
    verify(meterRegistry, times(1))
        .counter("OpenAILLMService.checks.result", "success", "false", "repository", "testRepo");
  }

  @Test
  void testJsonSerializationError() {
    AIPrompt prompt = new AIPrompt();
    prompt.setId(1L);
    prompt.setUserPrompt("Check strings for spelling");
    prompt.setModelName("gtp-3.5-turbo");
    prompt.setPromptTemperature(0.0F);
    prompt.setContextMessages(new ArrayList<>());
    List<AIPrompt> prompts = List.of(prompt);
    AssetExtractorTextUnit assetExtractorTextUnit = new AssetExtractorTextUnit();
    assetExtractorTextUnit.setSource("A test string");
    assetExtractorTextUnit.setName("A test string --- A test context");
    assetExtractorTextUnit.setComments("A test comment");
    List<AssetExtractorTextUnit> textUnits = List.of(assetExtractorTextUnit);
    AICheckRequest AICheckRequest = new AICheckRequest();
    AICheckRequest.setRepositoryName("testRepo");
    AICheckRequest.setTextUnits(textUnits);
    Repository repository = new Repository();
    repository.setName("testRepo");
    repository.setId(1L);
    when(repositoryRepository.findByName("testRepo")).thenReturn(repository);
    when(promptService.getPromptsByRepositoryAndPromptType(
            repository, PromptType.SOURCE_STRING_CHECKER))
        .thenReturn(prompts);
    List<OpenAIClient.ChatCompletionsResponse.Choice> choices =
        List.of(
            new OpenAIClient.ChatCompletionsResponse.Choice(
                0,
                new OpenAIClient.ChatCompletionsResponse.Choice.Message(
                    "test", "\"success\": true, \"suggestedFix\": \"\""),
                null));
    OpenAIClient.ChatCompletionsResponse chatCompletionsResponse =
        new OpenAIClient.ChatCompletionsResponse(null, null, null, null, choices, null, null);
    CompletableFuture<OpenAIClient.ChatCompletionsResponse> futureResponse =
        CompletableFuture.completedFuture(chatCompletionsResponse);
    when(openAIClient.getChatCompletions(any(OpenAIClient.ChatCompletionsRequest.class)))
        .thenReturn(futureResponse);

    AICheckResponse result = openAILLMService.executeAIChecks(AICheckRequest);

    assertTrue(result.getResults().get("A test string").getFirst().isSuccess());
    assertEquals(
        result.getResults().get("A test string").getFirst().getSuggestedFix(),
        "Check skipped as error parsing response from OpenAI.");
    verify(aiStringCheckRepository, times(1)).save(any());
    verify(meterRegistry, times(1))
        .counter("OpenAILLMService.checks.parse.error", "repository", "testRepo");
  }

  @Test
  void testNoPromptsSuccessResult() {
    List<AIPrompt> prompts = new ArrayList<>();
    AssetExtractorTextUnit assetExtractorTextUnit = new AssetExtractorTextUnit();
    assetExtractorTextUnit.setSource("A test string");
    assetExtractorTextUnit.setName("A test string --- A test context");
    assetExtractorTextUnit.setComments("A test comment");
    List<AssetExtractorTextUnit> textUnits = List.of(assetExtractorTextUnit);
    AICheckRequest AICheckRequest = new AICheckRequest();
    AICheckRequest.setRepositoryName("testRepo");
    AICheckRequest.setTextUnits(textUnits);
    Repository repository = new Repository();
    repository.setName("testRepo");
    repository.setId(1L);
    when(repositoryRepository.findByName("testRepo")).thenReturn(repository);
    when(promptService.getPromptsByRepositoryAndPromptType(
            repository, PromptType.SOURCE_STRING_CHECKER))
        .thenReturn(prompts);
    List<OpenAIClient.ChatCompletionsResponse.Choice> choices =
        List.of(
            new OpenAIClient.ChatCompletionsResponse.Choice(
                0,
                new OpenAIClient.ChatCompletionsResponse.Choice.Message(
                    "test", "{\"success\": true, \"suggestedFix\": \"\"}"),
                null));
    OpenAIClient.ChatCompletionsResponse chatCompletionsResponse =
        new OpenAIClient.ChatCompletionsResponse(null, null, null, null, choices, null, null);
    CompletableFuture<OpenAIClient.ChatCompletionsResponse> futureResponse =
        CompletableFuture.completedFuture(chatCompletionsResponse);
    when(openAIClient.getChatCompletions(any(OpenAIClient.ChatCompletionsRequest.class)))
        .thenReturn(futureResponse);

    AICheckResponse response = openAILLMService.executeAIChecks(AICheckRequest);
    assertNotNull(response);
    assertEquals(1, response.getResults().size());
    assertTrue(response.getResults().containsKey("A test string"));
    assertTrue(response.getResults().get("A test string").getFirst().isSuccess());
    assertEquals(
        "No prompts found for repository: testRepo, skipping check.",
        response.getResults().get("A test string").getFirst().getSuggestedFix());
    verify(aiStringCheckRepository, times(0)).save(any());
  }

  @Test
  public void testNoResultsPersistence() {
    AIPrompt prompt = new AIPrompt();
    prompt.setId(1L);
    prompt.setUserPrompt("Check strings for spelling");
    prompt.setModelName("gtp-3.5-turbo");
    prompt.setPromptTemperature(0.0F);
    prompt.setContextMessages(new ArrayList<>());
    List<AIPrompt> prompts = List.of(prompt);
    AssetExtractorTextUnit assetExtractorTextUnit = new AssetExtractorTextUnit();
    assetExtractorTextUnit.setSource("A test string");
    assetExtractorTextUnit.setName("A test string --- A test context");
    assetExtractorTextUnit.setComments("A test comment");
    List<AssetExtractorTextUnit> textUnits = List.of(assetExtractorTextUnit);
    AICheckRequest AICheckRequest = new AICheckRequest();
    AICheckRequest.setRepositoryName("testRepo");
    AICheckRequest.setTextUnits(textUnits);
    Repository repository = new Repository();
    repository.setId(1L);
    when(repositoryRepository.findByName("testRepo")).thenReturn(repository);
    when(promptService.getPromptsByRepositoryAndPromptType(
            repository, PromptType.SOURCE_STRING_CHECKER))
        .thenReturn(prompts);
    List<OpenAIClient.ChatCompletionsResponse.Choice> choices =
        List.of(
            new OpenAIClient.ChatCompletionsResponse.Choice(
                0,
                new OpenAIClient.ChatCompletionsResponse.Choice.Message(
                    "test", "{\"success\": true, \"suggestedFix\": \"\"}"),
                null));
    OpenAIClient.ChatCompletionsResponse chatCompletionsResponse =
        new OpenAIClient.ChatCompletionsResponse(null, null, null, null, choices, null, null);
    CompletableFuture<OpenAIClient.ChatCompletionsResponse> futureResponse =
        CompletableFuture.completedFuture(chatCompletionsResponse);
    when(openAIClient.getChatCompletions(any(OpenAIClient.ChatCompletionsRequest.class)))
        .thenReturn(futureResponse);
    openAILLMService.persistResults = false;

    AICheckResponse response = openAILLMService.executeAIChecks(AICheckRequest);
    assertNotNull(response);
    assertEquals(1, response.getResults().size());
    assertTrue(response.getResults().containsKey("A test string"));
    assertTrue(response.getResults().get("A test string").getFirst().isSuccess());
    verify(aiStringCheckRepository, times(0)).save(any());
  }

  @Test
  public void testPromptContextMessagesIncluded() {

    AIPrompt prompt = new AIPrompt();
    prompt.setId(1L);
    prompt.setUserPrompt("Check strings for spelling");
    prompt.setModelName("gtp-3.5-turbo");
    prompt.setPromptTemperature(0.0F);
    AIPromptType promptType = new AIPromptType();
    promptType.setName("SOURCE_STRING_CHECKER");
    prompt.setPromptType(promptType);

    AIPromptContextMessage testSystemContextMessage = new AIPromptContextMessage();
    testSystemContextMessage.setContent("A test system context message");
    testSystemContextMessage.setMessageType("system");
    testSystemContextMessage.setOrderIndex(1);
    testSystemContextMessage.setAiPrompt(prompt);

    AIPromptContextMessage testUserContextMessage = new AIPromptContextMessage();
    testUserContextMessage.setContent("A test user context message");
    testUserContextMessage.setMessageType("user");
    testUserContextMessage.setOrderIndex(2);
    testUserContextMessage.setAiPrompt(prompt);

    AIPromptContextMessage testAssistantContextMessage = new AIPromptContextMessage();
    testAssistantContextMessage.setContent("A test assistant context message");
    testAssistantContextMessage.setMessageType("assistant");
    testAssistantContextMessage.setOrderIndex(3);
    testAssistantContextMessage.setAiPrompt(prompt);

    prompt.setContextMessages(
        List.of(testSystemContextMessage, testUserContextMessage, testAssistantContextMessage));
    List<AIPrompt> prompts = List.of(prompt);

    AssetExtractorTextUnit assetExtractorTextUnit = new AssetExtractorTextUnit();
    assetExtractorTextUnit.setSource("A test string");
    assetExtractorTextUnit.setName("A test string --- A test context");
    assetExtractorTextUnit.setComments("A test comment");
    List<AssetExtractorTextUnit> textUnits = List.of(assetExtractorTextUnit);
    AICheckRequest AICheckRequest = new AICheckRequest();
    AICheckRequest.setRepositoryName("testRepo");
    AICheckRequest.setTextUnits(textUnits);
    Repository repository = new Repository();
    repository.setId(1L);
    when(repositoryRepository.findByName("testRepo")).thenReturn(repository);
    when(promptService.getPromptsByRepositoryAndPromptType(
            repository, PromptType.SOURCE_STRING_CHECKER))
        .thenReturn(prompts);
    List<OpenAIClient.ChatCompletionsResponse.Choice> choices =
        List.of(
            new OpenAIClient.ChatCompletionsResponse.Choice(
                0,
                new OpenAIClient.ChatCompletionsResponse.Choice.Message(
                    "test", "{\"success\": true, \"suggestedFix\": \"\"}"),
                null));
    OpenAIClient.ChatCompletionsResponse chatCompletionsResponse =
        new OpenAIClient.ChatCompletionsResponse(null, null, null, null, choices, null, null);
    CompletableFuture<OpenAIClient.ChatCompletionsResponse> futureResponse =
        CompletableFuture.completedFuture(chatCompletionsResponse);
    when(openAIClient.getChatCompletions(requestCaptor.capture())).thenReturn(futureResponse);

    openAILLMService.executeAIChecks(AICheckRequest);

    OpenAIClient.ChatCompletionsRequest request = requestCaptor.getValue();
    assertEquals(4, request.messages().size());
    assertEquals("A test system context message", request.messages().get(0).content());
    assertEquals("system", request.messages().get(0).role());
    assertEquals("A test user context message", request.messages().get(1).content());
    assertEquals("user", request.messages().get(1).role());
    assertEquals("A test assistant context message", request.messages().get(2).content());
    assertEquals("assistant", request.messages().get(2).role());
    assertEquals("Check strings for spelling", request.messages().get(3).content());
    assertEquals("user", request.messages().get(3).role());
  }

  @Test
  public void testSourceOnlyCheckedOnce() {
    AIPrompt prompt = new AIPrompt();
    prompt.setId(1L);
    prompt.setUserPrompt("Check strings for spelling");
    prompt.setModelName("gtp-3.5-turbo");
    prompt.setPromptTemperature(0.0F);
    prompt.setContextMessages(new ArrayList<>());
    List<AIPrompt> prompts = List.of(prompt);
    AssetExtractorTextUnit assetExtractorTextUnit = new AssetExtractorTextUnit();
    assetExtractorTextUnit.setSource("A test string");
    assetExtractorTextUnit.setName("A test string --- A test context_one");
    assetExtractorTextUnit.setComments("A test comment");
    AssetExtractorTextUnit assetExtractorTextUnit2 = new AssetExtractorTextUnit();
    assetExtractorTextUnit2.setSource("A test string");
    assetExtractorTextUnit2.setName("A test string --- A test context_many");
    assetExtractorTextUnit2.setComments("A test comment");
    List<AssetExtractorTextUnit> textUnits =
        List.of(assetExtractorTextUnit, assetExtractorTextUnit2);
    AICheckRequest AICheckRequest = new AICheckRequest();
    AICheckRequest.setRepositoryName("testRepo");
    AICheckRequest.setTextUnits(textUnits);
    Repository repository = new Repository();
    repository.setName("testRepo");
    repository.setId(1L);
    when(repositoryRepository.findByName("testRepo")).thenReturn(repository);
    when(promptService.getPromptsByRepositoryAndPromptType(
            repository, PromptType.SOURCE_STRING_CHECKER))
        .thenReturn(prompts);
    List<OpenAIClient.ChatCompletionsResponse.Choice> choices =
        List.of(
            new OpenAIClient.ChatCompletionsResponse.Choice(
                0,
                new OpenAIClient.ChatCompletionsResponse.Choice.Message(
                    "test", "{\"success\": true, \"suggestedFix\": \"\"}"),
                null));
    OpenAIClient.ChatCompletionsResponse chatCompletionsResponse =
        new OpenAIClient.ChatCompletionsResponse(null, null, null, null, choices, null, null);
    CompletableFuture<OpenAIClient.ChatCompletionsResponse> futureResponse =
        CompletableFuture.completedFuture(chatCompletionsResponse);
    when(openAIClient.getChatCompletions(any(OpenAIClient.ChatCompletionsRequest.class)))
        .thenReturn(futureResponse);

    AICheckResponse response = openAILLMService.executeAIChecks(AICheckRequest);
    assertNotNull(response);
    assertEquals(1, response.getResults().size());
    assertTrue(response.getResults().containsKey("A test string"));
    assertTrue(response.getResults().get("A test string").getFirst().isSuccess());
    verify(aiStringCheckRepository, times(1)).save(any());
    verify(meterRegistry, times(1))
        .counter("OpenAILLMService.checks.result", "success", "true", "repository", "testRepo");
  }

  @Test
  void testTranslateSuccess() {
    TMTextUnit tmTextUnit = new TMTextUnit();
    tmTextUnit.setId(1L);
    tmTextUnit.setContent("Hello");
    tmTextUnit.setComment("Greeting");

    AIPrompt prompt = new AIPrompt();
    prompt.setId(1L);
    prompt.setSystemPrompt("Translate the following text:");
    prompt.setUserPrompt("Translate this text to French:");
    prompt.setModelName("gtp-3.5-turbo");
    prompt.setPromptTemperature(0.0F);
    prompt.setContextMessages(new ArrayList<>());

    OpenAIClient.ChatCompletionsResponse.Choice choice =
        new OpenAIClient.ChatCompletionsResponse.Choice(
            0, new OpenAIClient.ChatCompletionsResponse.Choice.Message("test", "Bonjour"), "stop");
    OpenAIClient.ChatCompletionsResponse chatCompletionsResponse =
        new OpenAIClient.ChatCompletionsResponse(
            null, null, null, null, List.of(choice), null, null);
    CompletableFuture<OpenAIClient.ChatCompletionsResponse> futureResponse =
        CompletableFuture.completedFuture(chatCompletionsResponse);
    when(openAIClient.getChatCompletions(any(OpenAIClient.ChatCompletionsRequest.class)))
        .thenReturn(futureResponse);

    String translation = openAILLMService.translate(tmTextUnit, "en", "fr", prompt);
    assertEquals("Bonjour", translation);
  }

  @Test
  void testTranslateResponseNonStopFinishReason() {
    TMTextUnit tmTextUnit = new TMTextUnit();
    tmTextUnit.setId(1L);
    tmTextUnit.setContent("Hello");
    tmTextUnit.setComment("Greeting");

    AIPrompt prompt = new AIPrompt();
    prompt.setId(1L);
    prompt.setSystemPrompt("Translate the following text:");
    prompt.setUserPrompt("Translate this text to French:");
    prompt.setModelName("gtp-3.5-turbo");
    prompt.setPromptTemperature(0.0F);
    prompt.setContextMessages(new ArrayList<>());

    OpenAIClient.ChatCompletionsResponse.Choice choice =
        new OpenAIClient.ChatCompletionsResponse.Choice(
            0,
            new OpenAIClient.ChatCompletionsResponse.Choice.Message("test", "Bonjour"),
            "length");
    OpenAIClient.ChatCompletionsResponse chatCompletionsResponse =
        new OpenAIClient.ChatCompletionsResponse(
            null, null, null, null, List.of(choice), null, null);
    CompletableFuture<OpenAIClient.ChatCompletionsResponse> futureResponse =
        CompletableFuture.completedFuture(chatCompletionsResponse);
    when(openAIClient.getChatCompletions(any(OpenAIClient.ChatCompletionsRequest.class)))
        .thenReturn(futureResponse);

    assertThrows(
        AIException.class, () -> openAILLMService.translate(tmTextUnit, "en", "fr", prompt));
  }

  @Test
  void testTranslateStripTranslationFromJsonKey() {
    TMTextUnit tmTextUnit = new TMTextUnit();
    tmTextUnit.setId(1L);
    tmTextUnit.setContent("Hello");
    tmTextUnit.setComment("Greeting");

    AIPrompt prompt = new AIPrompt();
    prompt.setId(1L);
    prompt.setSystemPrompt("Translate the following text:");
    prompt.setUserPrompt("Translate this text to French:");
    prompt.setModelName("gtp-3.5-turbo");
    prompt.setPromptTemperature(0.0F);
    prompt.setContextMessages(new ArrayList<>());
    prompt.setJsonResponseKey("translation");
    prompt.setJsonResponse(true);

    OpenAIClient.ChatCompletionsResponse.Choice choice =
        new OpenAIClient.ChatCompletionsResponse.Choice(
            0,
            new OpenAIClient.ChatCompletionsResponse.Choice.Message(
                "test", "{\"translation\": \"Bonjour\"}"),
            "stop");
    OpenAIClient.ChatCompletionsResponse chatCompletionsResponse =
        new OpenAIClient.ChatCompletionsResponse(
            null, null, null, null, List.of(choice), null, null);
    CompletableFuture<OpenAIClient.ChatCompletionsResponse> futureResponse =
        CompletableFuture.completedFuture(chatCompletionsResponse);
    when(openAIClient.getChatCompletions(any(OpenAIClient.ChatCompletionsRequest.class)))
        .thenReturn(futureResponse);

    String translation = openAILLMService.translate(tmTextUnit, "en", "fr", prompt);
    assertEquals("Bonjour", translation);
  }

  @Test
  void testTranslateStripTranslationFromInvalidJson() {
    TMTextUnit tmTextUnit = new TMTextUnit();
    tmTextUnit.setId(1L);
    tmTextUnit.setContent("Hello");
    tmTextUnit.setComment("Greeting");

    AIPrompt prompt = new AIPrompt();
    prompt.setId(1L);
    prompt.setSystemPrompt("Translate the following text:");
    prompt.setUserPrompt("Translate this text to French:");
    prompt.setModelName("gtp-3.5-turbo");
    prompt.setPromptTemperature(0.0F);
    prompt.setContextMessages(new ArrayList<>());
    prompt.setJsonResponseKey("translation");
    prompt.setJsonResponse(true);

    OpenAIClient.ChatCompletionsResponse.Choice choice =
        new OpenAIClient.ChatCompletionsResponse.Choice(
            0,
            new OpenAIClient.ChatCompletionsResponse.Choice.Message(
                "test", "invalid: {\"translation\": \"Bonjour\"}"),
            null);
    OpenAIClient.ChatCompletionsResponse chatCompletionsResponse =
        new OpenAIClient.ChatCompletionsResponse(
            null, null, null, null, List.of(choice), null, null);
    CompletableFuture<OpenAIClient.ChatCompletionsResponse> futureResponse =
        CompletableFuture.completedFuture(chatCompletionsResponse);
    when(openAIClient.getChatCompletions(any(OpenAIClient.ChatCompletionsRequest.class)))
        .thenReturn(futureResponse);

    assertThrows(Exception.class, () -> openAILLMService.translate(tmTextUnit, "en", "fr", prompt));
  }

  @Test
  void testTranslateError() {
    TMTextUnit tmTextUnit = new TMTextUnit();
    tmTextUnit.setId(1L);
    tmTextUnit.setContent("Hello");
    tmTextUnit.setComment("Greeting");

    AIPrompt prompt = new AIPrompt();
    prompt.setId(1L);
    prompt.setSystemPrompt("Translate the following text:");
    prompt.setUserPrompt("Translate this text to French:");
    prompt.setModelName("gtp-3.5-turbo");
    prompt.setPromptTemperature(0.0F);
    prompt.setContextMessages(new ArrayList<>());

    when(openAIClient.getChatCompletions(any(OpenAIClient.ChatCompletionsRequest.class)))
        .thenThrow(new RuntimeException("OpenAI service error"));

    assertThrows(Exception.class, () -> openAILLMService.translate(tmTextUnit, "en", "fr", prompt));
  }

  @Test
  void testPromptTemplatingAllValuesInjected() {
    String promptText =
        """
         Translate the following source string from [mojito_source_locale] to [mojito_target_locale]:
         Source string: [mojito_source_string]
         Glossary matches: [mojito_glossary_term_matches]
         {{optional: Comment: [mojito_comment_string]}}
         {{optional: Context: [mojito_context_string]}}
         {{optional: Plural form: [mojito_plural_form]}}
         """;

    PluralForm one = new PluralForm();
    one.setName("one");
    TMTextUnit tmTextUnit = new TMTextUnit();
    tmTextUnit.setId(1L);
    tmTextUnit.setContent("Hello");
    tmTextUnit.setComment("A friendly greeting");
    tmTextUnit.setName("Hello --- some.id");
    tmTextUnit.setPluralForm(one);
    String prompt =
        openAILLMService.getTranslationFormattedPrompt(
            promptText, tmTextUnit, "en", "fr", Collections.emptyList());
    assertEquals(
        """
         Translate the following source string from en to fr:
         Source string: Hello
         Glossary matches: []
         Comment: A friendly greeting
         Context: some.id
         Plural form: one""",
        prompt);
  }

  @Test
  void testPromptTemplatingNoContextValue() {
    String promptText =
        """
             Translate the following source string from [mojito_source_locale] to [mojito_target_locale]:
             Source string: [mojito_source_string]
             {{optional: Comment: [mojito_comment_string]}}
             {{optional: Context: [mojito_context_string]}}
             {{optional: Plural form: [mojito_plural_form]}}
             """;

    PluralForm one = new PluralForm();
    one.setName("one");
    TMTextUnit tmTextUnit = new TMTextUnit();
    tmTextUnit.setId(1L);
    tmTextUnit.setContent("Hello");
    tmTextUnit.setComment("A friendly greeting");
    tmTextUnit.setName("Hello");
    tmTextUnit.setPluralForm(one);
    String prompt =
        openAILLMService.getTranslationFormattedPrompt(
            promptText, tmTextUnit, "en", "fr", Collections.emptyList());
    assertEquals(
        """
             Translate the following source string from en to fr:
             Source string: Hello
             Comment: A friendly greeting
             Plural form: one""",
        prompt);
  }

  @Test
  void testPromptTemplatingNoPluralValue() {
    String promptText =
        """
                 Translate the following source string from [mojito_source_locale] to [mojito_target_locale]:
                 Source string: [mojito_source_string]
                 {{optional: Comment: [mojito_comment_string]}}
                 {{optional: Context: [mojito_context_string]}}
                 {{optional: Plural form: [mojito_plural_form]}}
                 """;

    TMTextUnit tmTextUnit = new TMTextUnit();
    tmTextUnit.setId(1L);
    tmTextUnit.setContent("Hello");
    tmTextUnit.setComment("A friendly greeting");
    tmTextUnit.setName("Hello --- some.id");
    String prompt =
        openAILLMService.getTranslationFormattedPrompt(
            promptText, tmTextUnit, "en", "fr", Collections.emptyList());
    assertEquals(
        """
                 Translate the following source string from en to fr:
                 Source string: Hello
                 Comment: A friendly greeting
                 Context: some.id""",
        prompt);
  }

  @Test
  void testPromptTemplatingNoCommentValue() {
    String promptText =
        """
                 Translate the following source string from [mojito_source_locale] to [mojito_target_locale]:
                 Source string: [mojito_source_string]
                 {{optional: Comment: [mojito_comment_string]}}
                 {{optional: Context: [mojito_context_string]}}
                 {{optional: Plural form: [mojito_plural_form]}}
                 """;

    PluralForm one = new PluralForm();
    one.setName("one");
    TMTextUnit tmTextUnit = new TMTextUnit();
    tmTextUnit.setId(1L);
    tmTextUnit.setContent("Hello");
    tmTextUnit.setName("Hello --- some.id");
    tmTextUnit.setPluralForm(one);
    String prompt =
        openAILLMService.getTranslationFormattedPrompt(
            promptText, tmTextUnit, "en", "fr", Collections.emptyList());
    assertEquals(
        """
             Translate the following source string from en to fr:
             Source string: Hello
             Context: some.id
             Plural form: one""",
        prompt);
  }

  @Test
  void testPromptTemplatingInlineNoCommentValue() {
    String promptText =
        """
                 Translate the following source string from [mojito_source_locale] to [mojito_target_locale]:
                 Source string: [mojito_source_string]
                 {{optional: Comment: [mojito_comment_string]}} {{optional: Context: [mojito_context_string]}} {{optional: Plural form: [mojito_plural_form]}}
                 """;

    PluralForm one = new PluralForm();
    one.setName("one");
    TMTextUnit tmTextUnit = new TMTextUnit();
    tmTextUnit.setId(1L);
    tmTextUnit.setContent("Hello");
    tmTextUnit.setName("Hello --- some.id");
    tmTextUnit.setPluralForm(one);
    String prompt =
        openAILLMService.getTranslationFormattedPrompt(
            promptText, tmTextUnit, "en", "fr", Collections.emptyList());
    assertEquals(
        """
             Translate the following source string from en to fr:
             Source string: Hello
             Context: some.id Plural form: one""",
        prompt);
  }

  @Test
  void testPromptTemplatingInlineNoContextValue() {
    String promptText =
        """
                 Translate the following source string from [mojito_source_locale] to [mojito_target_locale]:
                 Source string: [mojito_source_string]
                 {{optional: Comment: [mojito_comment_string]}} {{optional: Context: [mojito_context_string]}} {{optional: Plural form: [mojito_plural_form]}}
                 """;

    PluralForm one = new PluralForm();
    one.setName("one");
    TMTextUnit tmTextUnit = new TMTextUnit();
    tmTextUnit.setId(1L);
    tmTextUnit.setContent("Hello");
    tmTextUnit.setComment("A friendly greeting");
    tmTextUnit.setPluralForm(one);
    String prompt =
        openAILLMService.getTranslationFormattedPrompt(
            promptText, tmTextUnit, "en", "fr", Collections.emptyList());
    assertEquals(
        """
             Translate the following source string from en to fr:
             Source string: Hello
             Comment: A friendly greeting Plural form: one""",
        prompt);
  }

  @Test
  void testPromptTemplatingJsonInPrompt() {
    String promptText =
        """
                 Translate the following source string from [mojito_source_locale] to [mojito_target_locale]:
                 Source string: [mojito_source_string]
                 { {{optional: "comment": "[mojito_comment_string]",}} {{optional: "context": "[mojito_context_string]",}} {{optional: "plural_form": "[mojito_plural_form]"}} }
                 """;

    PluralForm one = new PluralForm();
    one.setName("one");
    TMTextUnit tmTextUnit = new TMTextUnit();
    tmTextUnit.setId(1L);
    tmTextUnit.setContent("Hello");
    tmTextUnit.setComment("A friendly greeting");
    tmTextUnit.setName("Hello --- some.id");
    tmTextUnit.setPluralForm(one);
    String prompt =
        openAILLMService.getTranslationFormattedPrompt(
            promptText, tmTextUnit, "en", "fr", Collections.emptyList());
    assertEquals(
        """
             Translate the following source string from en to fr:
             Source string: Hello
             { "comment": "A friendly greeting", "context": "some.id", "plural_form": "one" }""",
        prompt);
  }

  @Test
  void testPromptTemplatingJsonInPromptContextMissing() {
    String promptText =
        """
                     Translate the following source string from [mojito_source_locale] to [mojito_target_locale]:
                     Source string: [mojito_source_string]
                     {{{optional: "comment": "[mojito_comment_string]",}} {{optional: "context": "[mojito_context_string]",}} {{optional: "plural_form": "[mojito_plural_form]"}}}
                     """;

    PluralForm one = new PluralForm();
    one.setName("one");
    TMTextUnit tmTextUnit = new TMTextUnit();
    tmTextUnit.setId(1L);
    tmTextUnit.setContent("Hello");
    tmTextUnit.setComment("A friendly greeting");
    tmTextUnit.setPluralForm(one);
    String prompt =
        openAILLMService.getTranslationFormattedPrompt(
            promptText, tmTextUnit, "en", "fr", Collections.emptyList());
    assertEquals(
        """
                 Translate the following source string from en to fr:
                 Source string: Hello
                 {"comment": "A friendly greeting", "plural_form": "one"}""",
        prompt);
  }

  @Test
  void testPromptTemplatingJsonInPromptGlossaryMatches() {
    String promptText =
        """
       Translate the following source string from [mojito_source_locale] to [mojito_target_locale]:
       Source string: [mojito_source_string]
       {{{optional: "comment": "[mojito_comment_string]",}} {{optional: "context": "[mojito_context_string]",}} {{optional: "plural_form": "[mojito_plural_form]"}}}
       {{optional: The glossary matches are: [mojito_glossary_term_matches]. }}
       """;

    PluralForm one = new PluralForm();
    one.setName("one");
    TMTextUnit tmTextUnit = new TMTextUnit();
    tmTextUnit.setId(1L);
    tmTextUnit.setContent("Hello");
    tmTextUnit.setComment("A friendly greeting");
    tmTextUnit.setPluralForm(one);
    GlossaryTerm glossaryTerm = new GlossaryTerm();
    glossaryTerm.setText("Hello");
    glossaryTerm.setTranslations(Collections.singletonMap("fr", "Bonjour"));
    String prompt =
        openAILLMService.getTranslationFormattedPrompt(
            promptText, tmTextUnit, "en", "fr", Collections.singletonList(glossaryTerm));
    assertEquals(
        """
                     Translate the following source string from en to fr:
                     Source string: Hello
                     {"comment": "A friendly greeting", "plural_form": "one"}
                     The glossary matches are: [{"text":"Hello","isExactMatch":false,"isCaseSensitive":false,"isDoNotTranslate":false,"translation":"Bonjour"}].""",
        prompt);
  }

  @Test
  void testPromptTemplatingJsonInPromptGlossaryMatchesDNT() {
    String promptText =
        """
           Translate the following source string from [mojito_source_locale] to [mojito_target_locale]:
           Source string: [mojito_source_string]
           {{{optional: "comment": "[mojito_comment_string]",}} {{optional: "context": "[mojito_context_string]",}} {{optional: "plural_form": "[mojito_plural_form]"}}}
           {{optional: The glossary matches are: [mojito_glossary_term_matches]. }}
           """;

    PluralForm one = new PluralForm();
    one.setName("one");
    TMTextUnit tmTextUnit = new TMTextUnit();
    tmTextUnit.setId(1L);
    tmTextUnit.setContent("Hello");
    tmTextUnit.setComment("A friendly greeting");
    tmTextUnit.setPluralForm(one);
    GlossaryTerm glossaryTerm = new GlossaryTerm();
    glossaryTerm.setText("Hello");
    glossaryTerm.setDoNotTranslate(true);
    glossaryTerm.setTranslations(Collections.singletonMap("fr", null));
    String prompt =
        openAILLMService.getTranslationFormattedPrompt(
            promptText, tmTextUnit, "en", "fr", Collections.singletonList(glossaryTerm));
    assertEquals(
        """
                         Translate the following source string from en to fr:
                         Source string: Hello
                         {"comment": "A friendly greeting", "plural_form": "one"}
                         The glossary matches are: [{"text":"Hello","isExactMatch":false,"isCaseSensitive":false,"isDoNotTranslate":true,"translation":"Hello"}].""",
        prompt);
  }

  @Test
  void testPromptTemplatingOptionalBlockNoGlossaryMatches() {
    String promptText =
        """
               Translate the following source string from [mojito_source_locale] to [mojito_target_locale]:
               Source string: [mojito_source_string]
               {{optional: The glossary matches are: [mojito_glossary_term_matches]. }}
               {{{optional: "comment": "[mojito_comment_string]",}} {{optional: "context": "[mojito_context_string]",}} {{optional: "plural_form": "[mojito_plural_form]"}}}
               """;

    PluralForm one = new PluralForm();
    one.setName("one");
    TMTextUnit tmTextUnit = new TMTextUnit();
    tmTextUnit.setId(1L);
    tmTextUnit.setContent("Hello");
    tmTextUnit.setComment("A friendly greeting");
    tmTextUnit.setPluralForm(one);

    String prompt =
        openAILLMService.getTranslationFormattedPrompt(
            promptText, tmTextUnit, "en", "fr", Collections.emptyList());
    assertEquals(
        """
                             Translate the following source string from en to fr:
                             Source string: Hello
                             {"comment": "A friendly greeting", "plural_form": "one"}""",
        prompt);
  }

  @Test
  void testPromptTemplatingStaticBlockNoGlossaryMatches() {
    String promptText =
        """
                   Translate the following source string from [mojito_source_locale] to [mojito_target_locale]:
                   Source string: [mojito_source_string]
                   The glossary matches are: [mojito_glossary_term_matches].
                   {{{optional: "comment": "[mojito_comment_string]",}} {{optional: "context": "[mojito_context_string]",}} {{optional: "plural_form": "[mojito_plural_form]"}}}
                   """;

    PluralForm one = new PluralForm();
    one.setName("one");
    TMTextUnit tmTextUnit = new TMTextUnit();
    tmTextUnit.setId(1L);
    tmTextUnit.setContent("Hello");
    tmTextUnit.setComment("A friendly greeting");
    tmTextUnit.setPluralForm(one);

    String prompt =
        openAILLMService.getTranslationFormattedPrompt(
            promptText, tmTextUnit, "en", "fr", Collections.emptyList());
    assertEquals(
        """
                                 Translate the following source string from en to fr:
                                 Source string: Hello
                                 The glossary matches are: [].
                                 {"comment": "A friendly greeting", "plural_form": "one"}""",
        prompt);
  }

  @Test
  void testPromptTemplatingInlineSentence() {
    String promptText =
        """
                 Translate the following source string from [mojito_source_locale] to [mojito_target_locale]:
                 Source string: [mojito_source_string]
                 {{optional: The comment is: [mojito_comment_string]. }}{{optional: The context is: [mojito_context_string]. }}{{optional: The plural form is: [mojito_plural_form]. }}
                 """;

    PluralForm one = new PluralForm();
    one.setName("one");
    TMTextUnit tmTextUnit = new TMTextUnit();
    tmTextUnit.setId(1L);
    tmTextUnit.setContent("Hello");
    tmTextUnit.setComment("A friendly greeting");
    tmTextUnit.setName("Hello");
    tmTextUnit.setPluralForm(one);
    String prompt =
        openAILLMService.getTranslationFormattedPrompt(
            promptText, tmTextUnit, "en", "fr", Collections.emptyList());
    assertEquals(
        """
                 Translate the following source string from en to fr:
                 Source string: Hello
                 The comment is: A friendly greeting. The plural form is: one.""",
        prompt);
  }

  @Test
  void testPromptTemplatingGlossaryMatchesInlineSentence() {
    String promptText =
        """
                     Translate the following source string from [mojito_source_locale] to [mojito_target_locale]:
                     Source string: [mojito_source_string]
                     {{optional: The comment is: [mojito_comment_string]. }}{{optional: The context is: [mojito_context_string]. }}{{optional: The plural form is: [mojito_plural_form]. }}
                     {{optional: The glossary matches are: [mojito_glossary_term_matches]. }}
                     """;

    PluralForm one = new PluralForm();
    one.setName("one");
    TMTextUnit tmTextUnit = new TMTextUnit();
    tmTextUnit.setId(1L);
    tmTextUnit.setContent("Hello");
    tmTextUnit.setComment(null);
    tmTextUnit.setName("Hello");
    tmTextUnit.setPluralForm(null);
    GlossaryTerm glossaryTerm = new GlossaryTerm();
    glossaryTerm.setText("Hello");
    glossaryTerm.setTranslations(Collections.singletonMap("fr", "Bonjour"));
    String prompt =
        openAILLMService.getTranslationFormattedPrompt(
            promptText, tmTextUnit, "en", "fr", Collections.singletonList(glossaryTerm));
    assertEquals(
        """
                Translate the following source string from en to fr:
                Source string: Hello
                The glossary matches are: [{"text":"Hello","isExactMatch":false,"isCaseSensitive":false,"isDoNotTranslate":false,"translation":"Bonjour"}].""",
        prompt);
  }

  @Test
  void testPromptTemplatingOptionalGlossaryStaticOthers() {
    String promptText =
        """
                         Translate the following source string from [mojito_source_locale] to [mojito_target_locale]:
                         Source string: [mojito_source_string]
                         The comment is: [mojito_comment_string]. The context is: [mojito_context_string]
                         {{optional: The glossary matches are: [mojito_glossary_term_matches]. }}
                         """;

    PluralForm one = new PluralForm();
    one.setName("one");
    TMTextUnit tmTextUnit = new TMTextUnit();
    tmTextUnit.setId(1L);
    tmTextUnit.setContent("Hello");
    tmTextUnit.setComment("A comment");
    tmTextUnit.setName("Hello --- some.id");
    tmTextUnit.setPluralForm(null);
    GlossaryTerm glossaryTerm = new GlossaryTerm();
    glossaryTerm.setText("Hello");
    glossaryTerm.setTranslations(Collections.singletonMap("fr", "Bonjour"));
    String prompt =
        openAILLMService.getTranslationFormattedPrompt(
            promptText, tmTextUnit, "en", "fr", Collections.singletonList(glossaryTerm));
    assertEquals(
        """
                    Translate the following source string from en to fr:
                    Source string: Hello
                    The comment is: A comment. The context is: some.id
                    The glossary matches are: [{"text":"Hello","isExactMatch":false,"isCaseSensitive":false,"isDoNotTranslate":false,"translation":"Bonjour"}].""",
        prompt);
  }

  @Test
  void testPromptTemplatingMultipleGlossaryMatches() {
    String promptText =
        """
                         Translate the following source string from [mojito_source_locale] to [mojito_target_locale]:
                         Source string: [mojito_source_string]
                         {{optional: The comment is: [mojito_comment_string]. }}{{optional: The context is: [mojito_context_string]. }}{{optional: The plural form is: [mojito_plural_form]. }}
                         {{optional: The glossary matches are: [mojito_glossary_term_matches]. }}
                         """;

    PluralForm one = new PluralForm();
    one.setName("one");
    TMTextUnit tmTextUnit = new TMTextUnit();
    tmTextUnit.setId(1L);
    tmTextUnit.setContent("Hello");
    tmTextUnit.setComment(null);
    tmTextUnit.setName("Hello");
    tmTextUnit.setPluralForm(null);
    GlossaryTerm glossaryTerm = new GlossaryTerm();
    glossaryTerm.setText("Hello");
    glossaryTerm.setTranslations(Collections.singletonMap("fr", "Bonjour"));
    GlossaryTerm glossaryTerm2 = new GlossaryTerm();
    glossaryTerm2.setText("Another term");
    glossaryTerm2.setCaseSensitive(true);
    glossaryTerm2.setTranslations(Collections.singletonMap("fr", "Another term translation"));
    String prompt =
        openAILLMService.getTranslationFormattedPrompt(
            promptText, tmTextUnit, "en", "fr", List.of(glossaryTerm, glossaryTerm2));
    assertEquals(
        """
                    Translate the following source string from en to fr:
                    Source string: Hello
                    The glossary matches are: [{"text":"Hello","isExactMatch":false,"isCaseSensitive":false,"isDoNotTranslate":false,"translation":"Bonjour"},{"text":"Another term","isExactMatch":false,"isCaseSensitive":true,"isDoNotTranslate":false,"translation":"Another term translation"}].""",
        prompt);
  }

  @Test
  void testExecuteAIChecksWithSleepTime() {
    AIPrompt prompt = new AIPrompt();
    prompt.setId(1L);
    prompt.setUserPrompt("Check strings for spelling");
    prompt.setModelName("gtp-3.5-turbo");
    prompt.setPromptTemperature(0.0F);
    prompt.setContextMessages(new ArrayList<>());
    AssetExtractorTextUnit assetExtractorTextUnit = new AssetExtractorTextUnit();
    assetExtractorTextUnit.setSource("A test string");
    assetExtractorTextUnit.setName("A test string --- A test context");
    assetExtractorTextUnit.setComments("A test comment");
    AssetExtractorTextUnit assetExtractorTextUnit2 = new AssetExtractorTextUnit();
    assetExtractorTextUnit2.setSource("A test string 2");
    assetExtractorTextUnit2.setName("A test string --- A test context 2");
    assetExtractorTextUnit2.setComments("A test comment 2");
    List<AssetExtractorTextUnit> textUnits =
        List.of(assetExtractorTextUnit, assetExtractorTextUnit2);
    AICheckRequest aiCheckRequest = new AICheckRequest();
    aiCheckRequest.setRepositoryName("testRepo");
    aiCheckRequest.setTextUnits(textUnits);
    List<OpenAIClient.ChatCompletionsResponse.Choice> choices =
        List.of(
            new OpenAIClient.ChatCompletionsResponse.Choice(
                0,
                new OpenAIClient.ChatCompletionsResponse.Choice.Message(
                    "test", "{\"success\": true, \"suggestedFix\": \"\"}"),
                null));
    Repository repository = new Repository();
    repository.setName("testRepo");
    repository.setId(1L);
    OpenAIClient.ChatCompletionsResponse chatCompletionsResponse =
        new OpenAIClient.ChatCompletionsResponse(null, null, null, null, choices, null, null);
    CompletableFuture<OpenAIClient.ChatCompletionsResponse> futureResponse =
        CompletableFuture.completedFuture(chatCompletionsResponse);
    List<AIPrompt> prompts = List.of(prompt);

    when(repositoryRepository.findByName("testRepo")).thenReturn(repository);
    when(promptService.getPromptsByRepositoryAndPromptType(
            repository, PromptType.SOURCE_STRING_CHECKER))
        .thenReturn(prompts);
    doAnswer(
            invocation -> {
              Thread.sleep(100);
              return futureResponse;
            })
        .when(openAIClient)
        .getChatCompletions(any(OpenAIClient.ChatCompletionsRequest.class));

    AICheckResponse response = this.openAILLMService.executeAIChecks(aiCheckRequest);

    assertNotNull(response);
    assertEquals(2, response.getResults().size());
    assertTrue(response.getResults().containsKey("A test string"));
    assertTrue(response.getResults().get("A test string").getFirst().isSuccess());

    assertTrue(response.getResults().containsKey("A test string 2"));
    assertTrue(response.getResults().get("A test string 2").getFirst().isSuccess());

    verify(aiStringCheckRepository, times(2)).save(any());
    verify(meterRegistry, times(2))
        .counter("OpenAILLMService.checks.result", "success", "true", "repository", "testRepo");
  }

  @Test
  void testTranslateResultMetric404Error() {
    TMTextUnit tmTextUnit = new TMTextUnit();
    tmTextUnit.setId(1L);
    tmTextUnit.setContent("Hello");
    tmTextUnit.setComment("Greeting");

    AIPrompt prompt = new AIPrompt();
    prompt.setId(1L);
    prompt.setSystemPrompt("Translate the following text:");
    prompt.setUserPrompt("Translate this text to French:");
    prompt.setModelName("gtp-3.5-turbo");
    prompt.setPromptTemperature(0.0F);
    prompt.setContextMessages(new ArrayList<>());

    HttpResponse<String> mockHttpResponse = mock(HttpResponse.class);
    when(mockHttpResponse.statusCode()).thenReturn(404);
    when(mockHttpResponse.body()).thenReturn("Model not found");

    when(openAIClient.getChatCompletions(any(OpenAIClient.ChatCompletionsRequest.class)))
        .thenThrow(openAIClient.new OpenAIClientResponseException("Not Found", mockHttpResponse));

    assertThrows(
        AIException.class, () -> openAILLMService.translate(tmTextUnit, "en", "fr", prompt));
    verify(meterRegistry, times(2))
        .counter(
            eq("OpenAILLMService.translate.result"),
            eq(Tags.of("success", "false", "retryable", "true", "statusCode", "404")));
  }

  @Test
  void testTranslateResultMetricNullError() {
    TMTextUnit tmTextUnit = new TMTextUnit();
    tmTextUnit.setId(1L);
    tmTextUnit.setContent("Hello");
    tmTextUnit.setComment("Greeting");

    AIPrompt prompt = new AIPrompt();
    prompt.setId(1L);
    prompt.setSystemPrompt("Translate the following text:");
    prompt.setUserPrompt("Translate this text to French:");
    prompt.setModelName("gtp-3.5-turbo");
    prompt.setPromptTemperature(0.0F);
    prompt.setContextMessages(new ArrayList<>());

    when(openAIClient.getChatCompletions(any(OpenAIClient.ChatCompletionsRequest.class)))
        .thenThrow(new RuntimeException("OpenAI service error"));

    assertThrows(Exception.class, () -> openAILLMService.translate(tmTextUnit, "en", "fr", prompt));
    verify(meterRegistry, times(2))
        .counter(
            eq("OpenAILLMService.translate.result"),
            eq(Tags.of("success", "false", "retryable", "true", "statusCode", "null")));
  }
}

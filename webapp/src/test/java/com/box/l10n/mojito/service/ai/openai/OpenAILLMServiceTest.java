package com.box.l10n.mojito.service.ai.openai;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.box.l10n.mojito.entity.AIPrompt;
import com.box.l10n.mojito.entity.AIPromptContextMessage;
import com.box.l10n.mojito.entity.PromptType;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.json.ObjectMapper;
import com.box.l10n.mojito.okapi.extractor.AssetExtractorTextUnit;
import com.box.l10n.mojito.openai.OpenAIClient;
import com.box.l10n.mojito.rest.ai.AICheckRequest;
import com.box.l10n.mojito.rest.ai.AICheckResponse;
import com.box.l10n.mojito.service.ai.AIStringCheckRepository;
import com.box.l10n.mojito.service.repository.RepositoryRepository;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.ArrayList;
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
    when(meterRegistry.counter(anyString(), any(String[].class)))
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
}

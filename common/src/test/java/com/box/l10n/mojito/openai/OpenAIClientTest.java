package com.box.l10n.mojito.openai;

import static com.box.l10n.mojito.openai.OpenAIClient.ChatCompletionsRequest.Models.GPT_3_5_TURBO;
import static com.box.l10n.mojito.openai.OpenAIClient.ChatCompletionsRequest.SystemMessage.systemMessageBuilder;
import static com.box.l10n.mojito.openai.OpenAIClient.ChatCompletionsRequest.UserMessage.userMessageBuilder;
import static com.box.l10n.mojito.openai.OpenAIClient.ChatCompletionsRequest.chatCompletionsRequest;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.box.l10n.mojito.openai.OpenAIClient.ChatCompletionsResponse;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import org.junit.jupiter.api.Test;

class OpenAIClientTest {

  static final String API_KEY;

  static {
    try {
      //      API_KEY =
      //
      // Files.readString(Paths.get(System.getProperty("user.home")).resolve(".keys/openai"))
      //              .trim();
      API_KEY = "test-api-key";
    } catch (Throwable e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  void testOpenAIClientBuilderApiKeyMustBeProvided() {
    IllegalStateException illegalStateException =
        assertThrowsExactly(IllegalStateException.class, () -> OpenAIClient.builder().build());
    assertEquals("API key must be provided", illegalStateException.getMessage());
  }

  @Test
  public void testGetChatCompletionsSuccess() throws Exception {
    OpenAIClient.ChatCompletionsRequest chatCompletionsRequest =
        chatCompletionsRequest()
            .model(GPT_3_5_TURBO)
            .messages(
                List.of(
                    systemMessageBuilder()
                        .content("Translate the following sentence from English to French")
                        .build(),
                    userMessageBuilder().content("This is a unit test").build()))
            .build();

    String jsonResponse =
        """
                {
                  "id": "chatcmpl-9DNYjOkXJxILUK3NXFv9MCZV0P8jZ",
                  "object": "chat.completion",
                  "created": 1712975853,
                  "model": "gpt-3.5-turbo-0125",
                  "choices": [
                    {
                      "index": 0,
                      "message": {
                        "role": "assistant",
                        "content": "Il s'agit d'un test unitaire"
                      },
                      "logprobs": null,
                      "finish_reason": "stop"
                    }
                  ],
                  "usage": {
                    "prompt_tokens": 24,
                    "completion_tokens": 9,
                    "total_tokens": 33
                  },
                  "system_fingerprint": "fp_c2295e73ad"
                }""";

    HttpResponse<String> mockResponse = mock(HttpResponse.class);
    when(mockResponse.statusCode()).thenReturn(200);
    when(mockResponse.body()).thenReturn(jsonResponse);

    HttpClient mockHttpClient = mock(HttpClient.class);
    when(mockHttpClient.sendAsync(
            any(HttpRequest.class), any(HttpResponse.BodyHandlers.ofString().getClass())))
        .thenReturn(CompletableFuture.completedFuture(mockResponse));

    OpenAIClient openAIClient =
        OpenAIClient.builder().apiKey(API_KEY).httpClient(mockHttpClient).build();

    ChatCompletionsResponse chatCompletionsResponse =
        openAIClient.getChatCompletions(chatCompletionsRequest).join();
    assertNotNull(chatCompletionsResponse);
    assertEquals("chatcmpl-9DNYjOkXJxILUK3NXFv9MCZV0P8jZ", chatCompletionsResponse.id());
    assertEquals(
        "Il s'agit d'un test unitaire",
        chatCompletionsResponse.choices().get(0).message().content());
  }

  /**
   * Test error that will be shown if the response can't be parse by the bean provided by the
   * library. Ideally, it should not happen, but in case it does the message must be clear.
   */
  @Test
  public void testGetChatCompletionsRequestError() throws Exception {
    OpenAIClient.ChatCompletionsRequest chatCompletionsRequest =
        chatCompletionsRequest()
            .model("invalid-model")
            .messages(
                List.of(
                    systemMessageBuilder()
                        .content("Translate the following sentence from English to French")
                        .build(),
                    userMessageBuilder().content("This is a unit test").build()))
            .build();

    HttpResponse<String> mockResponse = mock(HttpResponse.class);
    when(mockResponse.statusCode()).thenReturn(400);
    String errorMsg =
        """
                {
                    "error": {
                        "message": "The model `invalid-model` does not exist or you do not have access to it.",
                        "type": "invalid_request_error",
                        "param": null,
                        "code": "model_not_found"
                    }
                }""";
    when(mockResponse.body()).thenReturn(errorMsg);

    HttpClient mockHttpClient = mock(HttpClient.class);
    when(mockHttpClient.sendAsync(
            any(HttpRequest.class), any(HttpResponse.BodyHandlers.ofString().getClass())))
        .thenReturn(CompletableFuture.completedFuture(mockResponse));

    OpenAIClient openAIClient =
        OpenAIClient.builder().apiKey(API_KEY).httpClient(mockHttpClient).build();
    CompletionException completionException =
        assertThrows(
            CompletionException.class,
            () -> openAIClient.getChatCompletions(chatCompletionsRequest).join());
    assertEquals("ChatCompletion failed", completionException.getCause().getMessage());
    assertTrue(
        completionException
            .getMessage()
            .contains(
                """
                "error": {
                        "message": "The model `invalid-model` does not exist or you do not have access to it.",
                        "type": "invalid_request_error",
                        "param": null,
                        "code": "model_not_found"
                    }"""));
  }

  @Test
  public void testGetChatCompletionsDeserializationError() throws Exception {
    OpenAIClient.ChatCompletionsRequest chatCompletionsRequest =
        chatCompletionsRequest()
            .model(GPT_3_5_TURBO)
            .messages(
                List.of(
                    systemMessageBuilder()
                        .content("Translate the following sentence from English to French")
                        .build(),
                    userMessageBuilder().content("This is a unit test").build()))
            .build();
    OpenAIClient.builder()
        .apiKey(API_KEY)
        .build()
        .getChatCompletions(chatCompletionsRequest().build());

    String jsonResponse =
        """
                {
                  "id": "chatcmpl-9DNYjOkXJxILUK3NXFv9MCZV0P8jZ",
                  "object": "chat.completion",
                  "created": "invalid date to break deserialization",
                  "model": "gpt-3.5-turbo-0125",
                  "choices": [
                    {
                      "index": 0,
                      "message": {
                        "role": "assistant",
                        "content": "Il s'agit d'un test unitaire"
                      },
                      "logprobs": null,
                      "finish_reason": "stop"
                    }
                  ],
                  "usage": {
                    "prompt_tokens": 24,
                    "completion_tokens": 9,
                    "total_tokens": 33
                  },
                  "system_fingerprint": "fp_c2295e73ad"
                }""";

    HttpResponse<String> mockResponse = mock(HttpResponse.class);
    when(mockResponse.statusCode()).thenReturn(200);
    when(mockResponse.body()).thenReturn(jsonResponse);

    HttpClient mockHttpClient = mock(HttpClient.class);
    when(mockHttpClient.sendAsync(
            any(HttpRequest.class), any(HttpResponse.BodyHandlers.ofString().getClass())))
        .thenReturn(CompletableFuture.completedFuture(mockResponse));

    OpenAIClient openAIClient =
        OpenAIClient.builder().apiKey(API_KEY).httpClient(mockHttpClient).build();
    CompletionException completionException =
        assertThrows(
            CompletionException.class,
            () -> openAIClient.getChatCompletions(chatCompletionsRequest).join());
    assertEquals(
        "Can't deserialize ChatCompletionsResponse", completionException.getCause().getMessage());
  }
}

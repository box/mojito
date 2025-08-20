package com.box.l10n.mojito.openai;

import static com.box.l10n.mojito.openai.OpenAIClient.ChatCompletionsRequest.Models.GPT_3_5_TURBO;
import static com.box.l10n.mojito.openai.OpenAIClient.ChatCompletionsRequest.SystemMessage.systemMessageBuilder;
import static com.box.l10n.mojito.openai.OpenAIClient.ChatCompletionsRequest.UserMessage.userMessageBuilder;
import static com.box.l10n.mojito.openai.OpenAIClient.ChatCompletionsRequest.chatCompletionsRequest;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.box.l10n.mojito.openai.OpenAIClient.ChatCompletionsResponse;
import com.box.l10n.mojito.openai.OpenAIClient.OpenAIClientResponseException;
import com.box.l10n.mojito.openai.OpenAIClient.UploadFileRequest;
import com.box.l10n.mojito.openai.OpenAIClient.UploadFileRequest.BinaryContent;
import com.box.l10n.mojito.openai.OpenAIClient.UploadFileResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Flow;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

public class OpenAIClientTest {

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
  public void testOpenAIClientBuilderApiKeyMustBeProvided() {
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
        openAIClient
            .getChatCompletions(chatCompletionsRequest, Duration.of(5, ChronoUnit.SECONDS))
            .join();
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
            () ->
                openAIClient
                    .getChatCompletions(chatCompletionsRequest, Duration.of(5, ChronoUnit.SECONDS))
                    .join());
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
  public void testGetResponsesSuccessWithTextOnly() throws Exception {
    OpenAIClient.ResponsesRequest responsesRequest =
        OpenAIClient.ResponsesRequest.builder()
            .model("gpt-4o-mini")
            .addUserText("Translate 'Save' to French. Output only the translation.")
            .build();

    String jsonResponse =
        """
        {
          "id": "resp_abc123",
          "object": "response",
          "created_at": 1755702430,
          "status": "completed",
          "model": "gpt-4o-mini",
          "output": [
            {
              "id": "msg_abc123",
              "type": "message",
              "status": "completed",
              "content": [
                { "type": "output_text", "text": "Enregistrer" }
              ],
              "role": "assistant"
            }
          ],
          "output_text": ["Enregistrer"]
        }
        """;

    HttpResponse<String> mockResponse = mock(HttpResponse.class);
    when(mockResponse.statusCode()).thenReturn(200);
    when(mockResponse.body()).thenReturn(jsonResponse);

    HttpClient mockHttpClient = mock(HttpClient.class);
    when(mockHttpClient.sendAsync(
            any(HttpRequest.class), any(HttpResponse.BodyHandlers.ofString().getClass())))
        .thenReturn(CompletableFuture.completedFuture(mockResponse));

    OpenAIClient openAIClient =
        OpenAIClient.builder().apiKey(API_KEY).httpClient(mockHttpClient).build();

    OpenAIClient.ResponsesResponse responsesResponse =
        openAIClient.getResponses(responsesRequest, Duration.ofSeconds(5)).join();

    assertNotNull(responsesResponse);
    assertEquals("resp_abc123", responsesResponse.id());
    assertEquals("gpt-4o-mini", responsesResponse.model());
    assertEquals("completed", responsesResponse.status());
    assertEquals(1, responsesResponse.output().size());
    assertEquals("Enregistrer", responsesResponse.outputText());
  }

  @Test
  public void testGetResponsesRequestError() throws Exception {
    OpenAIClient.ResponsesRequest responsesRequest =
        OpenAIClient.ResponsesRequest.builder()
            .model("gpt-4o-mini")
            .addUserText("Translate 'Save' to French. Output only the translation.")
            .build();

    String errorMsg =
        """
        {
          "error": {
            "message": "Invalid model",
            "type": "invalid_request_error",
            "param": "model",
            "code": "model_not_found"
          }
        }
        """;

    HttpResponse<String> mockResponse = mock(HttpResponse.class);
    when(mockResponse.statusCode()).thenReturn(400);
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
            () -> openAIClient.getResponses(responsesRequest, Duration.ofSeconds(5)).join());
    assertEquals("Responses API failed", completionException.getCause().getMessage());
    assertTrue(completionException.getMessage().contains("Invalid model"));
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
        .getChatCompletions(chatCompletionsRequest().build(), Duration.of(5, ChronoUnit.SECONDS));

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
            () ->
                openAIClient
                    .getChatCompletions(chatCompletionsRequest, Duration.of(5, ChronoUnit.SECONDS))
                    .join());
    assertEquals(
        "Can't deserialize ChatCompletionsResponse", completionException.getCause().getMessage());
  }

  @Test
  public void testUploadFileSuccess() throws Exception {

    HttpClient mockHttpClient = mock(HttpClient.class);
    HttpResponse<String> mockResponse = mock(HttpResponse.class);

    when(mockResponse.statusCode()).thenReturn(200);
    when(mockResponse.body())
        .thenReturn(
            """
          {
            "id": "file-123",
            "filename": "example.jsonl",
            "status": "uploaded",
            "created_at": 1690000000
          }""");
    when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(mockResponse);

    OpenAIClient openAIClient =
        OpenAIClient.builder().apiKey(API_KEY).httpClient(mockHttpClient).build();

    UploadFileRequest fileUploadRequest = UploadFileRequest.forBatch("example.jsonl", "{}\n{}\n");

    UploadFileResponse uploadFileResponse = openAIClient.uploadFile(fileUploadRequest);

    assertNotNull(uploadFileResponse);
    assertEquals("file-123", uploadFileResponse.id());
    assertEquals("example.jsonl", uploadFileResponse.filename());
    assertEquals("uploaded", uploadFileResponse.status());
    assertEquals(1690000000, uploadFileResponse.createdAt());
  }

  @Test
  public void testUploadFileError() throws Exception {

    HttpClient mockHttpClient = mock(HttpClient.class);
    HttpResponse<String> mockResponse = mock(HttpResponse.class);

    when(mockResponse.statusCode()).thenReturn(400);
    String errorMessage =
        """
        {
          "error": {
            "message": "Invalid file format for Batch API. Must be .jsonl",
            "type": "invalid_request_error",
            "param": null,
            "code": null
          }
        }
        """;
    when(mockResponse.body()).thenReturn(errorMessage);
    when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(mockResponse);

    OpenAIClient openAIClient =
        OpenAIClient.builder().apiKey(API_KEY).httpClient(mockHttpClient).build();

    UploadFileRequest fileUploadRequest =
        UploadFileRequest.forBatch(
            "example.jsonl",
            """
          {
            "a" : "b"
          }
          """);

    OpenAIClientResponseException openAIClientResponseException =
        assertThrows(
            OpenAIClientResponseException.class, () -> openAIClient.uploadFile(fileUploadRequest));
    assertEquals(openAIClientResponseException.httpResponse.statusCode(), 400);
    assertEquals(openAIClientResponseException.httpResponse.body(), errorMessage);
  }

  @Test
  public void testFileUploadRequestMultiPartBody() {
    UploadFileRequest uploadFileRequest = UploadFileRequest.forBatch("test.jsonl", "{}\n{}");
    BodyPublisher bodyPublisher = uploadFileRequest.getMultipartBodyPublisher("test-boundary");
    String actual = bodyPublisherToString(bodyPublisher);
    assertEquals(
        """
            --test-boundary\r
            Content-Disposition: form-data; name="purpose"\r
            \r
            batch\r
            --test-boundary\r
            Content-Disposition: form-data; name="file"; filename="test.jsonl"\r
            Content-Type: application/json\r
            \r
            {}
            {}\r
            --test-boundary--\r
            """,
        actual);
  }

  @Test
  public void testFileUploadRequestMultiPartBodyForImage() {
    byte[] fakePng = new byte[] {(byte) 0x89, 'P', 'N', 'G'}; // minimal header

    UploadFileRequest req =
        new UploadFileRequest("vision", "test.png", new BinaryContent(fakePng, "image/png"));

    byte[] bodyBytes = bodyPublisherToBytes(req.getMultipartBodyPublisher("img-boundary"));
    String bodyAsText = new String(bodyBytes, StandardCharsets.UTF_8);

    assertTrue(bodyAsText.contains("Content-Disposition: form-data; name=\"purpose\""));
    assertTrue(bodyAsText.contains("vision"));
    assertTrue(bodyAsText.contains("filename=\"test.png\""));
    assertTrue(bodyAsText.contains("Content-Type: image/png"));

    String marker = "Content-Type: image/png\r\n\r\n";
    int start = bodyAsText.indexOf(marker) + marker.length();
    int end = bodyAsText.indexOf("\r\n--img-boundary--", start);

    byte[] payload = Arrays.copyOfRange(bodyBytes, start, end);

    assertArrayEquals(fakePng, payload);
  }

  @Test
  public void testUploadVisionAndResponsesWithImageFileId() throws Exception {
    HttpResponse<String> mockUploadResponse = mock(HttpResponse.class);
    when(mockUploadResponse.statusCode()).thenReturn(200);
    when(mockUploadResponse.body())
        .thenReturn(
            """
            {
              "id": "file_vision_123",
              "filename": "test.png",
              "status": "uploaded",
              "created_at": 1710000000
            }
            """);

    HttpResponse<String> mockResponsesResponse = mock(HttpResponse.class);
    when(mockResponsesResponse.statusCode()).thenReturn(200);
    when(mockResponsesResponse.body())
        .thenReturn(
            """
            {
              "id": "resp_abc",
              "object": "response",
              "created_at": 1755702430,
              "status": "completed",
              "model": "gpt-4.1-mini",
              "output": [
                {
                  "id": "msg_abc",
                  "type": "message",
                  "status": "completed",
                  "content": [
                    { "type": "output_text", "text": "Enregistrer" }
                  ],
                  "role": "assistant"
                }
              ],
              "output_text": ["Enregistrer"]
            }
            """);

    HttpClient mockHttpClient = mock(HttpClient.class);

    when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(mockUploadResponse);
    when(mockHttpClient.sendAsync(
            any(HttpRequest.class), any(HttpResponse.BodyHandlers.ofString().getClass())))
        .thenReturn(CompletableFuture.completedFuture(mockResponsesResponse));

    OpenAIClient client = OpenAIClient.builder().apiKey(API_KEY).httpClient(mockHttpClient).build();

    OpenAIClient.UploadFileResponse uploaded =
        client.uploadFile(
            new OpenAIClient.UploadFileRequest(
                "vision",
                "test.png",
                new OpenAIClient.UploadFileRequest.BinaryContent(
                    new byte[] {(byte) 0x89, 'P', 'N', 'G'}, "image/png")));

    assertEquals("file_vision_123", uploaded.id());

    OpenAIClient.ResponsesRequest rr =
        OpenAIClient.ResponsesRequest.builder()
            .model("gpt-4.1-mini")
            .addUserText("what's in this image?")
            .addUserImageFileId(uploaded.id())
            .build();

    OpenAIClient.ResponsesResponse responses =
        client.getResponses(rr, Duration.ofSeconds(10)).join();

    assertNotNull(responses);
    assertEquals("completed", responses.status());
    assertEquals(1, responses.output().size());
    assertEquals("Enregistrer", responses.output().getFirst().content().getFirst().text());
    assertEquals("Enregistrer", responses.outputText());
  }

  @Test
  public void testResponsesBuilderAccumulatesMultipleMessages() throws Exception {
    HttpClient mockHttpClient = mock(HttpClient.class);
    HttpResponse<String> mockResponses = mock(HttpResponse.class);
    when(mockResponses.statusCode()).thenReturn(200);
    when(mockResponses.body())
        .thenReturn(
            """
            {"id":"resp_ok","object":"response","created_at":1755702430,
             "status":"completed","model":"gpt-4o-mini","output":[],"output_text":[]}
            """);

    final CompletableFuture<HttpResponse<String>> cf =
        CompletableFuture.completedFuture(mockResponses);
    ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
    when(mockHttpClient.sendAsync(requestCaptor.capture(), any(HttpResponse.BodyHandler.class)))
        .thenReturn(cf);

    OpenAIClient client = OpenAIClient.builder().apiKey(API_KEY).httpClient(mockHttpClient).build();

    OpenAIClient.ResponsesRequest rr =
        OpenAIClient.ResponsesRequest.builder()
            .model("gpt-4o-mini")
            .addUserText("First")
            .addUserImageFileId("file_123")
            .addUserText("Second")
            .addUserImageUrl("data:image/png;base64,AAA")
            .build();

    client.getResponses(rr, Duration.ofSeconds(5)).join();

    HttpRequest sent = requestCaptor.getValue();
    assertNotNull(sent);
    assertEquals(URI.create("https://api.openai.com/v1/responses"), sent.uri());
    assertTrue(sent.bodyPublisher().isPresent());
    String body = bodyPublisherToString(sent.bodyPublisher().get());

    ObjectMapper om = new ObjectMapper();
    JsonNode root = om.readTree(body);
    JsonNode input = root.get("input");
    assertNotNull(input);
    assertTrue(input.isArray());
    assertEquals(4, input.size());

    assertEquals("user", input.get(0).get("role").asText());
    assertEquals("input_text", input.get(0).get("content").get(0).get("type").asText());
    assertEquals("First", input.get(0).get("content").get(0).get("text").asText());

    assertEquals("input_image", input.get(1).get("content").get(0).get("type").asText());
    assertEquals("file_123", input.get(1).get("content").get(0).get("file_id").asText());

    assertEquals("input_text", input.get(2).get("content").get(0).get("type").asText());
    assertEquals("Second", input.get(2).get("content").get(0).get("text").asText());

    assertEquals("input_image", input.get(3).get("content").get(0).get("type").asText());
    assertEquals(
        "data:image/png;base64,AAA", input.get(3).get("content").get(0).get("image_url").asText());
  }

  private static String bodyPublisherToString(BodyPublisher bodyPublisher) {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    bodyPublisher.subscribe(
        new Flow.Subscriber<>() {

          @Override
          public void onSubscribe(Flow.Subscription subscription) {
            subscription.request(Long.MAX_VALUE);
          }

          @Override
          public void onNext(ByteBuffer item) {
            byte[] buf = new byte[item.remaining()];
            item.get(buf);
            try {
              baos.write(buf);
            } catch (IOException e) {
              throw new UncheckedIOException(e);
            }
          }

          @Override
          public void onError(Throwable throwable) {}

          @Override
          public void onComplete() {}
        });
    return baos.toString(StandardCharsets.UTF_8);
  }

  private static byte[] bodyPublisherToBytes(BodyPublisher bodyPublisher) {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    bodyPublisher.subscribe(
        new Flow.Subscriber<>() {
          @Override
          public void onSubscribe(Flow.Subscription subscription) {
            subscription.request(Long.MAX_VALUE);
          }

          @Override
          public void onNext(ByteBuffer item) {
            byte[] buf = new byte[item.remaining()];
            item.get(buf);
            baos.writeBytes(buf);
          }

          @Override
          public void onError(Throwable throwable) {
            throw new RuntimeException(throwable);
          }

          @Override
          public void onComplete() {}
        });
    return baos.toByteArray();
  }

  @Test
  public void testDownloadFileContentSuccess() throws IOException, InterruptedException {
    HttpClient mockHttpClient = mock(HttpClient.class);
    HttpResponse<String> mockResponse = mock(HttpResponse.class);

    when(mockResponse.statusCode()).thenReturn(200);
    String fileContent =
        """
        {"a" : "b"}
        {"c" : "d"}
        """;
    when(mockResponse.body()).thenReturn(fileContent);
    when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(mockResponse);

    OpenAIClient openAIClient =
        OpenAIClient.builder().apiKey(API_KEY).httpClient(mockHttpClient).build();

    OpenAIClient.DownloadFileContentResponse downloadFileContentResponse =
        openAIClient.downloadFileContent(
            new OpenAIClient.DownloadFileContentRequest("id-for-test"));

    assertEquals(fileContent, downloadFileContentResponse.content());
  }

  @Test
  public void testDownloadFileContentError() throws IOException, InterruptedException {
    HttpClient mockHttpClient = mock(HttpClient.class);
    HttpResponse<String> mockResponse = mock(HttpResponse.class);

    when(mockResponse.statusCode()).thenReturn(404);
    String body =
        """
        {
          "error": {
            "message": "No such File object: id-for-test",
            "type": "invalid_request_error",
            "param": "id",
            "code": null
          }
        }
        """;
    when(mockResponse.body()).thenReturn(body);
    when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(mockResponse);

    OpenAIClient openAIClient =
        OpenAIClient.builder().apiKey(API_KEY).httpClient(mockHttpClient).build();

    OpenAIClientResponseException openAIClientResponseException =
        assertThrows(
            OpenAIClientResponseException.class,
            () ->
                openAIClient.downloadFileContent(
                    new OpenAIClient.DownloadFileContentRequest("id-for-test")));
    assertEquals(body, openAIClientResponseException.httpResponse.body());
    assertEquals(404, openAIClientResponseException.httpResponse.statusCode());
  }

  @Test
  public void testCreateBatchSuccess() throws IOException, InterruptedException {

    HttpClient mockHttpClient = mock(HttpClient.class);
    HttpResponse<String> mockResponse = mock(HttpResponse.class);

    when(mockResponse.statusCode()).thenReturn(200);
    String body =
        """
        {
          "id": "batch_67199315c20081909074e442115938a2",
          "object": "batch",
          "endpoint": "/v1/chat/completions",
          "errors": null,
          "input_file_id": "file-pp1I2zv79eAnm47wt6rCNL5a",
          "completion_window": "24h",
          "status": "validating",
          "output_file_id": null,
          "error_file_id": null,
          "created_at": 1729729301,
          "in_progress_at": null,
          "expires_at": 1729815701,
          "finalizing_at": null,
          "completed_at": null,
          "failed_at": null,
          "expired_at": null,
          "cancelling_at": null,
          "cancelled_at": null,
          "request_counts": {
            "total": 0,
            "completed": 0,
            "failed": 0
          },
          "metadata": {
            "k1": "v1",
            "k2": "v2"
          }
        }
        """;
    when(mockResponse.body()).thenReturn(body);
    when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(mockResponse);

    OpenAIClient openAIClient =
        OpenAIClient.builder().apiKey(API_KEY).httpClient(mockHttpClient).build();

    OpenAIClient.CreateBatchResponse batch =
        openAIClient.createBatch(
            OpenAIClient.CreateBatchRequest.forChatCompletion(
                "file-pp1I2zv79eAnm47wt6rCNL5a", Map.of("k1", "v1", "k2", "v2")));
    assertEquals("batch_67199315c20081909074e442115938a2", batch.id());
    assertEquals("file-pp1I2zv79eAnm47wt6rCNL5a", batch.inputFileId());
    assertEquals("v1", batch.metadata().get("k1"));
  }

  @Test
  public void testCreateBatchError() throws IOException, InterruptedException {

    HttpClient mockHttpClient = mock(HttpClient.class);
    HttpResponse<String> mockResponse = mock(HttpResponse.class);

    when(mockResponse.statusCode()).thenReturn(400);
    String body =
        """
        {
          "error": {
            "message": "Invalid 'input_file_id': 'wrong-id'. Expected an ID that begins with 'file'.",
            "type": "invalid_request_error",
            "param": "input_file_id",
            "code": "invalid_value"
          }
        }""";
    when(mockResponse.body()).thenReturn(body);
    when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(mockResponse);

    OpenAIClient openAIClient =
        OpenAIClient.builder().apiKey(API_KEY).httpClient(mockHttpClient).build();

    OpenAIClientResponseException openAIClientResponseException =
        assertThrows(
            OpenAIClientResponseException.class,
            () ->
                openAIClient.createBatch(
                    OpenAIClient.CreateBatchRequest.forChatCompletion("wrong-id", null)));
    assertEquals(body, openAIClientResponseException.httpResponse.body());
    assertEquals(400, openAIClientResponseException.httpResponse.statusCode());
  }

  @Test
  public void testRetrieveBatchSuccess() throws IOException, InterruptedException {

    HttpClient mockHttpClient = mock(HttpClient.class);
    HttpResponse<String> mockResponse = mock(HttpResponse.class);

    when(mockResponse.statusCode()).thenReturn(200);
    String body =
        """
        {
          "id": "batch_67199315c20081909074e442115938a2",
          "object": "batch",
          "endpoint": "/v1/chat/completions",
          "errors": null,
          "input_file_id": "file-pp1I2zv79eAnm47wt6rCNL5a",
          "completion_window": "24h",
          "status": "validating",
          "output_file_id": null,
          "error_file_id": null,
          "created_at": 1729729301,
          "in_progress_at": null,
          "expires_at": 1729815701,
          "finalizing_at": null,
          "completed_at": null,
          "failed_at": null,
          "expired_at": null,
          "cancelling_at": null,
          "cancelled_at": null,
          "request_counts": {
            "total": 0,
            "completed": 0,
            "failed": 0
          },
          "metadata": {
            "k1": "v1",
            "k2": "v2"
          }
        }
        """;
    when(mockResponse.body()).thenReturn(body);
    when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(mockResponse);

    OpenAIClient openAIClient =
        OpenAIClient.builder().apiKey(API_KEY).httpClient(mockHttpClient).build();

    OpenAIClient.RetrieveBatchResponse batch =
        openAIClient.retrieveBatch(
            new OpenAIClient.RetrieveBatchRequest("batch_67199315c20081909074e442115938a2"));
    assertEquals("batch_67199315c20081909074e442115938a2", batch.id());
    assertEquals("file-pp1I2zv79eAnm47wt6rCNL5a", batch.inputFileId());
    assertEquals("v1", batch.metadata().get("k1"));
  }

  @Test
  public void testRetrieveBatchError() throws IOException, InterruptedException {

    HttpClient mockHttpClient = mock(HttpClient.class);
    HttpResponse<String> mockResponse = mock(HttpResponse.class);

    when(mockResponse.statusCode()).thenReturn(400);
    String body =
        """
        {
          "error": {
            "message": "Invalid 'input_file_id': 'wrong-id'. Expected an ID that begins with 'file'.",
            "type": "invalid_request_error",
            "param": "input_file_id",
            "code": "invalid_value"
          }
        }""";
    when(mockResponse.body()).thenReturn(body);
    when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(mockResponse);

    OpenAIClient openAIClient =
        OpenAIClient.builder().apiKey(API_KEY).httpClient(mockHttpClient).build();

    OpenAIClientResponseException openAIClientResponseException =
        assertThrows(
            OpenAIClientResponseException.class,
            () ->
                openAIClient.createBatch(
                    OpenAIClient.CreateBatchRequest.forChatCompletion("wrong-id", null)));
    assertEquals(body, openAIClientResponseException.httpResponse.body());
    assertEquals(400, openAIClientResponseException.httpResponse.statusCode());
  }
}

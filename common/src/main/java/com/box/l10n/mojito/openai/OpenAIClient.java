package com.box.l10n.mojito.openai;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.jsonSchema.JsonSchemaGenerator;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class OpenAIClient {

  final String apiKey;

  final String host;

  final ObjectMapper objectMapper;

  final HttpClient httpClient;

  final Executor asyncExecutor;

  OpenAIClient(
      String apiKey,
      String host,
      ObjectMapper objectMapper,
      HttpClient httpClient,
      Executor asyncExecutor) {
    this.apiKey = Objects.requireNonNull(apiKey);
    this.host = Objects.requireNonNull(host);
    this.objectMapper = Objects.requireNonNull(objectMapper);
    this.httpClient = Objects.requireNonNull(httpClient);
    this.asyncExecutor = Objects.requireNonNull(asyncExecutor);
  }

  public static class Builder {

    private String apiKey;

    private String host = "https://api.openai.com";

    private ObjectMapper objectMapper;

    private HttpClient httpClient;

    private Executor asyncExecutor;

    public Builder() {}

    public Builder apiKey(String apiKey) {
      this.apiKey = Objects.requireNonNull(apiKey);
      return this;
    }

    public Builder host(String host) {
      this.host = Objects.requireNonNull(host);
      return this;
    }

    public Builder objectMapper(ObjectMapper objectMapper) {
      this.objectMapper = Objects.requireNonNull(objectMapper);
      return this;
    }

    public Builder httpClient(HttpClient httpClient) {
      this.httpClient = Objects.requireNonNull(httpClient);
      return this;
    }

    public Builder asyncExecutor(Executor asyncExecutor) {
      this.asyncExecutor = asyncExecutor;
      return this;
    }

    public OpenAIClient build() {
      if (apiKey == null) {
        throw new IllegalStateException("API key must be provided");
      }

      if (objectMapper == null) {
        objectMapper = createObjectMapper();
      }
      if (httpClient == null) {
        httpClient = createHttpClient();
      }

      if (asyncExecutor == null) {
        asyncExecutor = ForkJoinPool.commonPool();
      }

      return new OpenAIClient(apiKey, host, objectMapper, httpClient, asyncExecutor);
    }

    private HttpClient createHttpClient() {
      return HttpClient.newBuilder().build();
    }

    private ObjectMapper createObjectMapper() {
      var objectMapper = new ObjectMapper();
      objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
      objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
      objectMapper.registerModule(new JavaTimeModule());
      return objectMapper;
    }
  }

  public static Builder builder() {
    return new Builder();
  }

  public CompletableFuture<ChatCompletionsResponse> getChatCompletions(
      ChatCompletionsRequest chatCompletionsRequest, Duration httpRequestTimeout) {

    if (chatCompletionsRequest.stream()) {
      throw new IllegalArgumentException(
          "chatCompletionsRequest must have the \"stream\" attribute set to \"false\"");
    }

    String payload;
    try {
      payload = objectMapper.writeValueAsString(chatCompletionsRequest);
    } catch (JsonProcessingException e) {
      throw new RuntimeException("Can't serialize ChatCompletionsRequest", e);
    }

    HttpRequest request =
        HttpRequest.newBuilder()
            .timeout(httpRequestTimeout)
            .uri(getUriForEndpoint(ChatCompletionsRequest.ENDPOINT))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + this.apiKey)
            .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
            .build();

    CompletableFuture<ChatCompletionsResponse> chatCompletionsResponse =
        httpClient
            .sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenApplyAsync(
                httpResponse -> {
                  if (httpResponse.statusCode() != 200) {
                    throw new OpenAIClientResponseException("ChatCompletion failed", httpResponse);
                  } else {
                    try {
                      return objectMapper.readValue(
                          httpResponse.body(), ChatCompletionsResponse.class);
                    } catch (JsonProcessingException e) {
                      throw new OpenAIClientResponseException(
                          "Can't deserialize ChatCompletionsResponse", e, httpResponse);
                    }
                  }
                },
                asyncExecutor);

    return chatCompletionsResponse;
  }

  public CompletableFuture<Stream<ChatCompletionsStreamResponse>> streamChatCompletions(
      ChatCompletionsRequest chatCompletionsRequest) {

    if (!chatCompletionsRequest.stream()) {
      throw new IllegalArgumentException(
          "chatCompletionsRequest must have the \"stream\" attribute set to \"true\"");
    }

    String requestPayload;
    try {
      requestPayload = objectMapper.writeValueAsString(chatCompletionsRequest);
    } catch (JsonProcessingException e) {
      throw new RuntimeException("Can't serialize ChatCompletionsRequest", e);
    }

    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(getUriForEndpoint(ChatCompletionsRequest.ENDPOINT))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + this.apiKey)
            .POST(HttpRequest.BodyPublishers.ofString(requestPayload, StandardCharsets.UTF_8))
            .build();

    CompletableFuture<Stream<ChatCompletionsStreamResponse>> streamCompletableFuture =
        httpClient
            .sendAsync(request, HttpResponse.BodyHandlers.ofLines())
            .thenApply(
                httpResponse -> {
                  if (httpResponse.statusCode() != 200) {
                    throw new OpenAIClientResponseException(
                        "ChatCompletion stream failed", httpResponse);
                  }
                  return httpResponse
                      .body()
                      .takeWhile(s -> !"data: [DONE]".equals(s))
                      .filter(Predicate.not(String::isBlank))
                      .map(
                          body -> {
                            if (!body.startsWith("data: ")) {
                              throw new OpenAIClientResponseException(
                                  "Only support \"data\" lines in stream are supported, got: \"%s\""
                                      .formatted(body),
                                  httpResponse);
                            }

                            String jsonPart = body.substring(5);
                            try {
                              return objectMapper.readValue(
                                  jsonPart, ChatCompletionsStreamResponse.class);
                            } catch (JsonProcessingException e) {
                              throw new OpenAIClientResponseException(
                                  "Can't deserialize ChatCompletionsStreamResponse",
                                  e,
                                  httpResponse);
                            }
                          });
                });

    return streamCompletableFuture;
  }

  public record ChatCompletionsRequest(
      String model,
      List<Message> messages,
      Integer seed,
      Boolean stream,
      Float temperature,
      @JsonProperty("max_completion_tokens") Integer maxCompletionTokens,
      @JsonProperty("top_p") Float topP,
      @JsonProperty("frequency_penalty") Float frequencyPenalty,
      @JsonProperty("presence_penalty") Float presencePenalty,
      @JsonProperty("response_format") ResponseFormat responseFormat) {

    static String ENDPOINT = "/v1/chat/completions";

    public enum Models {
      GPT_3_5_TURBO("gpt-3.5-turbo");

      @JsonValue String name;

      Models(String name) {
        this.name = name;
      }
    }

    public interface ResponseFormat {}

    public record JsonFormat(String type, @JsonProperty("json_schema") JsonSchema jsonSchema)
        implements ResponseFormat {

      public record JsonSchema(boolean strict, String name, Object schema) {

        public static ObjectNode createJsonSchema(Class<?> type) {
          ObjectMapper objectMapper = new ObjectMapper();
          JsonSchemaGenerator schemaGen = new JsonSchemaGenerator(objectMapper);
          com.fasterxml.jackson.module.jsonSchema.JsonSchema baseSchema = null;
          try {
            baseSchema = schemaGen.generateSchema(type);
          } catch (JsonMappingException e) {
            throw new RuntimeException(e);
          }
          JsonNode schemaNode = objectMapper.valueToTree(baseSchema);
          ObjectNode rootNode = (ObjectNode) schemaNode;
          enhanceSchema(rootNode);
          return rootNode;
        }

        private static void enhanceSchema(ObjectNode objectNode) {

          if (!objectNode.has("type")) {
            objectNode.put("type", "object");
          }
          objectNode.put("additionalProperties", false);

          if (objectNode.has("properties")) {
            ObjectNode propertiesNode = (ObjectNode) objectNode.get("properties");
            ArrayNode requiredFields = objectNode.putArray("required");

            Iterator<Map.Entry<String, JsonNode>> fields = propertiesNode.fields();
            while (fields.hasNext()) {
              Map.Entry<String, JsonNode> field = fields.next();
              String fieldName = field.getKey();
              requiredFields.add(fieldName);

              JsonNode fieldSchema = field.getValue();
              if (fieldSchema.isObject()) {
                ObjectNode fieldObjectNode = (ObjectNode) fieldSchema;

                String fieldType =
                    fieldObjectNode.has("type") ? fieldObjectNode.get("type").asText() : null;

                if ("object".equals(fieldType) && fieldObjectNode.has("properties")) {
                  enhanceSchema(fieldObjectNode);
                } else if ("array".equals(fieldType) && fieldObjectNode.has("items")) {
                  enhanceArrayItems(fieldObjectNode);
                }
              }
            }
          }
        }

        private static void enhanceArrayItems(ObjectNode arrayNode) {
          JsonNode itemsNode = arrayNode.get("items");
          if (itemsNode != null && itemsNode.isObject()) {
            ObjectNode itemsObjectNode = (ObjectNode) itemsNode;

            if (itemsObjectNode.has("properties")) {
              enhanceSchema(itemsObjectNode);
            }

            if (!itemsObjectNode.has("additionalProperties")) {
              itemsObjectNode.put("additionalProperties", false);
            }
          }
        }
      }
    }

    public interface Message {
      String role();

      String content();

      String name();
    }

    public record SystemMessage(String role, String content, String name) implements Message {

      public static class SystemMessageBuilder {
        private String role = "system";
        private String content;
        private String name;

        private SystemMessageBuilder() {}

        public SystemMessageBuilder role(String role) {
          this.role = role;
          return this;
        }

        public SystemMessageBuilder content(String content) {
          this.content = content;
          return this;
        }

        public SystemMessageBuilder name(String name) {
          this.name = name;
          return this;
        }

        public SystemMessage build() {
          return new SystemMessage(role, content, name);
        }
      }

      public static SystemMessageBuilder systemMessageBuilder() {
        return new SystemMessageBuilder();
      }
    }

    public record UserMessage(String role, String content, String name) implements Message {

      public static class UserMessageBuilder {
        private String role = "user";
        private String content;
        private String name;

        private UserMessageBuilder() {}

        public UserMessageBuilder role(String role) {
          this.role = role;
          return this;
        }

        public UserMessageBuilder content(String content) {
          this.content = content;
          return this;
        }

        public UserMessageBuilder name(String name) {
          this.name = name;
          return this;
        }

        public UserMessage build() {
          return new UserMessage(role, content, name);
        }
      }

      public static UserMessageBuilder userMessageBuilder() {
        return new UserMessageBuilder();
      }
    }

    public static class Builder {
      private String model;
      private List<Message> messages;
      private Integer seed;
      private Boolean stream = false;
      private Float temperature;
      private Integer maxCompletionTokens;
      private Float topP;
      private Float frequencyPenalty;
      private Float presencePenalty;
      private ResponseFormat responseFormat;

      public Builder model(Models model) {
        return model(model.name);
      }

      public Builder model(String model) {
        this.model = model;
        return this;
      }

      public Builder messages(List<Message> messages) {
        this.messages = messages;
        return this;
      }

      public Builder seed(Integer seed) {
        this.seed = seed;
        return this;
      }

      public Builder stream(Boolean stream) {
        this.stream = stream;
        return this;
      }

      public Builder temperature(Float temperature) {
        this.temperature = temperature;
        return this;
      }

      public Builder maxCompletionTokens(Integer maxTokens) {
        this.maxCompletionTokens = maxTokens;
        return this;
      }

      public Builder topP(Float topP) {
        this.topP = topP;
        return this;
      }

      public Builder frequencyPenalty(Float frequencyPenalty) {
        this.frequencyPenalty = frequencyPenalty;
        return this;
      }

      public Builder presencePenalty(Float presencePenalty) {
        this.presencePenalty = presencePenalty;
        return this;
      }

      public Builder responseFormat(ResponseFormat responseFormat) {
        this.responseFormat = responseFormat;
        return this;
      }

      public ChatCompletionsRequest build() {
        return new ChatCompletionsRequest(
            model,
            messages,
            seed,
            stream,
            temperature,
            maxCompletionTokens,
            topP,
            frequencyPenalty,
            presencePenalty,
            responseFormat);
      }
    }

    public static Builder chatCompletionsRequest() {
      return new Builder();
    }
  }

  public record ChatCompletionsResponse(
      String id,
      String object,
      Instant created,
      String model,
      List<Choice> choices,
      Usage usage,
      @JsonProperty("system_fingerprint") String systemFingerprint) {

    public record Choice(
        int index, Message message, @JsonProperty("finish_reason") String finishReason) {

      public record Message(String role, String content) {}
    }

    public record Usage(
        @JsonProperty("prompt_tokens") int promptTokens,
        @JsonProperty("completion_tokens") int completionTokens,
        @JsonProperty("total_tokens") int totalTokens) {}
  }

  public record ChatCompletionsStreamResponse(
      String id,
      String object,
      Instant created,
      String model,
      @JsonProperty("system_fingerprint") String systemFingerprint,
      List<Choice> choices) {

    public record Choice(
        int index, Delta delta, @JsonProperty("finish_reason") String finishReason) {

      public enum FinishReasons {
        STOP("stop");

        String value;

        FinishReasons(String name) {
          this.value = name;
        }

        public String getValue() {
          return value;
        }
      }

      public record Delta(String content) {}
    }
  }

  public CompletableFuture<EmbeddingResponse> getEmbedding(EmbeddingRequest embeddingRequest) {

    String requestBody;
    try {
      requestBody = objectMapper.writeValueAsString(embeddingRequest);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }

    HttpRequest httpRequest =
        HttpRequest.newBuilder()
            .uri(getUriForEndpoint(EmbeddingRequest.ENDPOINT))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + this.apiKey)
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build();

    CompletableFuture<EmbeddingResponse> embeddingResponse =
        httpClient
            .sendAsync(httpRequest, HttpResponse.BodyHandlers.ofString())
            .thenApply(
                httpResponse -> {
                  try {
                    if (httpResponse.statusCode() != 200) {
                      throw new OpenAIClientResponseException("Embedding failed", httpResponse);
                    }
                    return objectMapper.readValue(httpResponse.body(), EmbeddingResponse.class);
                  } catch (JsonProcessingException e) {
                    throw new OpenAIClientResponseException(
                        "Can't deserialize EmbeddingResponse", e, httpResponse);
                  }
                });

    return embeddingResponse;
  }

  public record EmbeddingRequest(String input, String model) {

    static String ENDPOINT = "/v1/embeddings";

    public enum Models {
      TEXT_EMBEDDING_3_SMALL("text-embedding-3-small");

      @JsonValue String name;

      Models(String name) {
        this.name = name;
      }
    }

    public static class Builder {
      private String input;
      private String model;

      protected Builder() {}

      public Builder input(String input) {
        this.input = input;
        return this;
      }

      public Builder model(Models model) {
        return model(model.name);
      }

      public Builder model(String model) {
        this.model = model;
        return this;
      }

      public EmbeddingRequest build() {
        return new EmbeddingRequest(input, model);
      }
    }

    public static Builder builder() {
      return new Builder();
    }
  }

  public record EmbeddingResponse(
      String object, List<EmbeddingElement> data, String model, Usage usage) {

    public record EmbeddingElement(String object, List<Double> embedding, int index) {}

    public record Usage(
        @JsonProperty("prompt_tokens") int promptTokens,
        @JsonProperty("total_tokens") int totalTokens) {}
  }

  public UploadFileResponse uploadFile(UploadFileRequest uploadFileRequest) {

    final String boundary = UUID.randomUUID().toString();

    HttpRequest.BodyPublisher body = uploadFileRequest.getMultipartBodyPublisher(boundary);

    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(getUriForEndpoint(UploadFileRequest.ENDPOINT))
            .header("Authorization", "Bearer " + apiKey)
            .header("Content-Type", "multipart/form-data; boundary=" + boundary)
            .POST(body)
            .build();

    HttpResponse<String> response;
    try {
      response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    } catch (IOException | InterruptedException e) {
      throw new RuntimeException(e);
    }

    if (response.statusCode() != 200) {
      throw new OpenAIClientResponseException("Can't upload file", response);
    }

    UploadFileResponse uploadFileResponse;
    try {
      uploadFileResponse = objectMapper.readValue(response.body(), UploadFileResponse.class);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
    return uploadFileResponse;
  }

  public record UploadFileRequest(String purpose, String filename, FileContent fileContent) {

    static final String ENDPOINT = "/v1/files";

    sealed interface FileContent permits TextContent, BinaryContent {
      String contentType();
    }

    record TextContent(String value, String contentType) implements FileContent {}

    record BinaryContent(byte[] value, String contentType) implements FileContent {}

    public static UploadFileRequest forBatch(String filename, String content) {
      return new UploadFileRequest(
          Purpose.BATCH.toString(), filename, new TextContent(content, "application/json"));
    }

    public static UploadFileRequest forVision(String filename, byte[] content, String contentType) {
      return new UploadFileRequest(
          Purpose.VISION.toString(), filename, new BinaryContent(content, contentType));
    }

    enum Purpose {
      BATCH("batch"),
      ASSISTANTS("assistants"),
      FINE_TUNE("fine-tune"),
      VISION("vision");

      private final String purposeCode;

      Purpose(String purposeCode) {
        this.purposeCode = purposeCode;
      }

      public String getPurposeCode() {
        return purposeCode;
      }

      @Override
      public String toString() {
        return purposeCode;
      }

      public static Purpose fromCode(String purposeCode) {
        for (Purpose purpose : Purpose.values()) {
          if (purpose.purposeCode.equalsIgnoreCase(purposeCode)) {
            return purpose;
          }
        }
        throw new IllegalArgumentException("Unknown purpose: " + purposeCode);
      }
    }

    HttpRequest.BodyPublisher getMultipartBodyPublisher(String boundary) {
      String part1 =
          "--"
              + boundary
              + "\r\n"
              + "Content-Disposition: form-data; name=\"purpose\"\r\n\r\n"
              + purpose
              + "\r\n";

      String part2Header =
          "--"
              + boundary
              + "\r\n"
              + "Content-Disposition: form-data; name=\"file\"; filename=\""
              + filename
              + "\"\r\n"
              + "Content-Type: "
              + fileContent().contentType()
              + "\r\n\r\n";

      String ending = "\r\n--" + boundary + "--\r\n";

      return switch (fileContent()) {
        case TextContent t ->
            HttpRequest.BodyPublishers.ofByteArrays(
                List.of(
                    part1.getBytes(),
                    part2Header.getBytes(),
                    t.value().getBytes(StandardCharsets.UTF_8),
                    ending.getBytes()));
        case BinaryContent b ->
            HttpRequest.BodyPublishers.ofByteArrays(
                List.of(part1.getBytes(), part2Header.getBytes(), b.value(), ending.getBytes()));
      };
    }
  }

  public record UploadFileResponse(
      String object,
      String id,
      String purpose,
      String filename,
      int bytes,
      @JsonProperty("created_at") long createdAt,
      String status,
      @JsonProperty("status_details") String statusDetails) {}

  public record RequestBatchFileLine(
      @JsonProperty("custom_id") String customId, String method, String url, Object body) {

    public static RequestBatchFileLine forChatCompletion(
        String customId, ChatCompletionsRequest chatCompletionsRequest) {
      return new RequestBatchFileLine(
          customId, "POST", "/v1/chat/completions", chatCompletionsRequest);
    }
  }

  public record ChatCompletionResponseBatchFileLine(
      String id, @JsonProperty("custom_id") String customId, Response response) {
    public record Response(
        @JsonProperty("status_code") int statusCode,
        @JsonProperty("request_id") String requestId,
        @JsonProperty("body") ChatCompletionsResponse chatCompletionsResponse) {}
  }

  public DownloadFileContentResponse downloadFileContent(
      DownloadFileContentRequest downloadFileContentRequest) {
    HttpResponse<String> response;
    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(
                getUriForEndpoint(
                    DownloadFileContentRequest.ENDPOINT.formatted(
                        downloadFileContentRequest.fileId())))
            .header("Authorization", "Bearer " + apiKey)
            .header("Content-Type", "application/json")
            .GET()
            .build();

    try {
      response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    } catch (IOException | InterruptedException e) {
      throw new RuntimeException(
          "Error while sending the request to download the file: "
              + downloadFileContentRequest.fileId(),
          e);
    }

    if (response.statusCode() != 200) {
      throw new OpenAIClientResponseException("Can't download file content", response);
    }

    return new DownloadFileContentResponse(response.body());
  }

  public record DownloadFileContentRequest(String fileId) {
    static final String ENDPOINT = "/v1/files/%s/content";
  }

  public record DownloadFileContentResponse(String content) {}

  public CreateBatchResponse createBatch(CreateBatchRequest createBatchRequest) {

    String jsonBody;
    try {
      jsonBody = objectMapper.writeValueAsString(createBatchRequest);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }

    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(getUriForEndpoint(CreateBatchRequest.ENDPOINT))
            .header("Authorization", "Bearer " + apiKey)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
            .build();

    HttpResponse<String> response;
    try {
      response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    } catch (IOException | InterruptedException e) {
      throw new RuntimeException(e);
    }

    if (response.statusCode() != 200) {
      throw new OpenAIClientResponseException("Can't create batch", response);
    }

    CreateBatchResponse createBatchResponse;
    try {
      createBatchResponse = objectMapper.readValue(response.body(), CreateBatchResponse.class);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }

    return createBatchResponse;
  }

  public record CreateBatchRequest(
      @JsonProperty("input_file_id") String inputFileId,
      String endpoint,
      @JsonProperty("completion_window") String completionWindow,
      Map<String, String> metadata) {

    public static final String ENDPOINT = "/v1/batches";

    public static CreateBatchRequest forChatCompletion(
        String fileId, Map<String, String> metadata) {
      return new CreateBatchRequest(fileId, "/v1/chat/completions", "24h", metadata);
    }
  }

  public record CreateBatchResponse(
      String id,
      String object,
      String endpoint,
      String errors,
      @JsonProperty("input_file_id") String inputFileId,
      @JsonProperty("completion_window") String completionWindow,
      String status,
      @JsonProperty("output_file_id") String outputFileId,
      @JsonProperty("error_file_id") String errorFileId,
      @JsonProperty("created_at") long createdAt,
      @JsonProperty("in_progress_at") Long inProgressAt,
      @JsonProperty("expires_at") long expiresAt,
      @JsonProperty("completed_at") Long completedAt,
      @JsonProperty("failed_at") Long failedAt,
      @JsonProperty("expired_at") Long expiredAt,
      @JsonProperty("request_counts") RequestCounts requestCounts,
      Map<String, String> metadata) {
    record RequestCounts(int total, int completed, int failed) {}
  }

  public RetrieveBatchResponse retrieveBatch(RetrieveBatchRequest retrieveBatchRequest) {
    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(
                getUriForEndpoint(
                    RetrieveBatchRequest.ENDPOINT.formatted(retrieveBatchRequest.batchId())))
            .header("Authorization", "Bearer " + apiKey)
            .header("Content-Type", "application/json")
            .GET()
            .build();

    HttpResponse<String> response;
    try {
      response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    } catch (IOException | InterruptedException e) {
      throw new RuntimeException(e);
    }

    if (response.statusCode() != 200) {
      throw new OpenAIClientResponseException("Can't retrieve batch", response);
    }

    RetrieveBatchResponse retrieveBatchResponse;
    try {
      retrieveBatchResponse = objectMapper.readValue(response.body(), RetrieveBatchResponse.class);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }

    return retrieveBatchResponse;
  }

  public record RetrieveBatchRequest(String batchId) {
    public static final String ENDPOINT = "/v1/batches/%s";
  }

  public record RetrieveBatchResponse(
      String id,
      String object,
      String endpoint,
      Errors errors,
      @JsonProperty("input_file_id") String inputFileId,
      @JsonProperty("completion_window") String completionWindow,
      String status,
      @JsonProperty("output_file_id") String outputFileId,
      @JsonProperty("error_file_id") String errorFileId,
      @JsonProperty("created_at") long createdAt,
      @JsonProperty("in_progress_at") Long inProgressAt,
      @JsonProperty("expires_at") long expiresAt,
      @JsonProperty("completed_at") Long completedAt,
      @JsonProperty("failed_at") Long failedAt,
      @JsonProperty("expired_at") Long expiredAt,
      @JsonProperty("request_counts") RequestCounts requestCounts,
      Map<String, String> metadata) {
    public record RequestCounts(int total, int completed, int failed) {}

    public record Errors(@JsonProperty("object") String objectType, List<ErrorDetail> data) {}

    public record ErrorDetail(String code, String message, String param, Integer line) {}
  }

  private URI getUriForEndpoint(String endpoint) {
    return URI.create(host).resolve(endpoint);
  }

  public static class OpenAIClientResponseException extends RuntimeException {
    HttpResponse<?> httpResponse;

    public OpenAIClientResponseException(String message, HttpResponse<?> httpResponse) {
      super(message);
      this.httpResponse = Objects.requireNonNull(httpResponse);
    }

    public OpenAIClientResponseException(
        String message, Exception e, HttpResponse<?> httpResponse) {
      super(message, e);
      this.httpResponse = Objects.requireNonNull(httpResponse);
    }

    @Override
    public String toString() {

      String bodyAsStr;
      if (httpResponse.body() instanceof Stream) {
        bodyAsStr =
            ((Stream<?>) httpResponse.body()).map(Object::toString).collect(Collectors.joining());
      } else {
        bodyAsStr = httpResponse.body().toString();
      }

      return "OpenAIHttpClientException{"
          + "message='"
          + getMessage()
          + '\''
          + ", httpResponse="
          + httpResponse
          + ", httpResponse.body="
          + bodyAsStr
          + '}';
    }
  }

  public static class TemperatureHelper {
    public static float getTemperatureForReasoningModels(String model) {
      return model.startsWith("o") || model.startsWith("gpt-5") ? 1 : 0;
    }
  }
}

package com.box.l10n.mojito.openai;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class OpenAIClient {

  final String apiKey;

  final String host;

  final ObjectMapper objectMapper;

  final HttpClient httpClient;

  final Map<String, String> customHeaders;

  final Duration requestTimeout;

  OpenAIClient(
      String apiKey,
      String host,
      ObjectMapper objectMapper,
      HttpClient httpClient,
      Map<String, String> customHeaders,
      Duration requestTimeout) {
    this.apiKey = apiKey;
    this.host = Objects.requireNonNull(host);
    this.objectMapper = Objects.requireNonNull(objectMapper);
    this.httpClient = Objects.requireNonNull(httpClient);
    this.customHeaders = customHeaders;
    this.requestTimeout = requestTimeout;
  }

  public static class Builder {

    private String apiKey;

    private String host = "https://api.openai.com";

    private ObjectMapper objectMapper;

    private HttpClient httpClient;

    private Map<String, String> customHeaders;

    private Duration requestTimeout = Duration.ofSeconds(60);

    public Builder() {}

    public Builder apiKey(String apiKey) {
      this.apiKey = apiKey;
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

    public Builder customHeaders(Map<String, String> customHeaders) {
      this.customHeaders = customHeaders;
      return this;
    }

    public Builder timeout(Duration timeout) {
      this.requestTimeout = timeout;
      return this;
    }

    public OpenAIClient build() {

      if (objectMapper == null) {
        objectMapper = createObjectMapper();
      }
      if (httpClient == null) {
        httpClient = createHttpClient();
      }
      return new OpenAIClient(
          apiKey, host, objectMapper, httpClient, customHeaders, requestTimeout);
    }

    private HttpClient createHttpClient() {
      return HttpClient.newHttpClient();
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
      ChatCompletionsRequest chatCompletionsRequest) {

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
            .uri(getUriForEndpoint(ChatCompletionsRequest.ENDPOINT))
            .headers(getHeaders())
            .timeout(requestTimeout)
            .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
            .build();

    CompletableFuture<ChatCompletionsResponse> chatCompletionsResponse =
        httpClient
            .sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenApply(
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
                });

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
            .headers(getHeaders())
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
      int seed,
      boolean stream,
      float temperature,
      @JsonProperty("response_format") ResponseFormat responseFormat,
      @JsonProperty("max_tokens") int maxTokens,
      @JsonProperty("top_p") float topP,
      @JsonProperty("frequency_penalty") float frequencyPenalty,
      @JsonProperty("presence_penalty") float presencePenalty) {

    static String ENDPOINT = "/v1/chat/completions";

    public enum Models {
      GPT_3_5_TURBO("gpt-3.5-turbo"),
      GPT_4_TURBO("gtp-4-turbo"),
      GPT_4o("gpt-4o");

      @JsonValue String name;

      Models(String name) {
        this.name = name;
      }
    }

    public interface Message {
      String role();

      String content();

      String name();
    }

    public record ResponseFormat(String type) {
      public static class ResponseFormatBuilder {
        private String type;

        private ResponseFormatBuilder() {}

        public ResponseFormatBuilder type(String type) {
          this.type = type;
          return this;
        }

        public ResponseFormat build() {
          return new ResponseFormat(type);
        }
      }

      public static ResponseFormatBuilder responseFormatBuilder() {
        return new ResponseFormatBuilder();
      }
    }

    public record ChatMessage(String role, String content, String name) implements Message {

      public static class ChatMessageBuilder {
        private String role = "user";
        private String content;
        private String name;

        private ChatMessageBuilder() {}

        public ChatMessageBuilder role(String role) {
          this.role = role;
          return this;
        }

        public ChatMessageBuilder content(String content) {
          this.content = content;
          return this;
        }

        public ChatMessageBuilder name(String name) {
          this.name = name;
          return this;
        }

        public ChatMessage build() {
          return new ChatMessage(role, content, name);
        }
      }

      public static ChatMessageBuilder messageBuilder() {
        return new ChatMessageBuilder();
      }
    }

    public static class Builder {
      private String model;
      private List<Message> messages;
      private int seed;
      private boolean stream = false;
      private float temperature;
      private int maxTokens = 256;
      private float topP;
      private float frequencyPenalty;
      private float presencePenalty;
      private ResponseFormat responseFormat =
          ResponseFormat.responseFormatBuilder().type("text").build();

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

      public Builder seed(int seed) {
        this.seed = seed;
        return this;
      }

      public Builder stream(boolean stream) {
        this.stream = stream;
        return this;
      }

      public Builder temperature(float temperature) {
        this.temperature = temperature;
        return this;
      }

      public Builder maxTokens(int maxTokens) {
        this.maxTokens = maxTokens;
        return this;
      }

      public Builder topP(float topP) {
        this.topP = topP;
        return this;
      }

      public Builder frequencyPenalty(float frequencyPenalty) {
        this.frequencyPenalty = frequencyPenalty;
        return this;
      }

      public Builder presencePenalty(float presencePenalty) {
        this.presencePenalty = presencePenalty;
        return this;
      }

      public Builder jsonResponseType(boolean isJson) {
        if (isJson) {
          this.responseFormat = ResponseFormat.responseFormatBuilder().type("json_object").build();
        }
        return this;
      }

      public ChatCompletionsRequest build() {
        return new ChatCompletionsRequest(
            model,
            messages,
            seed,
            stream,
            temperature,
            responseFormat,
            maxTokens,
            topP,
            frequencyPenalty,
            presencePenalty);
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
        STOP("stop"),
        LENGTH("length"),
        FUNCTION_CALL("function_call"),
        CONTENT_FILTER("content_filter"),
        NULL("null");

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

  private String[] getHeaders() {
    List<String> headersList = new ArrayList<>();
    headersList.add("Content-Type");
    headersList.add("application/json");
    if (apiKey != null && !apiKey.isBlank()) {
      headersList.add("Authorization");
      headersList.add("Bearer " + this.apiKey);
    }
    if (customHeaders != null && !customHeaders.isEmpty()) {
      customHeaders.forEach(
          (key, value) -> {
            headersList.add(key);
            headersList.add(value);
          });
    }
    return headersList.toArray(new String[0]);
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
            .headers(getHeaders())
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

  private URI getUriForEndpoint(String endpoint) {
    return URI.create(host).resolve(endpoint);
  }

  public class OpenAIClientResponseException extends RuntimeException {
    HttpResponse httpResponse;

    public OpenAIClientResponseException(String message, HttpResponse httpResponse) {
      super(message);
      this.httpResponse = Objects.requireNonNull(httpResponse);
    }

    public OpenAIClientResponseException(String message, Exception e, HttpResponse httpResponse) {
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
}

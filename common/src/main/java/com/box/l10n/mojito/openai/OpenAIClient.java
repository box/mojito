package com.box.l10n.mojito.openai;

import static com.box.l10n.mojito.openai.OpenAIClient.ResponsesRequest.TextContainer.JsonSchema.createJsonSchema;

import com.box.l10n.mojito.openai.OpenAIClient.ResponsesRequest.InputMessage.Content;
import com.box.l10n.mojito.openai.OpenAIClient.ResponsesRequest.InputMessage.ImageFileId;
import com.box.l10n.mojito.openai.OpenAIClient.ResponsesRequest.InputMessage.ImageUrl;
import com.box.l10n.mojito.openai.OpenAIClient.ResponsesRequest.TextContainer.JsonSchema;
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
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
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
import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.impl.DefaultAuthenticationStrategy;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.util.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenAIClient implements java.io.Closeable {

  static Logger logger = LoggerFactory.getLogger(OpenAIClient.class);

  final String apiKey;

  final String host;

  final ObjectMapper objectMapper;

  final CloseableHttpClient httpClient;

  final Executor asyncExecutor;

  OpenAIClient(
      String apiKey,
      String host,
      ObjectMapper objectMapper,
      CloseableHttpClient httpClient,
      Executor asyncExecutor) {
    this.apiKey = Objects.requireNonNull(apiKey);
    this.host = Objects.requireNonNull(host);
    this.objectMapper = Objects.requireNonNull(objectMapper);
    this.httpClient = Objects.requireNonNull(httpClient);
    this.asyncExecutor = Objects.requireNonNull(asyncExecutor);
  }

  @Override
  public void close() throws IOException {
    httpClient.close();
  }

  public static class Builder {

    private String apiKey;

    private String host = "https://api.openai.com";

    private ObjectMapper objectMapper;

    private CloseableHttpClient httpClient;

    private Executor asyncExecutor;

    private String proxyHost;
    private Integer proxyPort;
    private String proxyUser;
    private String proxyPassword;
    private List<String> proxyPreferredAuthSchemes;

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

    public Builder httpClient(CloseableHttpClient httpClient) {
      this.httpClient = Objects.requireNonNull(httpClient);
      return this;
    }

    public Builder asyncExecutor(Executor asyncExecutor) {
      this.asyncExecutor = asyncExecutor;
      return this;
    }

    public Builder proxyHost(String proxyHost) {
      this.proxyHost = proxyHost;
      return this;
    }

    public Builder proxyPort(Integer proxyPort) {
      this.proxyPort = proxyPort;
      return this;
    }

    public Builder proxyUser(String proxyUser) {
      this.proxyUser = proxyUser;
      return this;
    }

    public Builder proxyPassword(String proxyPassword) {
      this.proxyPassword = proxyPassword;
      return this;
    }

    public Builder proxyPreferredAuthSchemes(List<String> proxyPreferredAuthSchemes) {
      this.proxyPreferredAuthSchemes = proxyPreferredAuthSchemes;
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

    private CloseableHttpClient createHttpClient() {

      if (proxyHost == null) {
        return HttpClients.createDefault();
      }
      HttpHost proxy = new HttpHost("http", proxyHost, proxyPort != null ? proxyPort : 3128);
      var builder = HttpClients.custom().setProxy(proxy);
      if (proxyUser != null && proxyPassword != null) {
        BasicCredentialsProvider credsProvider = new BasicCredentialsProvider();
        credsProvider.setCredentials(
            new AuthScope(proxy),
            new UsernamePasswordCredentials(proxyUser, proxyPassword.toCharArray()));
        builder.setDefaultCredentialsProvider(credsProvider);

        if (proxyPreferredAuthSchemes != null && !proxyPreferredAuthSchemes.isEmpty()) {
          List<String> preferredOrder =
              proxyPreferredAuthSchemes.stream().map(String::toLowerCase).toList();
          builder.setProxyAuthenticationStrategy(
              // make sure that preferred schemes are tried before others
              (challengeType, challenges, context) ->
                  DefaultAuthenticationStrategy.INSTANCE
                      .select(challengeType, challenges, context)
                      .stream()
                      .sorted(
                          Comparator.comparingInt(
                              s -> {
                                int idx = preferredOrder.indexOf(s.getName().toLowerCase());
                                return idx >= 0 ? idx : preferredOrder.size();
                              }))
                      .toList());
        }
      }
      return builder.build();
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

  public CompletableFuture<ResponsesResponse> getResponses(
      ResponsesRequest responsesRequest, Duration httpRequestTimeout) {

    String payload;
    try {
      payload = objectMapper.writeValueAsString(responsesRequest);
    } catch (JsonProcessingException e) {
      throw new RuntimeException("Can't serialize ResponsesRequest", e);
    }

    return CompletableFuture.supplyAsync(
        () -> {
          HttpPost post = new HttpPost(getUriForEndpoint(ResponsesRequest.ENDPOINT));
          post.setHeader("Authorization", "Bearer " + apiKey);
          post.setEntity(new StringEntity(payload, ContentType.APPLICATION_JSON));
          post.setConfig(
              RequestConfig.custom()
                  .setResponseTimeout(Timeout.ofMilliseconds(httpRequestTimeout.toMillis()))
                  .build());

          try (CloseableHttpResponse response = httpClient.execute(post)) {
            int statusCode = response.getCode();
            String body = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            if (statusCode != 200) {
              throw new OpenAIClientResponseException("Responses API failed", statusCode, body);
            }
            try {
              return objectMapper.readValue(body, ResponsesResponse.class);
            } catch (JsonProcessingException e) {
              throw new OpenAIClientResponseException(
                  "Can't deserialize ResponsesResponse", e, statusCode, body);
            }
          } catch (OpenAIClientResponseException e) {
            throw e;
          } catch (IOException | ParseException e) {
            throw new RuntimeException(e);
          }
        },
        asyncExecutor);
  }

  public record ResponsesRequest(
      String model,
      String instructions,
      List<InputMessage> input,
      TextContainer text,
      Map<String, String> metadata) {

    public record TextContainer(JsonSchema format) {

      public record JsonSchema(String name, Object schema, boolean strict) {

        public String getType() {
          return "json_schema";
        }

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

          logger.error(
              com.box.l10n.mojito.json.ObjectMapper.withIndentedOutput()
                  .writeValueAsStringUnchecked(rootNode));
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

    static String ENDPOINT = "/v1/responses";

    public record InputMessage(String role, List<Content> content) {
      public sealed interface Content permits Text, ImageUrl, ImageFileId {
        String getType();
      }

      public record Text(String text) implements Content {
        public String getType() {
          return "input_text";
        }
      }

      public record ImageUrl(@JsonProperty("image_url") String imageUrl) implements Content {
        public String getType() {
          return "input_image";
        }
      }

      public record ImageFileId(@JsonProperty("file_id") String fileId) implements Content {
        public String getType() {
          return "input_image";
        }
      }
    }

    public static class Builder {
      private String model;
      private String instructions;
      private List<InputMessage> input = new ArrayList<>();
      private TextContainer textContainer;
      private Map<String, String> metadata = new HashMap<>();

      public Builder model(String model) {
        this.model = model;
        return this;
      }

      public Builder instructions(String instruction) {
        this.instructions = instruction;
        return this;
      }

      Builder addInputMessage(String role, List<Content> items) {
        this.input.add(new InputMessage(role, items));
        return this;
      }

      public Builder addUserText(String text) {
        return addInputMessage("user", List.of(new InputMessage.Text(text)));
      }

      public Builder addDeveloperText(String text) {
        return addInputMessage("developer", List.of(new InputMessage.Text(text)));
      }

      public Builder addUserImageFileId(String fileId) {
        this.input.add(new InputMessage("user", List.of(new ImageFileId(fileId))));
        return this;
      }

      public Builder addUserImageUrl(String dataOrRegularUrl) {
        this.input.add(new InputMessage("user", List.of(new ImageUrl(dataOrRegularUrl))));
        return this;
      }

      public Builder addJsonSchema(Class<?> type) {
        this.textContainer =
            new TextContainer(new JsonSchema("output_json_schema", createJsonSchema(type), true));
        return this;
      }

      /**
       * Set of 16 key-value pairs that can be attached to an object
       *
       * @param key maximum length of 64 characters
       * @param value maximum length of 512 characters
       * @return
       */
      public Builder addMetadata(String key, String value) {
        if (this.metadata.size() == 16) {
          throw new IllegalArgumentException("Cannot add more than 16 entries");
        }
        if (key.length() > 64) {
          throw new IllegalArgumentException("key cannot exceed 64 characters");
        }
        if (value.length() > 512) {
          throw new IllegalArgumentException("value cannot exceed 512 characters");
        }
        this.metadata.put(key, value);
        return this;
      }

      public ResponsesRequest build() {
        return new ResponsesRequest(
            model,
            instructions,
            input,
            textContainer,
            this.metadata.isEmpty() ? null : this.metadata);
      }
    }

    public static Builder builder() {
      return new Builder();
    }
  }

  public record ResponsesResponse(
      String id,
      String object,
      @JsonProperty("created_at") Long createdAt,
      String status,
      Error error,
      @JsonProperty("incomplete_details") IncompleteDetails incompleteDetails,
      String model,
      List<Output> output,
      Map<String, String> metadata) {

    public String outputText() {
      return output.stream()
          .flatMap(o -> o.content().stream())
          .filter(c -> "output_text".equals(c.type()))
          .map(Content::text)
          .collect(Collectors.joining());
    }

    public record Error(String code, String message) {}

    public record IncompleteDetails(String reason) {}

    public record Output(
        String id, String type, String status, List<Content> content, String role) {}

    public record Content(String type, String text) {}
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

    return CompletableFuture.supplyAsync(
        () -> {
          HttpPost post = new HttpPost(getUriForEndpoint(ChatCompletionsRequest.ENDPOINT));
          post.setHeader("Authorization", "Bearer " + apiKey);
          post.setEntity(new StringEntity(payload, ContentType.APPLICATION_JSON));
          post.setConfig(
              RequestConfig.custom()
                  .setResponseTimeout(Timeout.ofMilliseconds(httpRequestTimeout.toMillis()))
                  .build());

          try (CloseableHttpResponse response = httpClient.execute(post)) {
            int statusCode = response.getCode();
            String body = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            if (statusCode != 200) {
              throw new OpenAIClientResponseException("ChatCompletion failed", statusCode, body);
            }
            try {
              return objectMapper.readValue(body, ChatCompletionsResponse.class);
            } catch (JsonProcessingException e) {
              throw new OpenAIClientResponseException(
                  "Can't deserialize ChatCompletionsResponse", e, statusCode, body);
            }
          } catch (OpenAIClientResponseException e) {
            throw e;
          } catch (IOException | ParseException e) {
            throw new RuntimeException(e);
          }
        },
        asyncExecutor);
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

    return CompletableFuture.supplyAsync(
        () -> {
          HttpPost post = new HttpPost(getUriForEndpoint(ChatCompletionsRequest.ENDPOINT));
          post.setHeader("Authorization", "Bearer " + apiKey);
          post.setEntity(new StringEntity(requestPayload, ContentType.APPLICATION_JSON));

          CloseableHttpResponse response;
          try {
            response = httpClient.execute(post);
          } catch (IOException e) {
            throw new RuntimeException(e);
          }

          int statusCode = response.getCode();
          if (statusCode != 200) {
            try {
              String body = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
              throw new OpenAIClientResponseException(
                  "ChatCompletion stream failed", statusCode, body);
            } catch (OpenAIClientResponseException e) {
              throw e;
            } catch (IOException | ParseException e) {
              throw new OpenAIClientResponseException(
                  "ChatCompletion stream failed", statusCode, "");
            } finally {
              try {
                response.close();
              } catch (IOException ignored) {
              }
            }
          }

          BufferedReader reader;
          try {
            reader =
                new BufferedReader(
                    new InputStreamReader(
                        response.getEntity().getContent(), StandardCharsets.UTF_8));
          } catch (IOException e) {
            try {
              response.close();
            } catch (IOException ignored) {
            }
            throw new RuntimeException(e);
          }

          return reader
              .lines()
              .takeWhile(s -> !"data: [DONE]".equals(s))
              .filter(Predicate.not(String::isBlank))
              .map(
                  line -> {
                    if (!line.startsWith("data: ")) {
                      throw new OpenAIClientResponseException(
                          "Only support \"data\" lines in stream are supported, got: \"%s\""
                              .formatted(line),
                          statusCode,
                          "");
                    }

                    String jsonPart = line.substring(5);
                    try {
                      return objectMapper.readValue(jsonPart, ChatCompletionsStreamResponse.class);
                    } catch (JsonProcessingException e) {
                      throw new OpenAIClientResponseException(
                          "Can't deserialize ChatCompletionsStreamResponse", e, statusCode, "");
                    }
                  })
              .onClose(
                  () -> {
                    try {
                      response.close();
                    } catch (IOException ignored) {
                    }
                  });
        },
        asyncExecutor);
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

    return CompletableFuture.supplyAsync(
        () -> {
          HttpPost post = new HttpPost(getUriForEndpoint(EmbeddingRequest.ENDPOINT));
          post.setHeader("Authorization", "Bearer " + apiKey);
          post.setEntity(new StringEntity(requestBody, ContentType.APPLICATION_JSON));

          try (CloseableHttpResponse response = httpClient.execute(post)) {
            int statusCode = response.getCode();
            String body = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            if (statusCode != 200) {
              throw new OpenAIClientResponseException("Embedding failed", statusCode, body);
            }
            try {
              return objectMapper.readValue(body, EmbeddingResponse.class);
            } catch (JsonProcessingException e) {
              throw new OpenAIClientResponseException(
                  "Can't deserialize EmbeddingResponse", e, statusCode, body);
            }
          } catch (OpenAIClientResponseException e) {
            throw e;
          } catch (IOException | ParseException e) {
            throw new RuntimeException(e);
          }
        },
        asyncExecutor);
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

    HttpPost post = new HttpPost(getUriForEndpoint(UploadFileRequest.ENDPOINT));
    post.setHeader("Authorization", "Bearer " + apiKey);

    MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create();
    entityBuilder.addTextBody("purpose", uploadFileRequest.purpose());
    switch (uploadFileRequest.fileContent()) {
      case UploadFileRequest.TextContent t ->
          entityBuilder.addBinaryBody(
              "file",
              t.value().getBytes(StandardCharsets.UTF_8),
              ContentType.create(t.contentType()),
              uploadFileRequest.filename());
      case UploadFileRequest.BinaryContent b ->
          entityBuilder.addBinaryBody(
              "file", b.value(), ContentType.create(b.contentType()), uploadFileRequest.filename());
    }
    post.setEntity(entityBuilder.build());

    try (CloseableHttpResponse response = httpClient.execute(post)) {
      int statusCode = response.getCode();
      String body = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
      if (statusCode != 200) {
        throw new OpenAIClientResponseException("Can't upload file", statusCode, body);
      }
      return objectMapper.readValue(body, UploadFileResponse.class);
    } catch (OpenAIClientResponseException e) {
      throw e;
    } catch (IOException | ParseException e) {
      throw new RuntimeException(e);
    }
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

    HttpGet get =
        new HttpGet(
            getUriForEndpoint(
                DownloadFileContentRequest.ENDPOINT.formatted(
                    downloadFileContentRequest.fileId())));
    get.setHeader("Authorization", "Bearer " + apiKey);

    try (CloseableHttpResponse response = httpClient.execute(get)) {
      int statusCode = response.getCode();
      String body = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
      if (statusCode != 200) {
        throw new OpenAIClientResponseException("Can't download file content", statusCode, body);
      }
      return new DownloadFileContentResponse(body);
    } catch (OpenAIClientResponseException e) {
      throw e;
    } catch (IOException | ParseException e) {
      throw new RuntimeException(
          "Error while sending the request to download the file: "
              + downloadFileContentRequest.fileId(),
          e);
    }
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

    HttpPost post = new HttpPost(getUriForEndpoint(CreateBatchRequest.ENDPOINT));
    post.setHeader("Authorization", "Bearer " + apiKey);
    post.setEntity(new StringEntity(jsonBody, ContentType.APPLICATION_JSON));

    try (CloseableHttpResponse response = httpClient.execute(post)) {
      int statusCode = response.getCode();
      String body = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
      if (statusCode != 200) {
        throw new OpenAIClientResponseException("Can't create batch", statusCode, body);
      }
      return objectMapper.readValue(body, CreateBatchResponse.class);
    } catch (OpenAIClientResponseException e) {
      throw e;
    } catch (IOException | ParseException e) {
      throw new RuntimeException(e);
    }
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

    HttpGet get =
        new HttpGet(
            getUriForEndpoint(
                RetrieveBatchRequest.ENDPOINT.formatted(retrieveBatchRequest.batchId())));
    get.setHeader("Authorization", "Bearer " + apiKey);

    try (CloseableHttpResponse response = httpClient.execute(get)) {
      int statusCode = response.getCode();
      String body = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
      if (statusCode != 200) {
        throw new OpenAIClientResponseException("Can't retrieve batch", statusCode, body);
      }
      return objectMapper.readValue(body, RetrieveBatchResponse.class);
    } catch (OpenAIClientResponseException e) {
      throw e;
    } catch (IOException | ParseException e) {
      throw new RuntimeException(e);
    }
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

    public record HttpResponseInfo(int statusCode, String body) {}

    HttpResponseInfo httpResponse;

    public OpenAIClientResponseException(String message, int statusCode, String responseBody) {
      super(message);
      this.httpResponse = new HttpResponseInfo(statusCode, responseBody);
    }

    public OpenAIClientResponseException(
        String message, Exception e, int statusCode, String responseBody) {
      super(message, e);
      this.httpResponse = new HttpResponseInfo(statusCode, responseBody);
    }

    @Override
    public String toString() {
      return "OpenAIHttpClientException{"
          + "message='"
          + getMessage()
          + '\''
          + ", statusCode="
          + httpResponse.statusCode()
          + ", body="
          + httpResponse.body()
          + '}';
    }
  }

  public static class TemperatureHelper {
    public static float getTemperatureForReasoningModels(String model) {
      return model.startsWith("o") || model.startsWith("gpt-5") ? 1 : 0;
    }
  }
}

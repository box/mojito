package com.box.l10n.mojito.cli.command;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.box.l10n.mojito.rest.client.PollableTaskClient;
import com.box.l10n.mojito.rest.client.exception.PollableTaskException;
import com.box.l10n.mojito.rest.resttemplate.AuthenticatedRestTemplate;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Makes authenticated HTTP requests to the Mojito API and prints the response. Similar to {@code gh
 * api} for the GitHub CLI.
 *
 * <p>Handles authentication, instance configuration, pollable task waiting, and pagination
 * transparently, providing a clean JSON interface for agentic orchestration.
 */
@Component
@Scope("prototype")
@Parameters(
    commandNames = {"api"},
    commandDescription = "Make an authenticated API request")
public class ApiCommand extends Command {

  static Logger logger = LoggerFactory.getLogger(ApiCommand.class);

  @Parameter(description = "API endpoint path (e.g. /api/repositories or just repositories)")
  List<String> endpoint;

  @Parameter(
      names = {"-X", "--method"},
      description = "HTTP method (default: GET). Required when --input is used.")
  String method;

  @Parameter(
      names = {"-F", "--field"},
      description =
          "Add a typed parameter in key=value format. "
              + "Values true/false/null and integers are converted to JSON types. "
              + "Use @file to read value from file, @- to read from stdin.")
  List<String> typedFields;

  @Parameter(
      names = {"-f", "--raw-field"},
      description = "Add a string parameter in key=value format (no type conversion)")
  List<String> rawFields;

  @Parameter(
      names = {"--input"},
      description =
          "File to use as request body (use \"-\" for stdin). "
              + "When set, -F/-f fields are added to the query string instead.")
  String inputFile;

  @Parameter(
      names = {"-H", "--header"},
      description = "Add a HTTP request header in key:value format")
  List<String> requestHeaders;

  @Parameter(
      names = {"-w", "--wait"},
      description = "Wait for pollable task completion if response is a PollableTask")
  boolean waitForPollableTask = false;

  @Parameter(
      names = {"--paginate"},
      description = "Fetch all pages for paginated (Spring Data Page) responses")
  boolean paginate = false;

  @Parameter(
      names = {"--slurp"},
      description =
          "Use with --paginate to merge all pages into a single JSON array. "
              + "Without this flag, each page's content is printed as a separate JSON array.")
  boolean slurp = false;

  @Parameter(
      names = {"--page-size"},
      description =
          "Number of items per page when using --paginate (default: 100). "
              + "Sets the 'size' query param.")
  int pageSize = 100;

  @Parameter(
      names = {"--max-pages"},
      description =
          "Maximum number of pages to fetch when using --paginate (default: 10). "
              + "Set to 0 for no limit. "
              + "When the cap is reached, prints the next page number to stderr for resuming.")
  int maxPages = 10;

  @Parameter(
      names = {"--start-page"},
      description =
          "Page number to start from when using --paginate (default: 0). "
              + "Use to resume a previously capped paginated request.")
  int startPage = 0;

  @Parameter(
      names = {"-i", "--include"},
      description = "Include HTTP response status line and headers in the output")
  boolean includeHeaders = false;

  @Parameter(
      names = {"--pretty"},
      description = "Pretty-print JSON output")
  boolean pretty = false;

  @Parameter(
      names = {"--silent"},
      description = "Do not print the response body")
  boolean silent = false;

  @Autowired AuthenticatedRestTemplate authenticatedRestTemplate;

  @Autowired PollableTaskClient pollableTaskClient;

  private final ObjectMapper objectMapper = createObjectMapper();

  @Override
  public boolean shouldShowInCommandList() {
    return true;
  }

  @Override
  protected void execute() throws CommandException {
    validateArgs();

    String path = normalizeEndpoint(endpoint.get(0));
    boolean hasFields = hasFields();
    String resolvedMethod = resolveMethod(hasFields);
    HttpHeaders headers = buildHeaders();

    boolean methodSendsBody = !"GET".equals(resolvedMethod) && !"HEAD".equals(resolvedMethod);

    String body;
    if (inputFile != null) {
      body = readInputFile(inputFile);
      if (hasFields) {
        path = appendFieldsToQueryString(path);
      }
    } else if (hasFields && methodSendsBody) {
      body = buildFieldsBody();
    } else if (hasFields) {
      path = appendFieldsToQueryString(path);
      body = null;
    } else {
      body = null;
    }

    if (body != null && !headers.containsKey(HttpHeaders.CONTENT_TYPE)) {
      headers.setContentType(MediaType.APPLICATION_JSON);
    }

    if (paginate) {
      executePaginated(resolvedMethod, path, headers, body);
    } else {
      ResponseEntity<String> response = executeRequest(resolvedMethod, path, headers, body);
      handleResponse(response);
    }
  }

  void validateArgs() throws CommandException {
    if (endpoint == null || endpoint.isEmpty()) {
      throw new CommandException("An API endpoint path is required");
    }
    if (endpoint.size() > 1) {
      throw new CommandException("Only one endpoint path is allowed");
    }

    if (slurp && !paginate) {
      throw new CommandException("--slurp requires --paginate");
    }

    if (paginate && inputFile != null) {
      throw new CommandException("--paginate is not supported with --input");
    }

    if (paginate && waitForPollableTask) {
      throw new CommandException("--paginate and --wait cannot be used together");
    }

    if (pageSize <= 0) {
      throw new CommandException("--page-size must be a positive integer");
    }

    if (maxPages < 0) {
      throw new CommandException("--max-pages must be 0 (no limit) or a positive integer");
    }

    if (startPage < 0) {
      throw new CommandException("--start-page must be 0 or a positive integer");
    }

    int stdinReaders = countStdinReaders();
    if (stdinReaders > 1) {
      throw new CommandException(
          "stdin (@-) can only be read once. "
              + "Cannot use @- in multiple fields or combine --input - with @- fields.");
    }
  }

  private int countStdinReaders() {
    int count = 0;
    if ("-".equals(inputFile)) {
      count++;
    }
    if (typedFields != null) {
      for (String field : typedFields) {
        if (field.contains("=@-")) {
          count++;
        }
      }
    }
    return count;
  }

  String normalizeEndpoint(String rawEndpoint) {
    if (rawEndpoint.startsWith("http://") || rawEndpoint.startsWith("https://")) {
      return rawEndpoint;
    }
    if (!rawEndpoint.startsWith("/")) {
      rawEndpoint = "/api/" + rawEndpoint;
    }
    return rawEndpoint;
  }

  boolean hasFields() {
    return (typedFields != null && !typedFields.isEmpty())
        || (rawFields != null && !rawFields.isEmpty());
  }

  String resolveMethod(boolean hasFields) throws CommandException {
    if (method != null) {
      return method.toUpperCase();
    }
    if (inputFile != null) {
      throw new CommandException("--input requires an explicit HTTP method via -X (e.g. -X POST)");
    }
    return "GET";
  }

  HttpHeaders buildHeaders() {
    HttpHeaders headers = new HttpHeaders();
    if (requestHeaders != null) {
      for (String header : requestHeaders) {
        int colonIdx = header.indexOf(':');
        if (colonIdx <= 0) {
          throw new CommandException(
              "Invalid header format: \"" + header + "\". Expected key:value");
        }
        String key = header.substring(0, colonIdx).trim();
        String value = header.substring(colonIdx + 1).trim();
        headers.add(key, value);
      }
    }
    return headers;
  }

  String buildFieldsBody() throws CommandException {
    ObjectNode node = objectMapper.createObjectNode();

    if (rawFields != null) {
      for (String field : rawFields) {
        String[] kv = splitField(field);
        node.put(kv[0], kv[1]);
      }
    }

    if (typedFields != null) {
      for (String field : typedFields) {
        String[] kv = splitField(field);
        setTypedValue(node, kv[0], kv[1]);
      }
    }

    try {
      return objectMapper.writeValueAsString(node);
    } catch (JsonProcessingException e) {
      throw new CommandException("Failed to serialize request body: " + e.getMessage(), e);
    }
  }

  String[] splitField(String field) throws CommandException {
    int eqIdx = field.indexOf('=');
    if (eqIdx <= 0) {
      throw new CommandException("Invalid field format: \"" + field + "\". Expected key=value");
    }
    return new String[] {field.substring(0, eqIdx), field.substring(eqIdx + 1)};
  }

  void setTypedValue(ObjectNode node, String key, String value) throws CommandException {
    if ("true".equals(value)) {
      node.put(key, true);
    } else if ("false".equals(value)) {
      node.put(key, false);
    } else if ("null".equals(value)) {
      node.putNull(key);
    } else if (value.startsWith("@")) {
      String fileContent = readFieldFile(value.substring(1));
      node.put(key, fileContent);
    } else {
      try {
        long longValue = Long.parseLong(value);
        node.put(key, longValue);
      } catch (NumberFormatException e) {
        node.put(key, value);
      }
    }
  }

  String readFieldFile(String path) throws CommandException {
    if ("-".equals(path)) {
      return readStdin();
    }
    try {
      return Files.readString(Paths.get(path), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new CommandException("Failed to read file: " + path + ": " + e.getMessage(), e);
    }
  }

  String readInputFile(String path) throws CommandException {
    if ("-".equals(path)) {
      return readStdin();
    }
    try {
      return Files.readString(Paths.get(path), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new CommandException("Failed to read input file: " + path + ": " + e.getMessage(), e);
    }
  }

  String readStdin() throws CommandException {
    try {
      return new String(System.in.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new CommandException("Failed to read from stdin: " + e.getMessage(), e);
    }
  }

  String appendFieldsToQueryString(String path) {
    UriComponentsBuilder builder = UriComponentsBuilder.fromPath(path);
    addFieldsAsQueryParams(builder, rawFields);
    addFieldsAsQueryParams(builder, typedFields);
    return builder.build(false).toUriString();
  }

  private void addFieldsAsQueryParams(UriComponentsBuilder builder, List<String> fields) {
    if (fields != null) {
      for (String field : fields) {
        String[] kv = splitField(field);
        builder.queryParam(kv[0], kv[1]);
      }
    }
  }

  ResponseEntity<String> executeRequest(
      String httpMethod, String path, HttpHeaders headers, String body) throws CommandException {
    HttpMethod springMethod = HttpMethod.valueOf(httpMethod);
    HttpEntity<String> entity =
        body != null ? new HttpEntity<>(body, headers) : new HttpEntity<>(headers);
    String uri = authenticatedRestTemplate.getURIForResource(path);

    try {
      return authenticatedRestTemplate
          .getRestTemplate()
          .exchange(uri, springMethod, entity, String.class);
    } catch (HttpStatusCodeException e) {
      HttpHeaders responseHeaders = e.getResponseHeaders();
      String responseBody = e.getResponseBodyAsString();
      HttpStatusCode statusCode = e.getStatusCode();

      printErrorSummary(responseBody, statusCode.value());

      return ResponseEntity.status(statusCode)
          .headers(responseHeaders != null ? responseHeaders : new HttpHeaders())
          .body(responseBody);
    }
  }

  void handleResponse(ResponseEntity<String> response) throws CommandException {
    int statusCode = response.getStatusCode().value();

    if (includeHeaders) {
      printResponseHeaders(response);
    }

    String responseBody = response.getBody();

    if (waitForPollableTask && responseBody != null) {
      responseBody = maybeWaitForPollableTask(responseBody);
    }

    if (!silent && responseBody != null) {
      printBody(responseBody);
    }

    if (statusCode >= 400) {
      throw new CommandWithExitStatusException(1);
    }
  }

  void executePaginated(String httpMethod, String path, HttpHeaders headers, String body)
      throws CommandException {
    List<JsonNode> allContent = slurp ? new ArrayList<>() : null;
    boolean isFirstPage = true;
    boolean hasNextPage = true;
    int pageNumber = startPage;
    int pagesFetched = 0;

    while (hasNextPage) {
      String pagedPath =
          UriComponentsBuilder.fromUriString(path)
              .replaceQueryParam("page", pageNumber)
              .replaceQueryParam("size", pageSize)
              .build(false)
              .toUriString();

      ResponseEntity<String> response = executeRequest(httpMethod, pagedPath, headers, body);
      int statusCode = response.getStatusCode().value();

      if (statusCode >= 400) {
        if (includeHeaders) {
          printResponseHeaders(response);
        }
        if (!silent && response.getBody() != null) {
          printBody(response.getBody());
        }
        throw new CommandWithExitStatusException(1);
      }

      if (isFirstPage && includeHeaders) {
        printResponseHeaders(response);
      }

      String responseBody = response.getBody();
      if (responseBody == null) {
        break;
      }

      JsonNode root = parseJson(responseBody);
      if (root == null || !root.has("content") || !root.has("hasNext")) {
        if (!silent) {
          printBody(responseBody);
        }
        break;
      }

      JsonNode content = root.get("content");
      if (slurp) {
        if (content.isArray()) {
          for (JsonNode item : content) {
            allContent.add(item);
          }
        }
      } else if (!silent) {
        printJsonNode(content);
      }

      hasNextPage = root.path("hasNext").asBoolean(false);
      pageNumber++;
      pagesFetched++;
      isFirstPage = false;

      if (maxPages > 0 && pagesFetched >= maxPages) {
        if (hasNextPage) {
          System.err.println(
              "mojito: stopped after "
                  + pagesFetched
                  + " page(s), more pages available. "
                  + "Resume with --start-page "
                  + pageNumber);
        }
        break;
      }
    }

    if (slurp && !silent) {
      ArrayNode merged = objectMapper.createArrayNode();
      for (JsonNode item : allContent) {
        merged.add(item);
      }
      printJsonNode(merged);
    }
  }

  String maybeWaitForPollableTask(String responseBody) throws CommandException {
    JsonNode root = parseJson(responseBody);
    if (root == null || !root.isObject()) {
      return responseBody;
    }

    Long taskId = extractPollableTaskId(root);
    if (taskId == null) {
      return responseBody;
    }

    System.err.println("mojito: waiting for pollable task " + taskId + "...");

    try {
      pollableTaskClient.waitForPollableTask(taskId, PollableTaskClient.NO_TIMEOUT, null);
    } catch (PollableTaskException e) {
      System.err.println("mojito: pollable task " + taskId + " failed: " + e.getMessage());
      throw new CommandWithExitStatusException(1);
    }

    System.err.println("mojito: pollable task " + taskId + " completed");

    try {
      com.box.l10n.mojito.rest.entity.PollableTask finalTask =
          pollableTaskClient.getPollableTask(taskId);
      return objectMapper.writeValueAsString(finalTask);
    } catch (JsonProcessingException e) {
      throw new CommandException("Failed to serialize pollable task result: " + e.getMessage(), e);
    }
  }

  /**
   * Extracts the pollable task ID from a response. Checks both top-level PollableTask responses
   * (have "id" + "allFinished") and responses with a nested "pollableTask" field (e.g. SourceAsset,
   * CancelDropConfig, CopyTmConfig, ExportDropConfig).
   */
  Long extractPollableTaskId(JsonNode root) {
    if (looksLikePollableTask(root)) {
      return root.get("id").asLong();
    }

    JsonNode nested = root.get("pollableTask");
    if (nested != null && looksLikePollableTask(nested)) {
      return nested.get("id").asLong();
    }

    return null;
  }

  boolean looksLikePollableTask(JsonNode node) {
    return node != null
        && node.isObject()
        && node.has("id")
        && node.get("id").isNumber()
        && node.has("allFinished");
  }

  void printResponseHeaders(ResponseEntity<String> response) {
    System.out.println("HTTP " + response.getStatusCode().value());
    HttpHeaders headers = response.getHeaders();
    for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
      for (String value : entry.getValue()) {
        System.out.println(entry.getKey() + ": " + value);
      }
    }
    System.out.println();
  }

  void printErrorSummary(String responseBody, int statusCode) {
    String summary = null;
    if (responseBody != null && !responseBody.isBlank()) {
      JsonNode root = parseJson(responseBody);
      if (root != null && root.has("message")) {
        summary = root.get("message").asText();
      }
    }
    if (summary != null) {
      System.err.println("mojito: " + summary + " (HTTP " + statusCode + ")");
    } else {
      System.err.println("mojito: HTTP " + statusCode);
    }
  }

  void printBody(String body) {
    if (body == null || body.isEmpty()) {
      return;
    }
    if (pretty) {
      JsonNode node = parseJson(body);
      if (node != null) {
        printJsonNode(node);
        return;
      }
    }
    System.out.println(body);
  }

  void printJsonNode(JsonNode node) {
    try {
      if (pretty) {
        System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(node));
      } else {
        System.out.println(objectMapper.writeValueAsString(node));
      }
    } catch (JsonProcessingException e) {
      System.out.println(node.toString());
    }
  }

  JsonNode parseJson(String json) {
    try {
      return objectMapper.readTree(json);
    } catch (JsonProcessingException e) {
      return null;
    }
  }

  private static ObjectMapper createObjectMapper() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
    mapper.findAndRegisterModules();
    return mapper;
  }
}

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
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.LongNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
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

  static final int DEFAULT_POLL_MAX_RETRIES = 120;

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
      names = {"--binary"},
      description =
          "Read --input as raw bytes instead of UTF-8 text. "
              + "Use for binary uploads (e.g. images). Sets Content-Type to "
              + "application/octet-stream unless overridden with -H.")
  boolean binaryInput = false;

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
      description =
          "Fetch all pages of results automatically. "
              + "Style is selected with --paginate-style (default: page).")
  boolean paginate = false;

  @Parameter(
      names = {"--paginate-style"},
      description =
          "Pagination style: 'page' (Spring Data page/size with hasNext envelope, default) "
              + "or 'offset' (offset/limit with bare array response, used by text unit search).")
  String paginateStyle = "page";

  @Parameter(
      names = {"--slurp"},
      description =
          "Use with --paginate to merge all pages into a single JSON array. "
              + "Without this flag, each page's content is printed as a separate JSON array.")
  boolean slurp = false;

  @Parameter(
      names = {"--page-size"},
      description =
          "Items per request when using --paginate (default: 100). "
              + "Sets 'size' (page style) or 'limit' (offset style).")
  int pageSize = 100;

  @Parameter(
      names = {"--max-pages"},
      description =
          "Maximum number of requests when using --paginate (default: 10). "
              + "Set to 0 for no limit. "
              + "When the cap is reached, prints resume info to stderr.")
  int maxPages = 10;

  @Parameter(
      names = {"--start-page"},
      description =
          "Starting position when using --paginate (default: 0). "
              + "In page style: page number. In offset style: batch number (offset = N * page-size). "
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

  @Parameter(
      names = {"--spec"},
      description = "Print the OpenAPI spec for the Mojito API and exit")
  boolean printSpec = false;

  @Autowired AuthenticatedRestTemplate authenticatedRestTemplate;

  @Autowired PollableTaskClient pollableTaskClient;

  private final ObjectMapper objectMapper = createObjectMapper();

  @Override
  public boolean shouldShowInCommandList() {
    return true;
  }

  @Override
  protected void execute() throws CommandException {
    if (printSpec) {
      ResponseEntity<String> response = executeRequest("GET", "/api-docs", new HttpHeaders(), null);
      handleResponse(response);
      return;
    }

    validateArgs();

    String path = normalizeEndpoint(endpoint.get(0));
    boolean hasFields = hasFields();
    String resolvedMethod = resolveMethod(hasFields);
    HttpHeaders headers = buildHeaders();
    boolean methodSendsBody = !"GET".equals(resolvedMethod) && !"HEAD".equals(resolvedMethod);

    if (binaryInput && inputFile != null) {
      if (hasFields) {
        path = appendFieldsToQueryString(path);
      }
      if (!headers.containsKey(HttpHeaders.CONTENT_TYPE)) {
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
      }
      byte[] binaryBody = readFileAsBytes(inputFile);
      HttpEntity<byte[]> entity = new HttpEntity<>(binaryBody, headers);
      ResponseEntity<String> response = doExchange(resolvedMethod, path, entity);
      handleResponse(response);
      return;
    }

    String body;
    if (inputFile != null) {
      body = readFileAsString(inputFile);
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

    if (paginate && "offset".equalsIgnoreCase(paginateStyle)) {
      executePaginated(resolvedMethod, path, headers, hasFields, methodSendsBody, true);
    } else if (paginate) {
      executePaginated(resolvedMethod, path, headers, hasFields, methodSendsBody, false);
    } else {
      ResponseEntity<String> response = executeRequest(resolvedMethod, path, headers, body);
      handleResponse(response);
    }
  }

  // --- Argument validation ---

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

    if (paginate
        && !"page".equalsIgnoreCase(paginateStyle)
        && !"offset".equalsIgnoreCase(paginateStyle)) {
      throw new CommandException("--paginate-style must be 'page' or 'offset'");
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

    if (binaryInput && inputFile == null) {
      throw new CommandException("--binary requires --input");
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
    count += countStdinRefsInFields(typedFields);
    count += countStdinRefsInFields(rawFields);
    return count;
  }

  private int countStdinRefsInFields(List<String> fields) {
    if (fields == null) {
      return 0;
    }
    int count = 0;
    for (String field : fields) {
      if (field.contains("=@-")) {
        count++;
      }
    }
    return count;
  }

  // --- Path / method resolution ---

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

  // --- Field construction ---

  String buildFieldsBody() throws CommandException {
    return serializeFieldsToJson(buildFieldsNode());
  }

  ObjectNode buildFieldsNode() throws CommandException {
    ObjectNode root = objectMapper.createObjectNode();
    populateFields(root);
    return root;
  }

  private void populateFields(ObjectNode root) throws CommandException {
    if (rawFields != null) {
      for (String field : rawFields) {
        String[] kv = splitField(field);
        setFieldValue(root, kv[0], kv[1], false);
      }
    }
    if (typedFields != null) {
      for (String field : typedFields) {
        String[] kv = splitField(field);
        setFieldValue(root, kv[0], kv[1], true);
      }
    }
  }

  String serializeFieldsToJson(ObjectNode root) throws CommandException {
    try {
      return objectMapper.writeValueAsString(root);
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

  /**
   * Sets a field value in the JSON tree, supporting nested keys and arrays.
   *
   * <ul>
   *   <li>{@code key=value} sets a top-level field
   *   <li>{@code key[subkey]=value} sets a nested field: {@code {"key": {"subkey": value}}}
   *   <li>{@code key[]=value} appends to an array: {@code {"key": [value]}}
   * </ul>
   */
  void setFieldValue(ObjectNode root, String key, String value, boolean typed)
      throws CommandException {
    if (key.endsWith("[]")) {
      String arrayKey = key.substring(0, key.length() - 2);
      ArrayNode array = getOrCreateArray(root, arrayKey);
      array.add(coerceValue(value, typed));
    } else if (key.contains("[") && key.endsWith("]")) {
      int bracketStart = key.indexOf('[');
      String outerKey = key.substring(0, bracketStart);
      String innerKey = key.substring(bracketStart + 1, key.length() - 1);
      ObjectNode nested = getOrCreateObject(root, outerKey);
      nested.set(innerKey, coerceValue(value, typed));
    } else {
      root.set(key, coerceValue(value, typed));
    }
  }

  /**
   * Converts a string value to the appropriate Jackson JsonNode. For typed fields: true/false/null
   * become JSON types, integers become numbers, @file reads from file. For raw fields: always
   * returns a TextNode.
   */
  JsonNode coerceValue(String value, boolean typed) throws CommandException {
    if (!typed) {
      return TextNode.valueOf(value);
    }
    if ("true".equals(value)) {
      return BooleanNode.TRUE;
    }
    if ("false".equals(value)) {
      return BooleanNode.FALSE;
    }
    if ("null".equals(value)) {
      return NullNode.getInstance();
    }
    if (value.startsWith("@")) {
      return TextNode.valueOf(readFileAsString(value.substring(1)));
    }
    try {
      return LongNode.valueOf(Long.parseLong(value));
    } catch (NumberFormatException e) {
      return TextNode.valueOf(value);
    }
  }

  private ArrayNode getOrCreateArray(ObjectNode parent, String key) {
    JsonNode existing = parent.get(key);
    if (existing != null && existing.isArray()) {
      return (ArrayNode) existing;
    }
    ArrayNode array = objectMapper.createArrayNode();
    parent.set(key, array);
    return array;
  }

  private ObjectNode getOrCreateObject(ObjectNode parent, String key) {
    JsonNode existing = parent.get(key);
    if (existing != null && existing.isObject()) {
      return (ObjectNode) existing;
    }
    ObjectNode obj = objectMapper.createObjectNode();
    parent.set(key, obj);
    return obj;
  }

  // --- File / stdin reading ---

  String readFileAsString(String path) throws CommandException {
    if ("-".equals(path)) {
      return readStdin();
    }
    try {
      return Files.readString(Paths.get(path), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new CommandException("Failed to read file: " + path + ": " + e.getMessage(), e);
    }
  }

  private String readStdin() throws CommandException {
    try {
      return new String(System.in.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new CommandException("Failed to read from stdin: " + e.getMessage(), e);
    }
  }

  byte[] readFileAsBytes(String path) throws CommandException {
    if ("-".equals(path)) {
      try {
        return System.in.readAllBytes();
      } catch (IOException e) {
        throw new CommandException("Failed to read binary data from stdin: " + e.getMessage(), e);
      }
    }
    try {
      return Files.readAllBytes(Paths.get(path));
    } catch (IOException e) {
      throw new CommandException("Failed to read binary file: " + path + ": " + e.getMessage(), e);
    }
  }

  // --- Query string helpers ---

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

  // --- HTTP execution ---

  ResponseEntity<String> executeRequest(
      String httpMethod, String path, HttpHeaders headers, String body) throws CommandException {
    HttpEntity<String> entity =
        body != null ? new HttpEntity<>(body, headers) : new HttpEntity<>(headers);
    return doExchange(httpMethod, path, entity);
  }

  private <T> ResponseEntity<String> doExchange(
      String httpMethod, String path, HttpEntity<T> entity) throws CommandException {
    HttpMethod springMethod = HttpMethod.valueOf(httpMethod);
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

  // --- Response handling ---

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

  private void handlePaginationError(ResponseEntity<String> response) throws CommandException {
    if (includeHeaders) {
      printResponseHeaders(response);
    }
    if (!silent && response.getBody() != null) {
      printBody(response.getBody());
    }
    throw new CommandWithExitStatusException(1);
  }

  // --- Pagination (unified) ---

  /**
   * Unified pagination loop for both page/size and offset/limit styles. The {@code offsetMode} flag
   * selects which style to use.
   */
  void executePaginated(
      String httpMethod,
      String path,
      HttpHeaders headers,
      boolean hasFields,
      boolean methodSendsBody,
      boolean offsetMode)
      throws CommandException {
    List<JsonNode> allContent = slurp ? new ArrayList<>() : null;
    boolean isFirstPage = true;
    int pageNumber = startPage;
    int currentOffset = startPage * pageSize;
    int batchesFetched = 0;

    while (true) {
      ResponseEntity<String> response =
          offsetMode
              ? executeOffsetPageRequest(
                  httpMethod, path, headers, hasFields, methodSendsBody, currentOffset)
              : executePageRequest(httpMethod, path, headers, null, pageNumber);
      int statusCode = response.getStatusCode().value();

      if (statusCode >= 400) {
        handlePaginationError(response);
      }

      if (isFirstPage && includeHeaders) {
        printResponseHeaders(response);
      }

      String responseBody = response.getBody();
      if (responseBody == null) {
        break;
      }

      JsonNode root = parseJson(responseBody);
      PaginationResult pageResult =
          offsetMode
              ? extractOffsetResult(root, responseBody)
              : extractPageResult(root, responseBody);

      if (pageResult == null) {
        break;
      }

      if (slurp) {
        if (pageResult.items.isArray()) {
          for (JsonNode item : pageResult.items) {
            allContent.add(item);
          }
        }
      } else if (!silent) {
        printJsonNode(pageResult.items);
      }

      batchesFetched++;
      isFirstPage = false;

      if (offsetMode) {
        currentOffset += pageResult.items.size();
      } else {
        pageNumber++;
      }

      if (!pageResult.hasMore) {
        break;
      }

      if (maxPages > 0 && batchesFetched >= maxPages) {
        int nextBatch = offsetMode ? startPage + batchesFetched : pageNumber;
        System.err.println(
            "mojito: stopped after "
                + batchesFetched
                + " batch(es), more results may be available. "
                + "Resume with --start-page "
                + nextBatch);
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

  private ResponseEntity<String> executePageRequest(
      String httpMethod, String path, HttpHeaders headers, String body, int pageNumber)
      throws CommandException {
    String pagedPath =
        UriComponentsBuilder.fromUriString(path)
            .replaceQueryParam("page", pageNumber)
            .replaceQueryParam("size", pageSize)
            .build(false)
            .toUriString();
    return executeRequest(httpMethod, pagedPath, headers, body);
  }

  private ResponseEntity<String> executeOffsetPageRequest(
      String httpMethod,
      String path,
      HttpHeaders headers,
      boolean hasFields,
      boolean methodSendsBody,
      int currentOffset)
      throws CommandException {
    if (methodSendsBody && hasFields) {
      ObjectNode bodyNode = buildFieldsNode();
      bodyNode.put("offset", currentOffset);
      bodyNode.put("limit", pageSize);
      String requestBody = serializeFieldsToJson(bodyNode);
      if (!headers.containsKey(HttpHeaders.CONTENT_TYPE)) {
        headers.setContentType(MediaType.APPLICATION_JSON);
      }
      return executeRequest(httpMethod, path, headers, requestBody);
    }

    UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(path);
    if (hasFields) {
      addFieldsAsQueryParams(builder, rawFields);
      addFieldsAsQueryParams(builder, typedFields);
    }
    builder.replaceQueryParam("offset", currentOffset);
    builder.replaceQueryParam("limit", pageSize);
    return executeRequest(httpMethod, builder.build(false).toUriString(), headers, null);
  }

  private record PaginationResult(JsonNode items, boolean hasMore) {}

  private PaginationResult extractPageResult(JsonNode root, String rawBody) {
    if (root == null || !root.has("content") || !root.has("hasNext")) {
      if (!silent) {
        printBody(rawBody);
      }
      return null;
    }
    return new PaginationResult(root.get("content"), root.path("hasNext").asBoolean(false));
  }

  private PaginationResult extractOffsetResult(JsonNode root, String rawBody) {
    if (root == null || !root.isArray()) {
      if (!silent) {
        printBody(rawBody);
      }
      return null;
    }
    return new PaginationResult(root, root.size() >= pageSize);
  }

  // --- Async wait (PollableTask + hybrid search polling) ---

  String maybeWaitForPollableTask(String responseBody) throws CommandException {
    JsonNode root = parseJson(responseBody);
    if (root == null || !root.isObject()) {
      return responseBody;
    }

    String pollingResult = maybeWaitForPollingToken(root);
    if (pollingResult != null) {
      return pollingResult;
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
   * Handles the hybrid search polling pattern. If the response contains a {@code pollingToken} with
   * a {@code requestId}, polls the results endpoint until the search completes or the retry limit
   * is reached.
   *
   * @return the final response body, or null if this is not a polling token response
   */
  String maybeWaitForPollingToken(JsonNode root) throws CommandException {
    JsonNode pollingToken = root.get("pollingToken");
    if (pollingToken == null || !pollingToken.has("requestId")) {
      return null;
    }

    String requestId = pollingToken.get("requestId").asText();
    long pollIntervalMs =
        pollingToken.has("recommendedPollingDurationMillis")
            ? pollingToken.get("recommendedPollingDurationMillis").asLong(1000)
            : 1000;

    System.err.println(
        "mojito: search returned async, polling for results (id: " + requestId + ")...");

    String resultPath = "/api/textunits/search-hybrid/results/" + requestId;
    HttpHeaders headers = new HttpHeaders();

    for (int attempt = 0; attempt < DEFAULT_POLL_MAX_RETRIES; attempt++) {
      try {
        Thread.sleep(pollIntervalMs);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new CommandException("Interrupted while waiting for search results", e);
      }

      ResponseEntity<String> pollResponse = executeRequest("GET", resultPath, headers, null);
      int statusCode = pollResponse.getStatusCode().value();

      if (statusCode >= 400) {
        printErrorSummary(pollResponse.getBody(), statusCode);
        return pollResponse.getBody();
      }

      String pollBody = pollResponse.getBody();
      if (pollBody == null) {
        continue;
      }

      JsonNode pollRoot = parseJson(pollBody);
      if (pollRoot == null) {
        return pollBody;
      }

      if (pollRoot.has("results") && !pollRoot.get("results").isNull()) {
        System.err.println("mojito: search results ready");
        return pollBody;
      }

      if (pollRoot.has("error") && !pollRoot.get("error").isNull()) {
        System.err.println(
            "mojito: search failed: "
                + pollRoot.get("error").path("message").asText("unknown error"));
        return pollBody;
      }
    }

    throw new CommandException(
        "Timed out waiting for search results after "
            + DEFAULT_POLL_MAX_RETRIES
            + " attempts (id: "
            + requestId
            + ")");
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

  // --- Output helpers ---

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

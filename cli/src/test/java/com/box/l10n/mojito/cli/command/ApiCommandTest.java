package com.box.l10n.mojito.cli.command;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.box.l10n.mojito.cli.CLITestBase;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.service.repository.RepositoryRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class ApiCommandTest extends CLITestBase {

  static Logger logger = LoggerFactory.getLogger(ApiCommandTest.class);

  private final ObjectMapper objectMapper = new ObjectMapper();

  private PrintStream originalOut;
  private PrintStream originalErr;
  private ByteArrayOutputStream capturedOut;
  private ByteArrayOutputStream capturedErr;

  @Autowired RepositoryRepository repositoryRepository;

  @Before
  public void captureStreams() {
    originalOut = System.out;
    originalErr = System.err;
    capturedOut = new ByteArrayOutputStream();
    capturedErr = new ByteArrayOutputStream();
    System.setOut(new PrintStream(capturedOut));
    System.setErr(new PrintStream(capturedErr));
  }

  @After
  public void restoreStreams() {
    System.setOut(originalOut);
    System.setErr(originalErr);
  }

  private String getStdout() {
    System.out.flush();
    return capturedOut.toString();
  }

  private String getStderr() {
    System.err.flush();
    return capturedErr.toString();
  }

  /**
   * Extracts JSON from stdout that may contain interleaved server log lines (only happens in tests
   * where the server runs in-process). Finds the first line starting with '{' or '['.
   */
  private String extractJson(String output) {
    for (String line : output.split("\n")) {
      String trimmed = line.trim();
      if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
        return trimmed;
      }
    }
    return output.trim();
  }

  @Test
  public void testGetRepositories() throws Exception {
    createTestRepoUsingRepoService();

    L10nJCommander commander = getL10nJCommander();
    commander.run("api", "/api/repositories");

    String stdout = getStdout();
    assertFalse("Response should not be empty", stdout.isBlank());

    JsonNode json = objectMapper.readTree(stdout.trim());
    assertTrue("Response should be a JSON array", json.isArray());
  }

  @Test
  public void testGetWithBareEndpoint() throws Exception {
    createTestRepoUsingRepoService();

    L10nJCommander commander = getL10nJCommander();
    commander.run("api", "repositories");

    String stdout = getStdout();
    assertFalse("Response should not be empty", stdout.isBlank());

    JsonNode json = objectMapper.readTree(stdout.trim());
    assertTrue(
        "Bare endpoint 'repositories' should be normalized to '/api/repositories'", json.isArray());
  }

  @Test
  public void testGetWithIncludeHeaders() throws Exception {
    L10nJCommander commander = getL10nJCommander();
    commander.run("api", "/api/repositories", "--include");

    String stdout = getStdout();
    assertTrue("Output should contain HTTP status line", stdout.contains("HTTP 200"));
    assertTrue("Output should contain Content-Type header", stdout.contains("Content-Type:"));
  }

  @Test
  public void testGetWithPrettyPrint() throws Exception {
    createTestRepoUsingRepoService();

    L10nJCommander commander = getL10nJCommander();
    commander.run("api", "/api/repositories", "--pretty");

    String stdout = getStdout();
    assertTrue("Pretty-printed output should contain indentation", stdout.contains("  "));
  }

  @Test
  public void testGetWithSilent() throws Exception {
    L10nJCommander commander = getL10nJCommander();
    commander.run("api", "/api/repositories", "--silent");

    String stdout = getStdout();
    assertTrue("Silent mode should produce no stdout output", stdout.isBlank());
  }

  @Test
  public void testPostWithRawFields() throws Exception {
    String repoName = testIdWatcher.getEntityName("api-test-repo");

    L10nJCommander commander = getL10nJCommander();
    commander.run(
        "api", "/api/repositories",
        "-X", "POST",
        "-f", "name=" + repoName,
        "-f", "description=Created via api command");

    String stdout = getStdout();
    assertFalse("Response should not be empty", stdout.isBlank());

    JsonNode json = objectMapper.readTree(extractJson(stdout));
    assertTrue("Response should contain repo id", json.has("id"));

    Repository repo = repositoryRepository.findByName(repoName);
    assertNotNull("Repository should have been created", repo);
    assertEquals("Created via api command", repo.getDescription());
  }

  @Test
  public void testPostWithTypedFields() throws Exception {
    String repoName = testIdWatcher.getEntityName("api-typed-repo");

    L10nJCommander commander = getL10nJCommander();
    commander.run(
        "api", "/api/repositories",
        "-X", "POST",
        "-F", "name=" + repoName,
        "-F", "checkSLA=true");

    String stdout = getStdout();
    assertFalse("Response should not be empty", stdout.isBlank());

    JsonNode json = objectMapper.readTree(extractJson(stdout));
    assertTrue("Response should contain repo id", json.has("id"));

    Repository repo = repositoryRepository.findByName(repoName);
    assertNotNull("Repository should have been created", repo);
    assertTrue("checkSLA should be true (typed field)", repo.getCheckSLA());
  }

  @Test
  public void testExplicitGetWithFields() throws Exception {
    createTestRepoUsingRepoService();

    L10nJCommander commander = getL10nJCommander();
    commander.run(
        "api", "/api/repositories",
        "-X", "GET",
        "-f", "name=" + testIdWatcher.getEntityName("repo"));

    String stdout = getStdout();
    assertFalse("Response should not be empty", stdout.isBlank());

    JsonNode json = objectMapper.readTree(stdout.trim());
    assertTrue("Response should be a JSON array", json.isArray());
  }

  @Test
  public void testFieldsDefaultToGetQueryString() throws Exception {
    createTestRepoUsingRepoService();

    L10nJCommander commander = getL10nJCommander();
    commander.run("api", "/api/repositories", "-f", "name=" + testIdWatcher.getEntityName("repo"));

    String stdout = getStdout();
    assertFalse("Response should not be empty", stdout.isBlank());

    JsonNode json = objectMapper.readTree(stdout.trim());
    assertTrue(
        "Fields without -X should default to GET with query params, not POST", json.isArray());
  }

  @Test
  public void testExplicitPostWithFields() throws Exception {
    String repoName = testIdWatcher.getEntityName("api-explicit-post");

    L10nJCommander commander = getL10nJCommander();
    commander.run("api", "/api/repositories", "-X", "POST", "-f", "name=" + repoName);

    Repository repo = repositoryRepository.findByName(repoName);
    assertNotNull("Explicit -X POST with fields should create the repo", repo);
  }

  @Test
  public void testErrorResponseWrittenToStdout() throws Exception {
    L10nJCommander commander = getL10nJCommander();
    commander.run("api", "/api/repositories/999999");

    String stdout = getStdout();
    String stderr = getStderr();

    assertFalse("Error response body should be on stdout", stdout.isBlank());
    assertFalse("Error summary should be on stderr", stderr.isBlank());
    assertTrue(
        "Stderr should contain HTTP status", stderr.contains("mojito:") && stderr.contains("HTTP"));
  }

  @Test
  public void testPaginatedEndpoint() throws Exception {
    createTestRepoUsingRepoService();

    L10nJCommander commander = getL10nJCommander();
    commander.run("api", "/api/drops", "--paginate", "-f", "size=2");

    String stdout = getStdout();
    assertFalse("Paginated response should have output", stdout.isBlank());
  }

  @Test
  public void testOffsetPaginationWithGet() throws Exception {
    createTestRepoUsingRepoService();

    L10nJCommander commander = getL10nJCommander();
    commander.run(
        "api",
        "/api/textunits",
        "--paginate",
        "--paginate-style",
        "offset",
        "--page-size",
        "5",
        "--max-pages",
        "2",
        "-f",
        "repositoryNames=" + testIdWatcher.getEntityName("repo"));

    String stdout = getStdout();
    assertEquals("Offset pagination on GET should succeed", 0, commander.getExitCode());
  }

  @Test
  public void testPaginateAutoDetectsPageStyle() throws Exception {
    createTestRepoUsingRepoService();

    L10nJCommander commander = getL10nJCommander();
    commander.run("api", "/api/drops", "--paginate", "--page-size", "2", "--max-pages", "1");

    assertEquals("Auto-detected page pagination should succeed", 0, commander.getExitCode());
  }

  @Test
  public void testPaginateAutoDetectsOffsetStyle() throws Exception {
    createTestRepoUsingRepoService();

    L10nJCommander commander = getL10nJCommander();
    commander.run(
        "api",
        "/api/textunits",
        "--paginate",
        "--page-size",
        "5",
        "--max-pages",
        "1",
        "-f",
        "repositoryNames=" + testIdWatcher.getEntityName("repo"));

    assertEquals("Auto-detected offset pagination should succeed", 0, commander.getExitCode());
  }

  @Test
  public void testInvalidPaginateStyle() throws Exception {
    L10nJCommander commander = getL10nJCommander();
    commander.run("api", "/api/repositories", "--paginate", "--paginate-style", "invalid");

    assertTrue(
        "Should error on invalid paginate style",
        getStdout().contains("--paginate-style must be 'auto', 'page', or 'offset'"));
  }

  @Test
  public void testSlurpRequiresPaginate() throws Exception {
    L10nJCommander commander = getL10nJCommander();
    commander.run("api", "/api/repositories", "--slurp");

    assertTrue(
        "Should error when --slurp used without --paginate",
        getStdout().contains("--slurp requires --paginate"));
  }

  @Test
  public void testWaitAndPaginateMutuallyExclusive() throws Exception {
    L10nJCommander commander = getL10nJCommander();
    commander.run("api", "/api/repositories", "--paginate", "--wait");

    assertTrue(
        "Should error when --paginate and --wait combined",
        getStdout().contains("--paginate and --wait cannot be used together"));
  }

  @Test
  public void testInputRequiresExplicitMethod() throws Exception {
    L10nJCommander commander = getL10nJCommander();
    commander.run("api", "/api/repositories", "--input", "-");

    assertTrue(
        "Should error when --input used without -X",
        getStdout().contains("--input requires an explicit HTTP method"));
  }

  @Test
  public void testMissingEndpoint() throws Exception {
    L10nJCommander commander = getL10nJCommander();
    commander.run("api");

    assertTrue(
        "Should error when no endpoint provided",
        getStdout().contains("endpoint path is required"));
  }

  @Test
  public void testNonPollableResponseWithWaitFlag() throws Exception {
    createTestRepoUsingRepoService();

    L10nJCommander commander = getL10nJCommander();
    commander.run("api", "/api/repositories", "--wait");

    String stdout = getStdout();
    assertFalse(
        "Non-pollable response should still be printed when --wait is used", stdout.isBlank());
    assertEquals(
        "Exit code should be 0 for non-pollable response with --wait", 0, commander.getExitCode());
  }

  @Test
  public void testCustomHeader() throws Exception {
    L10nJCommander commander = getL10nJCommander();
    commander.run(
        "api", "/api/repositories",
        "-H", "Accept: application/json");

    String stdout = getStdout();
    assertFalse("Response should not be empty with custom header", stdout.isBlank());
  }

  @Test
  public void testNormalizeEndpointPaths() {
    ApiCommand cmd = new ApiCommand();

    assertEquals(
        "Leading slash path should be kept as-is",
        "/api/repositories",
        cmd.normalizeEndpoint("/api/repositories"));

    assertEquals(
        "Bare name should get /api/ prefix",
        "/api/repositories",
        cmd.normalizeEndpoint("repositories"));

    assertEquals(
        "Bare nested path should get /api/ prefix",
        "/api/drops/export",
        cmd.normalizeEndpoint("drops/export"));

    assertEquals(
        "Full URL should be kept as-is",
        "http://localhost:8080/api/repositories",
        cmd.normalizeEndpoint("http://localhost:8080/api/repositories"));
  }

  @Test
  public void testLooksLikePollableTask() throws Exception {
    ApiCommand cmd = new ApiCommand();

    JsonNode pollable =
        objectMapper.readTree("{\"id\": 123, \"allFinished\": false, \"name\": \"test\"}");
    assertTrue(
        "JSON with id (number) and allFinished should be detected as PollableTask",
        cmd.looksLikePollableTask(pollable));

    JsonNode notPollable = objectMapper.readTree("{\"id\": 123, \"name\": \"test\"}");
    assertFalse(
        "JSON without allFinished should not be detected as PollableTask",
        cmd.looksLikePollableTask(notPollable));

    JsonNode stringId = objectMapper.readTree("{\"id\": \"abc\", \"allFinished\": false}");
    assertFalse(
        "JSON with non-numeric id should not be detected as PollableTask",
        cmd.looksLikePollableTask(stringId));

    JsonNode array = objectMapper.readTree("[{\"id\": 1}]");
    assertFalse(
        "JSON array should not be detected as PollableTask", cmd.looksLikePollableTask(array));
  }

  @Test
  public void testExtractPollableTaskIdFromTopLevel() throws Exception {
    ApiCommand cmd = new ApiCommand();

    JsonNode topLevel =
        objectMapper.readTree("{\"id\": 123, \"allFinished\": false, \"name\": \"test\"}");
    assertEquals(Long.valueOf(123), cmd.extractPollableTaskId(topLevel));
  }

  @Test
  public void testExtractPollableTaskIdFromNested() throws Exception {
    ApiCommand cmd = new ApiCommand();

    JsonNode nested =
        objectMapper.readTree(
            "{\"name\": \"source.xliff\", \"pollableTask\": {\"id\": 456, \"allFinished\": false}}");
    assertEquals(
        "Should detect pollableTask nested inside response objects like SourceAsset",
        Long.valueOf(456),
        cmd.extractPollableTaskId(nested));
  }

  @Test
  public void testExtractPollableTaskIdReturnsNullForNonPollable() throws Exception {
    ApiCommand cmd = new ApiCommand();

    JsonNode plain = objectMapper.readTree("{\"id\": 123, \"name\": \"test\"}");
    assertEquals(
        "Should return null for objects that don't look like PollableTask",
        null,
        cmd.extractPollableTaskId(plain));
  }

  @Test
  public void testBuildFieldsWithMixedTypes() throws Exception {
    ApiCommand cmd = new ApiCommand();
    cmd.rawFields = java.util.List.of("stringKey=hello");
    cmd.typedFields =
        java.util.List.of("boolKey=true", "intKey=42", "nullKey=null", "plainKey=world");

    String body = cmd.buildFieldsBody();
    JsonNode json = objectMapper.readTree(body);

    assertTrue("stringKey should be a string", json.get("stringKey").isTextual());
    assertEquals("hello", json.get("stringKey").asText());

    assertTrue("boolKey should be boolean", json.get("boolKey").isBoolean());
    assertTrue(json.get("boolKey").asBoolean());

    assertTrue("intKey should be a number", json.get("intKey").isNumber());
    assertEquals(42, json.get("intKey").asInt());

    assertTrue("nullKey should be null", json.get("nullKey").isNull());

    assertTrue("plainKey should be a string", json.get("plainKey").isTextual());
    assertEquals("world", json.get("plainKey").asText());
  }

  @Test
  public void testBuildFieldsWithArrays() throws Exception {
    ApiCommand cmd = new ApiCommand();
    cmd.typedFields =
        java.util.List.of("repositoryIds[]=1", "repositoryIds[]=2", "repositoryIds[]=3");

    String body = cmd.buildFieldsBody();
    JsonNode json = objectMapper.readTree(body);

    assertTrue("repositoryIds should be an array", json.get("repositoryIds").isArray());
    assertEquals(3, json.get("repositoryIds").size());
    assertEquals(1, json.get("repositoryIds").get(0).asInt());
    assertEquals(2, json.get("repositoryIds").get(1).asInt());
    assertEquals(3, json.get("repositoryIds").get(2).asInt());
  }

  @Test
  public void testBuildFieldsWithRawArrays() throws Exception {
    ApiCommand cmd = new ApiCommand();
    cmd.rawFields = java.util.List.of("localeTags[]=fr-FR", "localeTags[]=ja-JP");

    String body = cmd.buildFieldsBody();
    JsonNode json = objectMapper.readTree(body);

    assertTrue("localeTags should be an array", json.get("localeTags").isArray());
    assertEquals(2, json.get("localeTags").size());
    assertEquals("fr-FR", json.get("localeTags").get(0).asText());
    assertEquals("ja-JP", json.get("localeTags").get(1).asText());
  }

  @Test
  public void testBuildFieldsMixedArraysAndPlain() throws Exception {
    ApiCommand cmd = new ApiCommand();
    cmd.typedFields =
        java.util.List.of(
            "repositoryIds[]=42",
            "searchType=CONTAINS",
            "localeTags[]=fr-FR",
            "localeTags[]=en-GB",
            "limit=100");

    String body = cmd.buildFieldsBody();
    JsonNode json = objectMapper.readTree(body);

    assertTrue(json.get("repositoryIds").isArray());
    assertEquals(42, json.get("repositoryIds").get(0).asInt());
    assertEquals("CONTAINS", json.get("searchType").asText());
    assertTrue(json.get("localeTags").isArray());
    assertEquals(2, json.get("localeTags").size());
    assertEquals(100, json.get("limit").asInt());
  }

  @Test
  public void testCoerceValueTyped() throws Exception {
    ApiCommand cmd = new ApiCommand();

    assertEquals("true should become BooleanNode", true, cmd.coerceValue("true", true).asBoolean());
    assertEquals(
        "false should become BooleanNode", false, cmd.coerceValue("false", true).asBoolean());
    assertTrue("null should become NullNode", cmd.coerceValue("null", true).isNull());
    assertEquals("integer should become LongNode", 42L, cmd.coerceValue("42", true).asLong());
    assertEquals(
        "non-integer string should become TextNode",
        "hello",
        cmd.coerceValue("hello", true).asText());
  }

  @Test
  public void testCoerceValueRaw() throws Exception {
    ApiCommand cmd = new ApiCommand();

    assertTrue("raw true should stay as text", cmd.coerceValue("true", false).isTextual());
    assertEquals("true", cmd.coerceValue("true", false).asText());

    assertTrue("raw 42 should stay as text", cmd.coerceValue("42", false).isTextual());
    assertEquals("42", cmd.coerceValue("42", false).asText());
  }

  @Test
  public void testTypedFieldFromFile() throws Exception {
    java.io.File tempFile = java.io.File.createTempFile("mojito-api-test-", ".txt");
    tempFile.deleteOnExit();
    java.nio.file.Files.writeString(tempFile.toPath(), "file-content-here");

    ApiCommand cmd = new ApiCommand();
    cmd.typedFields = java.util.List.of("data=@" + tempFile.getAbsolutePath());

    String body = cmd.buildFieldsBody();
    JsonNode json = objectMapper.readTree(body);

    assertEquals(
        "@file should read the file content", "file-content-here", json.get("data").asText());
  }

  @Test
  public void testMaybeWaitForPollingTokenNotPolling() throws Exception {
    ApiCommand cmd = new ApiCommand();

    JsonNode noToken = objectMapper.readTree("{\"results\": [1,2,3]}");
    String result = cmd.maybeWaitForPollingToken(noToken);
    assertNull("Should return null when response has no pollingToken", result);
  }

  @Test
  public void testMaybeWaitForPollingTokenWithError() throws Exception {
    ApiCommand cmd = new ApiCommand();

    JsonNode errorResponse =
        objectMapper.readTree(
            "{\"error\": {\"type\": \"RuntimeException\", \"message\": \"something broke\"}}");
    String result = cmd.maybeWaitForPollingToken(errorResponse);
    assertNull("Should return null when response has error but no pollingToken", result);
  }

  @Test
  public void testPaginatedSlurpMergesContent() throws Exception {
    createTestRepoUsingRepoService();

    L10nJCommander commander = getL10nJCommander();
    commander.run(
        "api", "/api/drops", "--paginate", "--slurp", "--page-size", "1", "--max-pages", "2");

    String stdout = getStdout();
    assertFalse("Slurp output should not be empty", stdout.isBlank());

    JsonNode json = objectMapper.readTree(stdout.trim());
    assertTrue("Slurp should produce a single JSON array", json.isArray());
  }

  @Test
  public void testCountStdinReadersCatchesRawFields() {
    ApiCommand cmd = new ApiCommand();
    cmd.endpoint = java.util.List.of("/api/test");
    cmd.method = "POST";
    cmd.rawFields = java.util.List.of("content=@-");
    cmd.inputFile = "-";

    try {
      cmd.validateArgs();
      org.junit.Assert.fail("Should reject multiple stdin readers");
    } catch (CommandException e) {
      assertTrue("Error should mention stdin", e.getMessage().contains("stdin"));
    }
  }
}

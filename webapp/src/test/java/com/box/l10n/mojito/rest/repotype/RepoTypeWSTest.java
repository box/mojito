package com.box.l10n.mojito.rest.repotype;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.box.l10n.mojito.rest.WSTestBase;
import com.box.l10n.mojito.rest.client.RepoTypeClient;
import com.box.l10n.mojito.rest.entity.IntegrityCheckerType;
import com.box.l10n.mojito.rest.entity.RepoType;
import com.box.l10n.mojito.rest.entity.RepoTypeIntegrityChecker;
import com.box.l10n.mojito.test.TestIdWatcher;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

/**
 * REST contract tests for {@code /api/repo-types} via {@link RepoTypeClient}. Behavior under test
 * is defined in JavaDoc and {@code docs/internal/Architecture.md} (Repo Types).
 */
public class RepoTypeWSTest extends WSTestBase {

  @Rule public TestIdWatcher testIdWatcher = new TestIdWatcher();

  @Autowired RepoTypeClient repoTypeClient;

  @Test
  public void testCreateRepoTypeReturnsCreatedWithIdAndName() {
    RepoType toCreate = new RepoType();
    toCreate.setName(testIdWatcher.getEntityName("React"));

    RepoType created = repoTypeClient.createRepoType(toCreate);

    assertNotNull(created.getId());
    assertEquals(toCreate.getName(), created.getName());
    assertNotNull(created.getCreatedDate());
    assertNotNull(created.getLastModifiedDate());
  }

  @Test
  public void testCreateRepoTypeDefaultsAiPromptToEmptyString() {
    RepoType toCreate = new RepoType();
    toCreate.setName(testIdWatcher.getEntityName("DefaultPrompt"));

    RepoType created = repoTypeClient.createRepoType(toCreate);

    assertEquals("", created.getAiPrompt());
  }

  @Test
  public void testCreateRepoTypeOmittingCheckersCreatesNone() {
    RepoType toCreate = new RepoType();
    toCreate.setName(testIdWatcher.getEntityName("NoCheckers"));

    RepoType created = repoTypeClient.createRepoType(toCreate);

    assertNotNull(created.getIntegrityCheckers());
    assertTrue(created.getIntegrityCheckers().isEmpty());
  }

  @Test
  public void testCreateRepoTypeDuplicateNameReturns409() {
    String name = testIdWatcher.getEntityName("Conflict");
    RepoType first = new RepoType();
    first.setName(name);
    repoTypeClient.createRepoType(first);

    RepoType duplicate = new RepoType();
    duplicate.setName(name);
    try {
      repoTypeClient.createRepoType(duplicate);
      fail("HTTP 409 is expected");
    } catch (HttpClientErrorException e) {
      assertEquals(409, e.getRawStatusCode());
    }
  }

  @Test
  public void testCreateRepoTypeMissingNameReturns400() {
    RepoType toCreate = new RepoType();
    try {
      repoTypeClient.createRepoType(toCreate);
      fail("HTTP 400 is expected");
    } catch (HttpClientErrorException e) {
      assertEquals(400, e.getRawStatusCode());
    }
  }

  @Test
  public void testCreateRepoTypeBlankNameReturns400() {
    RepoType toCreate = new RepoType();
    toCreate.setName("   ");
    try {
      repoTypeClient.createRepoType(toCreate);
      fail("HTTP 400 is expected");
    } catch (HttpClientErrorException e) {
      assertEquals(400, e.getRawStatusCode());
    }
  }

  @Test
  public void testCreateRepoTypeNameTooLongReturns400() {
    RepoType toCreate = new RepoType();
    toCreate.setName("n".repeat(256));
    try {
      repoTypeClient.createRepoType(toCreate);
      fail("HTTP 400 is expected");
    } catch (HttpClientErrorException e) {
      assertEquals(400, e.getRawStatusCode());
    }
  }

  @Test
  public void testGetRepoTypeByIdReturnsType() {
    RepoType toCreate = new RepoType();
    toCreate.setName(testIdWatcher.getEntityName("ById"));
    toCreate.setDescription("desc");
    toCreate.setAiPrompt("stack rules");
    RepoType created = repoTypeClient.createRepoType(toCreate);

    RepoType loaded = repoTypeClient.getRepoTypeById(created.getId());

    assertEquals(created.getId(), loaded.getId());
    assertEquals(created.getName(), loaded.getName());
    assertEquals("desc", loaded.getDescription());
    assertEquals("stack rules", loaded.getAiPrompt());
  }

  @Test
  public void testGetRepoTypeByIdMissingReturns404() {
    try {
      repoTypeClient.getRepoTypeById(987654321L);
      fail("HTTP 404 is expected");
    } catch (HttpClientErrorException e) {
      assertEquals(404, e.getRawStatusCode());
      assertTrue(
          e.getResponseBodyAsString().contains("RepoType with id: 987654321 not found")
              || e.getResponseBodyAsString().contains("987654321"));
    }
  }

  @Test
  public void testGetRepoTypesReturnsAllOrderedByName() {
    String nameApple = testIdWatcher.getEntityName("Apple");
    String nameZebra = testIdWatcher.getEntityName("Zebra");

    RepoType apple = new RepoType();
    apple.setName(nameApple);
    repoTypeClient.createRepoType(apple);

    RepoType zebra = new RepoType();
    zebra.setName(nameZebra);
    repoTypeClient.createRepoType(zebra);

    List<RepoType> all = repoTypeClient.getRepoTypes(null);
    assertNotNull(all);
    List<String> ourNames =
        all.stream()
            .map(RepoType::getName)
            .filter(n -> n.equals(nameApple) || n.equals(nameZebra))
            .collect(Collectors.toList());
    assertEquals(List.of(nameApple, nameZebra), ourNames);
  }

  @Test
  public void testGetRepoTypesByNameExactMatch() {
    String name = testIdWatcher.getEntityName("Exact");
    RepoType toCreate = new RepoType();
    toCreate.setName(name);
    repoTypeClient.createRepoType(toCreate);

    List<RepoType> byName = repoTypeClient.getRepoTypes(name);

    assertEquals(1, byName.size());
    assertEquals(name, byName.get(0).getName());
  }

  @Test
  public void testGetRepoTypesByNameTrimsFilter() {
    String name = testIdWatcher.getEntityName("Exact");
    RepoType toCreate = new RepoType();
    toCreate.setName(name);
    repoTypeClient.createRepoType(toCreate);

    List<RepoType> byName = repoTypeClient.getRepoTypes(name + " ");

    assertEquals(1, byName.size());
    assertEquals(name, byName.get(0).getName());
  }

  @Test
  public void testGetRepoTypesByUnknownNameReturnsEmptyList() {
    String name = testIdWatcher.getEntityName("Exact");
    RepoType toCreate = new RepoType();
    toCreate.setName(name);
    repoTypeClient.createRepoType(toCreate);

    List<RepoType> unknown = repoTypeClient.getRepoTypes(name + "-missing");

    assertNotNull(unknown);
    assertTrue(unknown.isEmpty());
  }

  @Test
  public void testUpdateRepoTypeNullFieldsLeaveValuesUnchanged() {
    RepoType toCreate = new RepoType();
    toCreate.setName(testIdWatcher.getEntityName("Patch"));
    toCreate.setDescription("before");
    toCreate.setAiPrompt("before-prompt");
    RepoType created = repoTypeClient.createRepoType(toCreate);

    RepoType patchDesc = new RepoType();
    patchDesc.setDescription("after");
    RepoType updated = repoTypeClient.updateRepoType(created.getId(), patchDesc);

    assertEquals(created.getName(), updated.getName());
    assertEquals("after", updated.getDescription());
    assertEquals("before-prompt", updated.getAiPrompt());
  }

  @Test
  public void testUpdateRepoTypeEmptyAiPromptClearsPrompt() {
    RepoType toCreate = new RepoType();
    toCreate.setName(testIdWatcher.getEntityName("ClearPrompt"));
    toCreate.setAiPrompt("before-prompt");
    RepoType created = repoTypeClient.createRepoType(toCreate);

    RepoType clearPrompt = new RepoType();
    clearPrompt.setAiPrompt("");
    RepoType updated = repoTypeClient.updateRepoType(created.getId(), clearPrompt);

    assertEquals("", updated.getAiPrompt());
  }

  @Test
  public void testUpdateRepoTypeRenameConflictReturns409() {
    RepoType toCreate = new RepoType();
    toCreate.setName(testIdWatcher.getEntityName("Patch"));
    RepoType created = repoTypeClient.createRepoType(toCreate);

    RepoType other = new RepoType();
    other.setName(testIdWatcher.getEntityName("Other"));
    RepoType otherCreated = repoTypeClient.createRepoType(other);

    RepoType renameConflict = new RepoType();
    renameConflict.setName(otherCreated.getName());
    try {
      repoTypeClient.updateRepoType(created.getId(), renameConflict);
      fail("HTTP 409 is expected");
    } catch (HttpClientErrorException e) {
      assertEquals(409, e.getRawStatusCode());
    }
  }

  @Test
  public void testUpdateRepoTypeMissingReturns404() {
    RepoType patchDesc = new RepoType();
    patchDesc.setDescription("after");
    try {
      repoTypeClient.updateRepoType(987654321L, patchDesc);
      fail("HTTP 404 is expected");
    } catch (HttpClientErrorException e) {
      assertEquals(404, e.getRawStatusCode());
    }
  }

  @Test
  public void testCreateRepoTypePersistsIntegrityCheckers() {
    RepoType toCreate = new RepoType();
    toCreate.setName(testIdWatcher.getEntityName("Checkers"));
    Set<RepoTypeIntegrityChecker> checkers = new HashSet<>();
    checkers.add(clientChecker("properties", IntegrityCheckerType.MESSAGE_FORMAT));
    checkers.add(clientChecker("properties", IntegrityCheckerType.TRAILING_WHITESPACE));
    toCreate.setIntegrityCheckers(checkers);

    RepoType created = repoTypeClient.createRepoType(toCreate);

    assertEquals(2, created.getIntegrityCheckers().size());
    assertClientCheckerPresent(created, "properties", IntegrityCheckerType.MESSAGE_FORMAT);
    assertClientCheckerPresent(created, "properties", IntegrityCheckerType.TRAILING_WHITESPACE);
  }

  @Test
  public void testCreateRepoTypeMissingCheckerTypeReturns400() {
    RepoType toCreate = new RepoType();
    toCreate.setName(testIdWatcher.getEntityName("MissingCheckerType"));
    Set<RepoTypeIntegrityChecker> checkers = new HashSet<>();
    checkers.add(clientChecker("properties", null));
    toCreate.setIntegrityCheckers(checkers);
    try {
      repoTypeClient.createRepoType(toCreate);
      fail("HTTP 400 is expected");
    } catch (HttpClientErrorException e) {
      assertEquals(400, e.getRawStatusCode());
    }
  }

  @Test
  public void testCreateRepoTypeMissingAssetExtensionReturns400() {
    RepoType toCreate = new RepoType();
    toCreate.setName(testIdWatcher.getEntityName("MissingExtension"));
    Set<RepoTypeIntegrityChecker> checkers = new HashSet<>();
    checkers.add(clientChecker(null, IntegrityCheckerType.MESSAGE_FORMAT));
    toCreate.setIntegrityCheckers(checkers);
    try {
      repoTypeClient.createRepoType(toCreate);
      fail("HTTP 400 is expected");
    } catch (HttpClientErrorException e) {
      assertEquals(400, e.getRawStatusCode());
    }
  }

  @Test
  public void testCreateRepoTypeUnknownCheckerTypeReturns400() {
    String name = testIdWatcher.getEntityName("BadChecker");
    String body =
        "{\"name\":\""
            + name
            + "\",\"integrityCheckers\":[{\"assetExtension\":\"properties\","
            + "\"integrityCheckerType\":\"NOT_A_CHECKER\"}]}";
    try {
      postRepoTypeJson(body);
      fail("HTTP 400 is expected");
    } catch (HttpClientErrorException e) {
      assertEquals(400, e.getRawStatusCode());
    }
  }

  @Test
  public void testUpdateRepoTypeUnknownCheckerTypeReturns400() {
    RepoType toCreate = new RepoType();
    toCreate.setName(testIdWatcher.getEntityName("PatchBadChecker"));
    RepoType created = repoTypeClient.createRepoType(toCreate);

    String body =
        "{\"integrityCheckers\":[{\"assetExtension\":\"properties\","
            + "\"integrityCheckerType\":\"NOT_A_CHECKER\"}]}";
    try {
      patchRepoTypeJson(created.getId(), body);
      fail("HTTP 400 is expected");
    } catch (HttpClientErrorException e) {
      assertEquals(400, e.getRawStatusCode());
    }
  }

  @Test
  public void testCreateAndGetRepoTypeWithFluentChecker() {
    RepoType toCreate = new RepoType();
    toCreate.setName(testIdWatcher.getEntityName("Fluent"));
    Set<RepoTypeIntegrityChecker> checkers = new HashSet<>();
    checkers.add(clientChecker("ftl", IntegrityCheckerType.FLUENT));
    toCreate.setIntegrityCheckers(checkers);

    RepoType created = repoTypeClient.createRepoType(toCreate);
    RepoType loaded = repoTypeClient.getRepoTypeById(created.getId());

    assertEquals(1, loaded.getIntegrityCheckers().size());
    assertClientCheckerPresent(loaded, "ftl", IntegrityCheckerType.FLUENT);
  }

  @Test
  public void testUpdateRepoTypeReplacesIntegrityCheckers() {
    RepoType toCreate = new RepoType();
    toCreate.setName(testIdWatcher.getEntityName("ReplaceCheckers"));
    Set<RepoTypeIntegrityChecker> checkers = new HashSet<>();
    checkers.add(clientChecker("properties", IntegrityCheckerType.MESSAGE_FORMAT));
    checkers.add(clientChecker("properties", IntegrityCheckerType.TRAILING_WHITESPACE));
    toCreate.setIntegrityCheckers(checkers);
    RepoType created = repoTypeClient.createRepoType(toCreate);

    RepoType replace = new RepoType();
    Set<RepoTypeIntegrityChecker> onlyTrailing = new HashSet<>();
    onlyTrailing.add(clientChecker("properties", IntegrityCheckerType.TRAILING_WHITESPACE));
    replace.setIntegrityCheckers(onlyTrailing);
    RepoType afterReplace = repoTypeClient.updateRepoType(created.getId(), replace);

    assertEquals(1, afterReplace.getIntegrityCheckers().size());
    assertClientCheckerPresent(
        afterReplace, "properties", IntegrityCheckerType.TRAILING_WHITESPACE);
  }

  @Test
  public void testUpdateRepoTypeEmptyIntegrityCheckersClearsAll() {
    RepoType toCreate = new RepoType();
    toCreate.setName(testIdWatcher.getEntityName("ClearCheckers"));
    Set<RepoTypeIntegrityChecker> checkers = new HashSet<>();
    checkers.add(clientChecker("properties", IntegrityCheckerType.MESSAGE_FORMAT));
    toCreate.setIntegrityCheckers(checkers);
    RepoType created = repoTypeClient.createRepoType(toCreate);

    RepoType clear = new RepoType();
    clear.setIntegrityCheckers(new HashSet<>());
    RepoType cleared = repoTypeClient.updateRepoType(created.getId(), clear);

    assertTrue(cleared.getIntegrityCheckers().isEmpty());
  }

  @Test
  public void testDeleteRepoTypeRemovesType() {
    RepoType toCreate = new RepoType();
    toCreate.setName(testIdWatcher.getEntityName("DeleteMe"));
    RepoType created = repoTypeClient.createRepoType(toCreate);

    ResponseEntity<Void> response =
        authenticatedRestTemplate
            .getRestTemplate()
            .exchange(
                authenticatedRestTemplate.getURIForResource("/api/repo-types/" + created.getId()),
                HttpMethod.DELETE,
                null,
                Void.class);
    assertEquals(204, response.getStatusCodeValue());

    try {
      repoTypeClient.getRepoTypeById(created.getId());
      fail("HTTP 404 is expected after delete");
    } catch (HttpClientErrorException e) {
      assertEquals(404, e.getRawStatusCode());
    }
  }

  @Test
  public void testDeleteRepoTypeMissingReturns404() {
    try {
      repoTypeClient.deleteRepoType(987654321L);
      fail("HTTP 404 is expected");
    } catch (HttpClientErrorException e) {
      assertEquals(404, e.getRawStatusCode());
    }
  }

  private void postRepoTypeJson(String jsonBody) {
    authenticatedRestTemplate
        .getRestTemplate()
        .postForEntity(
            authenticatedRestTemplate.getURIForResource("/api/repo-types"),
            jsonEntity(jsonBody),
            String.class);
  }

  private void patchRepoTypeJson(Long repoTypeId, String jsonBody) {
    authenticatedRestTemplate
        .getRestTemplate()
        .exchange(
            authenticatedRestTemplate.getURIForResource("/api/repo-types/" + repoTypeId),
            HttpMethod.PATCH,
            jsonEntity(jsonBody),
            String.class);
  }

  private static HttpEntity<String> jsonEntity(String jsonBody) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    return new HttpEntity<>(jsonBody, headers);
  }

  private static RepoTypeIntegrityChecker clientChecker(
      String assetExtension, IntegrityCheckerType type) {
    RepoTypeIntegrityChecker checker = new RepoTypeIntegrityChecker();
    checker.setAssetExtension(assetExtension);
    checker.setIntegrityCheckerType(type);
    return checker;
  }

  private static void assertClientCheckerPresent(
      RepoType repoType, String assetExtension, IntegrityCheckerType type) {
    assertNotNull(findClientChecker(repoType, assetExtension, type));
  }

  private static RepoTypeIntegrityChecker findClientChecker(
      RepoType repoType, String assetExtension, IntegrityCheckerType type) {
    for (RepoTypeIntegrityChecker checker : repoType.getIntegrityCheckers()) {
      if (assetExtension.equals(checker.getAssetExtension())
          && type.equals(checker.getIntegrityCheckerType())) {
        return checker;
      }
    }
    fail("Checker not found: " + assetExtension + " / " + type);
    return null;
  }
}

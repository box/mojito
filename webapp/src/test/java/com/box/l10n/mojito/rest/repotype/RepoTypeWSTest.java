package com.box.l10n.mojito.rest.repotype;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.box.l10n.mojito.rest.WSTestBase;
import com.box.l10n.mojito.rest.client.RepoTypeClient;
import com.box.l10n.mojito.rest.client.UserClient;
import com.box.l10n.mojito.rest.client.exception.ResourceNotCreatedException;
import com.box.l10n.mojito.rest.entity.IntegrityCheckerType;
import com.box.l10n.mojito.rest.entity.RepoType;
import com.box.l10n.mojito.rest.entity.RepoTypeIntegrityChecker;
import com.box.l10n.mojito.rest.entity.Role;
import com.box.l10n.mojito.rest.resttemplate.CookieStoreRestTemplate;
import com.box.l10n.mojito.rest.resttemplate.FormLoginConfig;
import com.box.l10n.mojito.test.TestIdWatcher;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;

/**
 * REST contract tests for {@code /api/repo-types} via {@link RepoTypeClient}. Behavior under test
 * is defined in JavaDoc and {@code docs/internal/Architecture.md} (Repo Types).
 */
public class RepoTypeWSTest extends WSTestBase {

  @Rule public TestIdWatcher testIdWatcher = new TestIdWatcher();

  @Autowired RepoTypeClient repoTypeClient;

  @Autowired UserClient userClient;

  @Autowired FormLoginConfig formLoginConfig;

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
      assertEquals("RepoType with name [" + name + "] already exists", e.getResponseBodyAsString());
    }
  }

  @Test
  public void testOnlyNameUniqueConstraintIsMappedTo409() {
    DataIntegrityViolationException nameConstraint =
        new DataIntegrityViolationException(
            "duplicate name",
            new org.hibernate.exception.ConstraintViolationException(
                "duplicate name", new java.sql.SQLException("dup"), "UK__REPO_TYPE__NAME"));
    assertTrue(RepoTypeWS.isRepoTypeNameUniqueConstraint(nameConstraint));

    DataIntegrityViolationException checkerPk =
        new DataIntegrityViolationException(
            "duplicate checker",
            new org.hibernate.exception.ConstraintViolationException(
                "duplicate checker", new java.sql.SQLException("dup"), "PRIMARY"));
    assertFalse(RepoTypeWS.isRepoTypeNameUniqueConstraint(checkerPk));

    try {
      RepoTypeWS.nameConflictOrRethrow(null, checkerPk);
      fail("Non-name integrity failures must not be mapped to 409");
    } catch (DataIntegrityViolationException expected) {
      // checkers-only PATCH with a null request name must not become
      // "RepoType with name [null] already exists"
    }

    try {
      RepoTypeWS.nameConflictOrRethrow(null, nameConstraint);
      fail("A name-constraint race with no persist name must not quote [null]");
    } catch (DataIntegrityViolationException expected) {
    }

    ResponseEntity<String> conflict = RepoTypeWS.nameConflictOrRethrow("React ", nameConstraint);
    assertEquals(409, conflict.getStatusCodeValue());
    assertEquals("RepoType with name [React] already exists", conflict.getBody());
  }

  @Test
  public void testCreateRepoTypeSameLettersDifferentCaseAreDistinct() {
    String name = testIdWatcher.getEntityName("React");
    String otherCase = name.toLowerCase(Locale.ROOT);
    RepoType first = new RepoType();
    first.setName(name);
    repoTypeClient.createRepoType(first);

    RepoType second = new RepoType();
    second.setName(otherCase);
    RepoType createdOtherCase = repoTypeClient.createRepoType(second);

    assertEquals(otherCase, createdOtherCase.getName());
    assertEquals(1, repoTypeClient.getRepoTypes(name).size());
    assertEquals(1, repoTypeClient.getRepoTypes(otherCase).size());
  }

  @Test
  public void testCreateRepoTypePaddedDuplicateNameReturns409WithTrimmedName() {
    String name = testIdWatcher.getEntityName("Conflict");
    RepoType first = new RepoType();
    first.setName(name);
    repoTypeClient.createRepoType(first);

    RepoType duplicate = new RepoType();
    duplicate.setName(name + " ");
    try {
      repoTypeClient.createRepoType(duplicate);
      fail("HTTP 409 is expected");
    } catch (HttpClientErrorException e) {
      assertEquals(409, e.getRawStatusCode());
      assertEquals("RepoType with name [" + name + "] already exists", e.getResponseBodyAsString());
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
      assertEquals("name is required", e.getResponseBodyAsString());
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
      assertEquals("name is required", e.getResponseBodyAsString());
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
      assertEquals("name must be at most 255 characters", e.getResponseBodyAsString());
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
          e.getResponseBodyAsString()
              .contains("\"message\":\"RepoType with id: 987654321 not found\""));
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
      assertEquals(
          "RepoType with name [" + otherCreated.getName() + "] already exists",
          e.getResponseBodyAsString());
    }
  }

  @Test
  public void testUpdateRepoTypeRenamesAndLookupFollowsNewName() {
    RepoType toCreate = new RepoType();
    String original = testIdWatcher.getEntityName("React");
    String renamed = testIdWatcher.getEntityName("React-ICU");
    toCreate.setName(original);
    RepoType created = repoTypeClient.createRepoType(toCreate);

    RepoType patch = new RepoType();
    patch.setName(renamed);
    RepoType updated = repoTypeClient.updateRepoType(created.getId(), patch);

    assertEquals(renamed, updated.getName());
    assertEquals(renamed, repoTypeClient.getRepoTypeById(created.getId()).getName());
    assertTrue(repoTypeClient.getRepoTypes(original).isEmpty());
    List<RepoType> byNewName = repoTypeClient.getRepoTypes(renamed);
    assertEquals(1, byNewName.size());
    assertEquals(created.getId(), byNewName.get(0).getId());
  }

  @Test
  public void testUpdateRepoTypePaddedRenameConflictReturns409WithTrimmedName() {
    RepoType toCreate = new RepoType();
    toCreate.setName(testIdWatcher.getEntityName("Patch"));
    RepoType created = repoTypeClient.createRepoType(toCreate);

    RepoType other = new RepoType();
    other.setName(testIdWatcher.getEntityName("Other"));
    RepoType otherCreated = repoTypeClient.createRepoType(other);

    RepoType renameConflict = new RepoType();
    renameConflict.setName(otherCreated.getName() + " ");
    try {
      repoTypeClient.updateRepoType(created.getId(), renameConflict);
      fail("HTTP 409 is expected");
    } catch (HttpClientErrorException e) {
      assertEquals(409, e.getRawStatusCode());
      assertEquals(
          "RepoType with name [" + otherCreated.getName() + "] already exists",
          e.getResponseBodyAsString());
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
      assertTrue(
          e.getResponseBodyAsString()
              .contains("\"message\":\"RepoType with id: 987654321 not found\""));
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
      assertEquals("integrityCheckerType is required", e.getResponseBodyAsString());
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
      assertEquals("assetExtension is required", e.getResponseBodyAsString());
    }
  }

  @Test
  public void testCreateRepoTypeNullCheckerReturns400() {
    String name = testIdWatcher.getEntityName("NullChecker");
    String body = "{\"name\":\"" + name + "\",\"integrityCheckers\":[null]}";
    try {
      postRepoTypeJson(body);
      fail("HTTP 400 is expected");
    } catch (HttpClientErrorException e) {
      assertEquals(400, e.getRawStatusCode());
      assertEquals("integrity checker must not be null", e.getResponseBodyAsString());
    }
  }

  @Test
  public void testUpdateRepoTypeNullCheckerReturns400() {
    RepoType toCreate = new RepoType();
    toCreate.setName(testIdWatcher.getEntityName("PatchNullChecker"));
    RepoType created = repoTypeClient.createRepoType(toCreate);

    try {
      patchRepoTypeJson(created.getId(), "{\"integrityCheckers\":[null]}");
      fail("HTTP 400 is expected");
    } catch (HttpClientErrorException e) {
      assertEquals(400, e.getRawStatusCode());
      assertEquals("integrity checker must not be null", e.getResponseBodyAsString());
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
      assertTrue(
          e.getResponseBodyAsString()
              .contains("\"message\":\"RepoType with id: " + created.getId() + " not found\""));
    }
  }

  @Test
  public void testDeleteRepoTypeMissingReturns404() {
    try {
      repoTypeClient.deleteRepoType(987654321L);
      fail("HTTP 404 is expected");
    } catch (HttpClientErrorException e) {
      assertEquals(404, e.getRawStatusCode());
      assertTrue(
          e.getResponseBodyAsString()
              .contains("\"message\":\"RepoType with id: 987654321 not found\""));
    }
  }

  @Test
  public void testUserRoleCanGetAndIsForbiddenToMutate() throws ResourceNotCreatedException {
    RepoType toCreate = new RepoType();
    toCreate.setName(testIdWatcher.getEntityName("UserGet"));
    toCreate.setDescription("before");
    RepoType created = repoTypeClient.createRepoType(toCreate);

    // PRINCIPAL_NAME on SPRING_SESSION_V2 is VARCHAR(100); TestIdWatcher names exceed that.
    String username = "u" + Integer.toHexString(testIdWatcher.getEntityName("u").hashCode());
    String password = "test";
    userClient.createUser(username, password, Role.ROLE_USER, "User", "Test", "Test User");
    UserSession user = loginAs(username, password);

    ResponseEntity<String> getResponse =
        user.restTemplate.getForEntity(
            authenticatedRestTemplate.getURIForResource("/api/repo-types/" + created.getId()),
            String.class);
    assertEquals(200, getResponse.getStatusCodeValue());
    assertTrue(getResponse.getBody().contains(created.getName()));

    String attemptedName = testIdWatcher.getEntityName("UserCreate");
    assertUserForbidden(
        user,
        HttpMethod.POST,
        authenticatedRestTemplate.getURIForResource("/api/repo-types"),
        jsonEntityWithCsrf("{\"name\":\"" + attemptedName + "\"}", user.csrfToken));
    assertTrue(repoTypeClient.getRepoTypes(attemptedName).isEmpty());

    assertUserForbidden(
        user,
        HttpMethod.PATCH,
        authenticatedRestTemplate.getURIForResource("/api/repo-types/" + created.getId()),
        jsonEntityWithCsrf("{\"description\":\"after\"}", user.csrfToken));
    assertEquals("before", repoTypeClient.getRepoTypeById(created.getId()).getDescription());

    assertUserForbidden(
        user,
        HttpMethod.DELETE,
        authenticatedRestTemplate.getURIForResource("/api/repo-types/" + created.getId()),
        jsonEntityWithCsrf(null, user.csrfToken));
    assertEquals(created.getName(), repoTypeClient.getRepoTypeById(created.getId()).getName());
  }

  /**
   * Logs in as the given user on a RestTemplate that does not re-authenticate on 403. The shared
   * {@link com.box.l10n.mojito.rest.resttemplate.AuthenticatedRestTemplate} interceptor treats 403
   * as a stale session, so it cannot assert USER authorization failures.
   */
  private UserSession loginAs(String username, String password) {
    CookieStoreRestTemplate userRestTemplate = new CookieStoreRestTemplate();
    String loginUrl =
        authenticatedRestTemplate.getURIForResource(formLoginConfig.getLoginFormPath());
    String loginHtml = userRestTemplate.getForObject(loginUrl, String.class);
    String loginCsrf = csrfTokenFromLoginHtml(loginHtml);

    HttpHeaders loginHeaders = new HttpHeaders();
    loginHeaders.set("X-CSRF-TOKEN", loginCsrf);
    loginHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
    MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
    form.add("username", username);
    form.add("password", password);
    userRestTemplate.postForEntity(loginUrl, new HttpEntity<>(form, loginHeaders), String.class);

    String csrfUrl =
        authenticatedRestTemplate.getURIForResource(formLoginConfig.getCsrfTokenPath());
    ResponseEntity<String> csrfResponse = userRestTemplate.getForEntity(csrfUrl, String.class);
    assertEquals(200, csrfResponse.getStatusCodeValue());
    assertNotNull(csrfResponse.getBody());
    return new UserSession(userRestTemplate, csrfResponse.getBody());
  }

  private static String csrfTokenFromLoginHtml(String loginHtml) {
    Matcher matcher = Pattern.compile("CSRF_TOKEN = '(.*?)';").matcher(loginHtml);
    if (!matcher.find()) {
      fail("Could not find CSRF_TOKEN on the login page");
    }
    return matcher.group(1);
  }

  private static void assertUserForbidden(
      UserSession user, HttpMethod method, String url, HttpEntity<String> entity) {
    try {
      user.restTemplate.exchange(url, method, entity, String.class);
      fail("HTTP 403 is expected");
    } catch (HttpClientErrorException e) {
      assertEquals(403, e.getRawStatusCode());
    }
  }

  private static HttpEntity<String> jsonEntityWithCsrf(String jsonBody, String csrfToken) {
    HttpHeaders headers = new HttpHeaders();
    if (jsonBody != null) {
      headers.setContentType(MediaType.APPLICATION_JSON);
    }
    headers.set("X-CSRF-TOKEN", csrfToken);
    return new HttpEntity<>(jsonBody, headers);
  }

  private static final class UserSession {
    final CookieStoreRestTemplate restTemplate;
    final String csrfToken;

    UserSession(CookieStoreRestTemplate restTemplate, String csrfToken) {
      this.restTemplate = restTemplate;
      this.csrfToken = csrfToken;
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

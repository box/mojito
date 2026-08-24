package com.box.l10n.mojito.rest.repotype;

import com.box.l10n.mojito.entity.RepoType;
import com.box.l10n.mojito.rest.View;
import com.box.l10n.mojito.service.repotype.RepoTypeInvalidException;
import com.box.l10n.mojito.service.repotype.RepoTypeNameAlreadyUsedException;
import com.box.l10n.mojito.service.repotype.RepoTypeService;
import com.fasterxml.jackson.annotation.JsonView;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST API for {@link RepoType} CRUD under {@code /api/repo-types}.
 *
 * <p>Mutating methods inherit the default {@code /api/**} security rules (PM or ADMIN; USER → 403).
 * GET requires an authenticated user (USER included).
 *
 * <p>Request and response bodies use the JPA {@link RepoType} entity directly. Integrity checkers
 * appear as the JSON array {@code integrityCheckers} ({@code assetExtension}, {@code
 * integrityCheckerType}).
 */
@RestController
public class RepoTypeWS {

  static Logger logger = LoggerFactory.getLogger(RepoTypeWS.class);

  @Autowired RepoTypeService repoTypeService;

  /**
   * Returns a single repo type by id.
   *
   * @param repoTypeId path id
   * @return the type including {@code integrityCheckers}
   * @throws RepoTypeWithIdNotFoundException if missing → HTTP 404
   */
  @JsonView(View.RepoType.class)
  @RequestMapping(value = "/api/repo-types/{repoTypeId}", method = RequestMethod.GET)
  public RepoType getRepoTypeById(@PathVariable Long repoTypeId)
      throws RepoTypeWithIdNotFoundException {
    return repoTypeService.getRepoTypeById(repoTypeId);
  }

  /**
   * Lists repo types.
   *
   * <ul>
   *   <li>No {@code name} query param — all types, ordered by name
   *   <li>With {@code name} — exact match; empty list if not found (HTTP 200, not 404)
   * </ul>
   *
   * @param name optional exact name filter
   * @return matching types
   */
  @JsonView(View.RepoType.class)
  @RequestMapping(value = "/api/repo-types", method = RequestMethod.GET)
  public List<RepoType> getRepoTypes(@RequestParam(value = "name", required = false) String name) {
    return repoTypeService.getRepoTypes(name);
  }

  /**
   * Creates a repo type.
   *
   * <ul>
   *   <li>Success → HTTP 201 with the created entity
   *   <li>Missing / blank {@code name}, over-length name/description, a {@code null} checker, or a
   *       checker missing {@code assetExtension} / {@code integrityCheckerType} → HTTP 400 with a
   *       message that names the invalid field
   *   <li>Duplicate name → HTTP 409 {@code RepoType with name [<trimmed name>] already exists}.
   *       Only the name unique constraint ({@code UK__REPO_TYPE__NAME}) is mapped this way; other
   *       integrity failures are not labeled as a name conflict.
   *   <li>Omitted / null {@code aiPrompt} → stored as empty string
   *   <li>Omitted / null {@code integrityCheckers} → no checkers
   * </ul>
   *
   * @param repoType body with at least {@code name}; other fields optional
   * @return 201 + created type, or 409 on name conflict
   */
  @JsonView(View.RepoType.class)
  @RequestMapping(value = "/api/repo-types", method = RequestMethod.POST)
  public ResponseEntity<?> createRepoType(@RequestBody RepoType repoType)
      throws RepoTypeWithIdNotFoundException {
    logger.info("Creating repo type");

    try {
      RepoType created =
          repoTypeService.createRepoType(
              repoType.getName(),
              repoType.getDescription(),
              repoType.getAiPrompt(),
              repoType.getIntegrityCheckers());
      return new ResponseEntity<>(created, HttpStatus.CREATED);
    } catch (RepoTypeInvalidException e) {
      logger.debug("Cannot create the repo type", e);
      return badRequest(e.getMessage());
    } catch (RepoTypeNameAlreadyUsedException e) {
      logger.debug("Cannot create the repo type", e);
      return nameAlreadyUsed(e.getName());
    } catch (DataIntegrityViolationException e) {
      logger.debug("Cannot create the repo type", e);
      return nameConflictOrRethrow(repoType.getName(), e);
    }
  }

  /**
   * Partially updates a repo type (PATCH semantics: {@code null} / omitted JSON fields leave values
   * unchanged).
   *
   * <ul>
   *   <li>Unknown id → HTTP 404
   *   <li>Blank {@code name}, over-length name/description, a {@code null} checker, or a checker
   *       missing {@code assetExtension} / {@code integrityCheckerType} → HTTP 400 with a message
   *       that names the invalid field
   *   <li>Name conflict with another type → HTTP 409 {@code RepoType with name [<trimmed name>]
   *       already exists} (name unique constraint only; a checkers-only PATCH does not quote a null
   *       request name as a name conflict)
   *   <li>Success → HTTP 200 with the updated entity
   *   <li>Omitted or null {@code name}, {@code description}, {@code aiPrompt}, or {@code
   *       integrityCheckers} → leave that field unchanged
   *   <li>Present {@code integrityCheckers} (including {@code []}) → replace the full checker set;
   *       empty array clears all checkers
   * </ul>
   *
   * @param repoTypeId id of the type to update
   * @param repoType fields to change; null/omitted fields are left unchanged
   * @return 200 + updated type, or 409 on name conflict
   * @throws RepoTypeWithIdNotFoundException if missing → HTTP 404
   */
  @JsonView(View.RepoType.class)
  @RequestMapping(value = "/api/repo-types/{repoTypeId}", method = RequestMethod.PATCH)
  public ResponseEntity<?> updateRepoType(
      @PathVariable Long repoTypeId, @RequestBody RepoType repoType)
      throws RepoTypeWithIdNotFoundException {
    logger.info("Updating repo type [{}]", repoTypeId);

    try {
      RepoType updated =
          repoTypeService.updateRepoType(
              repoTypeId,
              repoType.getName(),
              repoType.getDescription(),
              repoType.getAiPrompt(),
              repoType.getIntegrityCheckers());
      return new ResponseEntity<>(updated, HttpStatus.OK);
    } catch (RepoTypeInvalidException e) {
      logger.debug("Cannot update the repo type", e);
      return badRequest(e.getMessage());
    } catch (RepoTypeNameAlreadyUsedException e) {
      logger.debug("Cannot update the repo type", e);
      return nameAlreadyUsed(e.getName());
    } catch (DataIntegrityViolationException e) {
      logger.debug("Cannot update the repo type", e);
      return nameConflictOrRethrow(repoType.getName(), e);
    }
  }

  /**
   * Hard-deletes a repo type and its integrity checkers.
   *
   * <p>HTTP 204 on success (void return). Unknown id → HTTP 404.
   *
   * @param repoTypeId id of the type to delete
   * @throws RepoTypeWithIdNotFoundException if missing → HTTP 404
   */
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @RequestMapping(value = "/api/repo-types/{repoTypeId}", method = RequestMethod.DELETE)
  public void deleteRepoType(@PathVariable Long repoTypeId) throws RepoTypeWithIdNotFoundException {
    logger.info("Deleting repo type [{}]", repoTypeId);
    repoTypeService.deleteRepoType(repoTypeId);
  }

  /**
   * HTTP 409 body quotes the trimmed name so it matches uniqueness (the service strips surrounding
   * whitespace before persist).
   */
  private static ResponseEntity<String> nameAlreadyUsed(String name) {
    String conflictName = name == null ? null : name.trim();
    return new ResponseEntity<>(
        "RepoType with name [" + conflictName + "] already exists", HttpStatus.CONFLICT);
  }

  private static ResponseEntity<String> badRequest(String message) {
    return new ResponseEntity<>(message, HttpStatus.BAD_REQUEST);
  }

  /**
   * Maps a unique-name race ({@code UK__REPO_TYPE__NAME}) to HTTP 409. Any other integrity failure
   * (e.g. the checker table composite primary key) is rethrown so it is not labeled as a name
   * conflict. A checkers-only PATCH has a null request name; that must not produce {@code name
   * [null] already exists}.
   */
  static ResponseEntity<String> nameConflictOrRethrow(
      String name, DataIntegrityViolationException e) {
    if (!isRepoTypeNameUniqueConstraint(e)) {
      throw e;
    }
    if (name == null || name.trim().isEmpty()) {
      throw e;
    }
    return nameAlreadyUsed(name);
  }

  static boolean isRepoTypeNameUniqueConstraint(DataIntegrityViolationException e) {
    for (Throwable t = e; t != null; t = t.getCause()) {
      if (t instanceof org.hibernate.exception.ConstraintViolationException cve
          && cve.getConstraintName() != null
          && cve.getConstraintName().contains("UK__REPO_TYPE__NAME")) {
        return true;
      }
      if (t.getMessage() != null && t.getMessage().contains("UK__REPO_TYPE__NAME")) {
        return true;
      }
    }
    return false;
  }
}

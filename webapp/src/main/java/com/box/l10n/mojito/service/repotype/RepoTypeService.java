package com.box.l10n.mojito.service.repotype;

import com.box.l10n.mojito.entity.RepoType;
import com.box.l10n.mojito.entity.RepoTypeIntegrityChecker;
import com.box.l10n.mojito.rest.repotype.RepoTypeWithIdNotFoundException;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business logic for creating, reading, updating, and deleting {@link RepoType} records and their
 * integrity checkers.
 *
 * <p>Assigning a type to a {@link com.box.l10n.mojito.entity.Repository} is out of scope here.
 * Until that link exists, delete is a hard delete of the type and its checkers.
 */
@Service
public class RepoTypeService {

  static Logger logger = LoggerFactory.getLogger(RepoTypeService.class);

  @Autowired RepoTypeRepository repoTypeRepository;

  /**
   * Creates a new repo type and optionally its integrity checkers.
   *
   * <ul>
   *   <li>{@code name} is required (non-blank after trim) and at most {@link
   *       RepoType#NAME_MAX_LENGTH} characters; otherwise {@link RepoTypeInvalidException}. Leading
   *       and trailing whitespace is stripped before uniqueness checks and persist. There is no
   *       other grammar (open string, not a Java enum; same idea as repository names).
   *   <li>{@code name} must be unique <em>case-sensitively</em>; otherwise {@link
   *       RepoTypeNameAlreadyUsedException} ({@code React} and {@code react} can both exist)
   *   <li>{@code description} may be {@code null} or empty, and at most {@link
   *       RepoType#DESCRIPTION_MAX_LENGTH} characters
   *   <li>{@code aiPrompt} {@code null} is treated as empty string. There is no application max
   *       length (stored as unbounded long text).
   *   <li>{@code integrityCheckers} {@code null} or empty means no checkers; otherwise each checker
   *       must be non-null and have a non-blank {@code assetExtension} and a non-null {@code
   *       integrityCheckerType} ({@link RepoTypeInvalidException}). Extensions are trimmed and a
   *       leading {@code .} is stripped before persist.
   * </ul>
   *
   * @param name unique name
   * @param description optional description
   * @param aiPrompt shared type-layer prompt (translation and review)
   * @param integrityCheckers checkers to attach, or {@code null}/empty for none
   * @return the persisted type including generated id and checkers
   * @throws RepoTypeInvalidException if {@code name} is blank, {@code name} or {@code description}
   *     exceeds its max length, a checker is {@code null}, or a checker is missing {@code
   *     assetExtension} / {@code integrityCheckerType}
   * @throws RepoTypeNameAlreadyUsedException if {@code name} is already taken
   * @throws RepoTypeWithIdNotFoundException if the row cannot be reloaded after persist
   */
  @Transactional
  public RepoType createRepoType(
      String name,
      String description,
      String aiPrompt,
      Set<RepoTypeIntegrityChecker> integrityCheckers)
      throws RepoTypeNameAlreadyUsedException, RepoTypeWithIdNotFoundException {

    validateName(name = trimName(name));
    validateDescription(description);

    logger.debug("Check no repo type with name: {} exists", name);

    if (repoTypeRepository.findByName(name) != null) {
      throw new RepoTypeNameAlreadyUsedException(name);
    }

    logger.debug("Create a RepoType with name: {}", name);

    RepoType repoType = new RepoType();
    repoType.setName(name);
    repoType.setDescription(description);
    repoType.setAiPrompt(aiPrompt != null ? aiPrompt : "");
    repoType.setIntegrityCheckers(new HashSet<>());
    // Flush now so a unique-name race fails here (DataIntegrityViolationException). The WS
    // maps that to 409 after this @Transactional method has rolled back — do not catch it
    // inside the transaction (session is then unusable; a checked wrap can attempt commit).
    repoType = repoTypeRepository.saveAndFlush(repoType);

    if (integrityCheckers != null && !integrityCheckers.isEmpty()) {
      updateIntegrityCheckers(repoType, integrityCheckers);
    }

    logger.debug("Created repo type id: {} (name: {})", repoType.getId(), name);
    return getRepoTypeById(repoType.getId());
  }

  /**
   * Loads a repo type by id, including its integrity checkers.
   *
   * @param repoTypeId id of the type
   * @return the type
   * @throws RepoTypeWithIdNotFoundException if no type exists with that id
   */
  @Transactional
  public RepoType getRepoTypeById(Long repoTypeId) throws RepoTypeWithIdNotFoundException {
    return repoTypeRepository
        .findById(repoTypeId)
        .orElseThrow(() -> new RepoTypeWithIdNotFoundException(repoTypeId));
  }

  /**
   * Lists repo types, optionally filtered by exact name.
   *
   * <ul>
   *   <li>If {@code name} is {@code null} or blank, returns all types ordered by name
   *   <li>If {@code name} is set, trims it and returns a list with the single matching type, or an
   *       empty list if none match (does not throw)
   * </ul>
   *
   * @param name optional exact name filter
   * @return matching types; never {@code null}
   */
  @Transactional
  public List<RepoType> getRepoTypes(String name) {
    if (StringUtils.isBlank(name)) {
      return repoTypeRepository.findAllByOrderByNameAsc();
    }

    RepoType repoType = repoTypeRepository.findByName(name.trim());
    if (repoType == null) {
      return Collections.emptyList();
    }
    return Collections.singletonList(repoType);
  }

  /**
   * Updates an existing repo type (PATCH semantics: {@code null} means leave unchanged).
   *
   * <ul>
   *   <li>Unknown {@code repoTypeId} → {@link RepoTypeWithIdNotFoundException}
   *   <li>Non-null {@code name} is trimmed, then rejected if blank or longer than {@link
   *       RepoType#NAME_MAX_LENGTH} → {@link RepoTypeInvalidException}
   *   <li>Non-null {@code description} longer than {@link RepoType#DESCRIPTION_MAX_LENGTH} → {@link
   *       RepoTypeInvalidException}
   *   <li>Renaming to a name used by a <em>different</em> type → {@link
   *       RepoTypeNameAlreadyUsedException}; renaming to the same name is a no-op for uniqueness
   *   <li>{@code name}, {@code description}, {@code aiPrompt}: {@code null} leaves the field
   *       unchanged; a non-null value (including empty string for description/prompt) replaces it
   *   <li>{@code integrityCheckers}: {@code null} leaves the existing checkers unchanged; a
   *       non-null set (including empty) replaces the full set via {@link #updateIntegrityCheckers}
   *       — empty clears all checkers. Present checkers are validated/normalized the same way as
   *       create. Replacing checkers also updates {@code lastModifiedDate} on the type, even when
   *       name/description/prompt are omitted.
   * </ul>
   *
   * @param repoTypeId id of the type to update
   * @param name new name, or {@code null} to leave unchanged
   * @param description new description, or {@code null} to leave unchanged
   * @param aiPrompt new prompt, or {@code null} to leave unchanged
   * @param integrityCheckers new checker set, or {@code null} to leave unchanged
   * @return the updated type
   * @throws RepoTypeWithIdNotFoundException if the id does not exist
   * @throws RepoTypeInvalidException if a provided {@code name} is blank, {@code name} or {@code
   *     description} is too long, a checker is {@code null}, or a checker is missing required
   *     fields. {@code aiPrompt} has no application max length.
   * @throws RepoTypeNameAlreadyUsedException if the new name conflicts with another type
   */
  @Transactional
  public RepoType updateRepoType(
      Long repoTypeId,
      String name,
      String description,
      String aiPrompt,
      Set<RepoTypeIntegrityChecker> integrityCheckers)
      throws RepoTypeWithIdNotFoundException, RepoTypeNameAlreadyUsedException {

    RepoType repoType = getRepoTypeById(repoTypeId);

    if (name != null) {
      validateName(name = trimName(name));
      RepoType existing = repoTypeRepository.findByName(name);
      if (existing != null && !repoType.getId().equals(existing.getId())) {
        throw new RepoTypeNameAlreadyUsedException(name);
      }
      repoType.setName(name);
    }
    if (description != null) {
      validateDescription(description);
      repoType.setDescription(description);
    }
    if (aiPrompt != null) {
      repoType.setAiPrompt(aiPrompt);
    }

    if (name != null || description != null || aiPrompt != null) {
      repoType = repoTypeRepository.saveAndFlush(repoType);
    }

    if (integrityCheckers != null) {
      updateIntegrityCheckers(repoType, integrityCheckers);
    }

    return getRepoTypeById(repoType.getId());
  }

  /**
   * Hard-deletes a repo type and all of its integrity checkers.
   *
   * <p>Until repositories can reference a type, there is no “type in use” guard. After that link
   * exists, delete should refuse (or require clearing assignments) when any repository still
   * references the type.
   *
   * @param repoTypeId id of the type to delete
   * @throws RepoTypeWithIdNotFoundException if the id does not exist
   */
  @Transactional
  public void deleteRepoType(Long repoTypeId) throws RepoTypeWithIdNotFoundException {
    RepoType repoType = getRepoTypeById(repoTypeId);
    logger.debug("Delete repo type with name: {}", repoType.getName());
    repoTypeRepository.delete(repoType);
  }

  /**
   * Replaces the integrity checker set on a repo type.
   *
   * <p>Checkers are an element collection on {@link RepoType}. Identity is {@code (assetExtension,
   * integrityCheckerType)} only; there is no checker id or parent on the value type.
   *
   * <ul>
   *   <li>A non-null incoming set fully replaces the collection (Hibernate inserts/deletes join
   *       table rows as needed)
   *   <li>Each incoming checker must be non-null and have a non-blank {@code assetExtension} and a
   *       non-null {@code integrityCheckerType}; otherwise {@link RepoTypeInvalidException}
   *   <li>{@code assetExtension} is trimmed and a single leading {@code .} is stripped so {@code
   *       json}, {@code json }, and {@code .json} are the same pair
   *   <li>Duplicate incoming pairs collapse to one row (last occurrence wins)
   *   <li>{@code null} or empty incoming set removes all checkers for the type
   * </ul>
   *
   * <p>If no row exists for {@code repoType.getId()}, throws {@link
   * RepoTypeWithIdNotFoundException}. Does not save the caller's instance when the id is gone —
   * that would re-insert a deleted type.
   *
   * @param repoType type whose checkers are being replaced; must have a persisted id
   * @param integrityCheckers desired full set after the update
   * @throws RepoTypeWithIdNotFoundException if the type id does not exist
   */
  @Transactional
  public void updateIntegrityCheckers(
      RepoType repoType, Set<RepoTypeIntegrityChecker> integrityCheckers)
      throws RepoTypeWithIdNotFoundException {

    RepoType managed = getRepoTypeById(repoType.getId());

    Set<RepoTypeIntegrityChecker> replacement = new HashSet<>();
    if (integrityCheckers != null && !integrityCheckers.isEmpty()) {
      replacement.addAll(uniqueByExtensionAndType(integrityCheckers));
    }

    logger.debug(
        "Replacing integrity checkers for repo type id: {} (count: {})",
        repoType.getId(),
        replacement.size());

    Set<RepoTypeIntegrityChecker> current = managed.getIntegrityCheckers();
    if (current == null) {
      managed.setIntegrityCheckers(replacement);
    } else {
      current.clear();
      current.addAll(replacement);
    }
    // Element-collection changes do not dirty the parent row, so @LastModifiedDate would not
    // move. Stamp it so a checkers-only PATCH still updates repo_type.last_modified_date.
    managed.setLastModifiedDate(ZonedDateTime.now());
    repoTypeRepository.save(managed);
  }

  /**
   * Collapses checkers that share {@code (assetExtension, integrityCheckerType)} to a single
   * instance. Last occurrence wins so a later duplicate in the request body is what we persist.
   * Validates and normalizes each checker first so the de-dupe key matches what is stored.
   */
  private Set<RepoTypeIntegrityChecker> uniqueByExtensionAndType(
      Set<RepoTypeIntegrityChecker> integrityCheckers) {
    Map<String, RepoTypeIntegrityChecker> unique = new LinkedHashMap<>();
    for (RepoTypeIntegrityChecker checker : integrityCheckers) {
      normalizeChecker(checker);
      String key = checker.getAssetExtension() + ":" + checker.getIntegrityCheckerType().name();
      unique.put(key, checker);
    }
    return new LinkedHashSet<>(unique.values());
  }

  /**
   * Requires a non-null checker with both fields set, and normalizes {@code assetExtension} (trim,
   * strip one leading {@code .}) so lookup and persist use the same pair.
   */
  private void normalizeChecker(RepoTypeIntegrityChecker checker) {
    if (checker == null) {
      throw new RepoTypeInvalidException("integrity checker must not be null");
    }
    if (checker.getIntegrityCheckerType() == null) {
      throw new RepoTypeInvalidException("integrityCheckerType is required");
    }
    String extension = checker.getAssetExtension();
    if (StringUtils.isBlank(extension)) {
      throw new RepoTypeInvalidException("assetExtension is required");
    }
    extension = extension.trim();
    if (extension.startsWith(".")) {
      extension = extension.substring(1).trim();
    }
    if (StringUtils.isBlank(extension)) {
      throw new RepoTypeInvalidException("assetExtension is required");
    }
    checker.setAssetExtension(extension);
  }

  private static String trimName(String name) {
    return name == null ? null : name.trim();
  }

  private void validateName(String name) {
    if (StringUtils.isBlank(name)) {
      throw new RepoTypeInvalidException("name is required");
    }
    if (name.length() > RepoType.NAME_MAX_LENGTH) {
      throw new RepoTypeInvalidException(
          "name must be at most " + RepoType.NAME_MAX_LENGTH + " characters");
    }
  }

  private void validateDescription(String description) {
    if (description != null && description.length() > RepoType.DESCRIPTION_MAX_LENGTH) {
      throw new RepoTypeInvalidException(
          "description must be at most " + RepoType.DESCRIPTION_MAX_LENGTH + " characters");
    }
  }
}

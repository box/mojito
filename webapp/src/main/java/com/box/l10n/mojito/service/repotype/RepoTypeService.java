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
   *   <li>{@code name} is required (non-blank) and at most {@link RepoType#NAME_MAX_LENGTH}
   *       characters; otherwise {@link RepoTypeInvalidException}
   *   <li>{@code name} must be unique; otherwise {@link RepoTypeNameAlreadyUsedException}
   *   <li>{@code description} may be {@code null} or empty, and at most {@link
   *       RepoType#DESCRIPTION_MAX_LENGTH} characters
   *   <li>{@code aiPrompt} {@code null} is treated as empty string
   *   <li>{@code integrityCheckers} {@code null} or empty means no checkers; otherwise each row is
   *       associated with the new type and saved
   * </ul>
   *
   * @param name unique name
   * @param description optional description
   * @param aiPrompt shared type-layer prompt (translation and review)
   * @param integrityCheckers checkers to attach, or {@code null}/empty for none
   * @return the persisted type including generated id and checkers
   * @throws RepoTypeInvalidException if {@code name} is blank or a field exceeds its max length
   * @throws RepoTypeNameAlreadyUsedException if {@code name} is already taken
   */
  @Transactional
  public RepoType createRepoType(
      String name,
      String description,
      String aiPrompt,
      Set<RepoTypeIntegrityChecker> integrityCheckers)
      throws RepoTypeNameAlreadyUsedException {

    validateName(name);
    validateDescription(description);

    logger.debug("Check no repo type with name: {} exists", name);

    if (repoTypeRepository.findByName(name) != null) {
      throw new RepoTypeNameAlreadyUsedException(name + " is used by another repo type");
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
    return repoTypeRepository.findById(repoType.getId()).orElse(repoType);
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
    RepoType repoType = repoTypeRepository.findById(repoTypeId).orElse(null);
    if (repoType == null) {
      throw new RepoTypeWithIdNotFoundException(repoTypeId);
    }
    return repoType;
  }

  /**
   * Lists repo types, optionally filtered by exact name.
   *
   * <ul>
   *   <li>If {@code name} is {@code null} or blank, returns all types ordered by name
   *   <li>If {@code name} is set, returns a list with the single matching type, or an empty list if
   *       none match (does not throw)
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

    RepoType repoType = repoTypeRepository.findByName(name);
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
   *   <li>Non-null {@code name} that is blank or longer than {@link RepoType#NAME_MAX_LENGTH} →
   *       {@link RepoTypeInvalidException}
   *   <li>Non-null {@code description} longer than {@link RepoType#DESCRIPTION_MAX_LENGTH} → {@link
   *       RepoTypeInvalidException}
   *   <li>Renaming to a name used by a <em>different</em> type → {@link
   *       RepoTypeNameAlreadyUsedException}; renaming to the same name is a no-op for uniqueness
   *   <li>{@code name}, {@code description}, {@code aiPrompt}: {@code null} leaves the field
   *       unchanged; a non-null value (including empty string for description/prompt) replaces it
   *   <li>{@code integrityCheckers}: {@code null} leaves the existing checkers unchanged; a
   *       non-null set (including empty) replaces the full set via {@link #updateIntegrityCheckers}
   *       — empty clears all checkers. Replacing checkers also updates {@code lastModifiedDate} on
   *       the type, even when name/description/prompt are omitted.
   * </ul>
   *
   * @param repoTypeId id of the type to update
   * @param name new name, or {@code null} to leave unchanged
   * @param description new description, or {@code null} to leave unchanged
   * @param aiPrompt new prompt, or {@code null} to leave unchanged
   * @param integrityCheckers new checker set, or {@code null} to leave unchanged
   * @return the updated type
   * @throws RepoTypeWithIdNotFoundException if the id does not exist
   * @throws RepoTypeInvalidException if a provided {@code name} is blank or a field is too long
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
      validateName(name);
      RepoType existing = repoTypeRepository.findByName(name);
      if (existing != null && !repoType.getId().equals(existing.getId())) {
        throw new RepoTypeNameAlreadyUsedException(name + " is used by another repo type");
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

    return repoTypeRepository.findById(repoType.getId()).orElse(repoType);
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
   *   <li>Duplicate incoming pairs collapse to one row (last occurrence wins)
   *   <li>{@code null} or empty incoming set removes all checkers for the type
   * </ul>
   *
   * <p>Does not validate that {@code repoType} is managed; caller must pass a persisted entity.
   *
   * @param repoType type whose checkers are being replaced
   * @param integrityCheckers desired full set after the update
   */
  @Transactional
  public void updateIntegrityCheckers(
      RepoType repoType, Set<RepoTypeIntegrityChecker> integrityCheckers) {

    Set<RepoTypeIntegrityChecker> replacement = new HashSet<>();
    if (integrityCheckers != null && !integrityCheckers.isEmpty()) {
      replacement.addAll(uniqueByExtensionAndType(integrityCheckers));
    }

    logger.debug(
        "Replacing integrity checkers for repo type id: {} (count: {})",
        repoType.getId(),
        replacement.size());

    RepoType managed = repoTypeRepository.findById(repoType.getId()).orElse(repoType);
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
   */
  private Set<RepoTypeIntegrityChecker> uniqueByExtensionAndType(
      Set<RepoTypeIntegrityChecker> integrityCheckers) {
    Map<String, RepoTypeIntegrityChecker> unique = new LinkedHashMap<>();
    for (RepoTypeIntegrityChecker checker : integrityCheckers) {
      String key = checker.getAssetExtension() + ":" + checker.getIntegrityCheckerType().name();
      unique.put(key, checker);
    }
    return new LinkedHashSet<>(unique.values());
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

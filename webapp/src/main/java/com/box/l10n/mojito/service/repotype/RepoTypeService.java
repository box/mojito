package com.box.l10n.mojito.service.repotype;

import com.box.l10n.mojito.entity.RepoType;
import com.box.l10n.mojito.entity.RepoTypeIntegrityChecker;
import com.box.l10n.mojito.rest.repotype.RepoTypeWithIdNotFoundException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
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

  @Autowired RepoTypeIntegrityCheckerRepository repoTypeIntegrityCheckerRepository;

  /**
   * Creates a new repo type and optionally its integrity checkers.
   *
   * <ul>
   *   <li>{@code name} must be unique; otherwise {@link RepoTypeNameAlreadyUsedException}
   *   <li>{@code description} may be {@code null} or empty
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
   * @throws RepoTypeNameAlreadyUsedException if {@code name} is already taken
   */
  @Transactional
  public RepoType createRepoType(
      String name,
      String description,
      String aiPrompt,
      Set<RepoTypeIntegrityChecker> integrityCheckers)
      throws RepoTypeNameAlreadyUsedException {

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
    repoType = repoTypeRepository.save(repoType);

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
   *   <li>Renaming to a name used by a <em>different</em> type → {@link
   *       RepoTypeNameAlreadyUsedException}; renaming to the same name is a no-op for uniqueness
   *   <li>{@code name}, {@code description}, {@code aiPrompt}: {@code null} leaves the field
   *       unchanged; a non-null value (including empty string) replaces it
   *   <li>{@code integrityCheckers}: {@code null} leaves the existing checkers unchanged; a
   *       non-null set (including empty) replaces the full set via {@link
   *       #updateIntegrityCheckers} — empty clears all checkers
   * </ul>
   *
   * @param repoTypeId id of the type to update
   * @param name new name, or {@code null} to leave unchanged
   * @param description new description, or {@code null} to leave unchanged
   * @param aiPrompt new prompt, or {@code null} to leave unchanged
   * @param integrityCheckers new checker set, or {@code null} to leave unchanged
   * @return the updated type
   * @throws RepoTypeWithIdNotFoundException if the id does not exist
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
      RepoType existing = repoTypeRepository.findByName(name);
      if (existing != null && !repoType.getId().equals(existing.getId())) {
        throw new RepoTypeNameAlreadyUsedException(name + " is used by another repo type");
      }
      repoType.setName(name);
    }
    if (description != null) {
      repoType.setDescription(description);
    }
    if (aiPrompt != null) {
      repoType.setAiPrompt(aiPrompt);
    }

    if (name != null || description != null || aiPrompt != null) {
      repoType = repoTypeRepository.save(repoType);
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
    repoTypeIntegrityCheckerRepository.deleteByRepoType(repoType);
    repoTypeRepository.delete(repoType);
  }

  /**
   * Replaces the integrity checker set on a repo type.
   *
   * <p>Behavior mirrors {@code RepositoryService#updateAssetIntegrityCheckers}:
   *
   * <ul>
   *   <li>Match existing rows by {@code (assetExtension, integrityCheckerType)} and reuse their ids
   *   <li>Delete rows that are no longer in the incoming set
   *   <li>Insert new rows for pairs that did not exist
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

    if (integrityCheckers == null || integrityCheckers.isEmpty()) {
      logger.debug("Clearing all integrity checkers for repo type id: {}", repoType.getId());
      repoTypeIntegrityCheckerRepository.deleteByRepoType(repoType);
      repoType.setIntegrityCheckers(new HashSet<>());
      return;
    }

    Set<RepoTypeIntegrityChecker> existingCheckers =
        repoTypeIntegrityCheckerRepository.findByRepoType(repoType);
    Map<String, Map<String, RepoTypeIntegrityChecker>> existingToDelete =
        getIntegrityCheckerMap(existingCheckers);

    for (RepoTypeIntegrityChecker integrityChecker : integrityCheckers) {
      logger.debug(
          "Setting repo type for integrity checker: {}", integrityChecker.getAssetExtension());
      integrityChecker.setRepoType(repoType);
      Map<String, RepoTypeIntegrityChecker> existingForExtension =
          existingToDelete.get(integrityChecker.getAssetExtension());
      if (existingForExtension != null) {
        RepoTypeIntegrityChecker existing =
            existingForExtension.get(integrityChecker.getIntegrityCheckerType().name());
        if (existing != null) {
          logger.debug("Reusing existing integrity checker id: {}", existing.getId());
          integrityChecker.setId(existing.getId());
          existingForExtension.remove(integrityChecker.getIntegrityCheckerType().name());
          if (existingForExtension.isEmpty()) {
            existingToDelete.remove(integrityChecker.getAssetExtension());
          }
        }
      }
    }

    logger.debug("Deleting unused integrity checkers for repo type id: {}", repoType.getId());
    for (Map<String, RepoTypeIntegrityChecker> byType : existingToDelete.values()) {
      for (RepoTypeIntegrityChecker toDelete : byType.values()) {
        repoTypeIntegrityCheckerRepository.delete(toDelete);
      }
    }

    repoTypeIntegrityCheckerRepository.saveAll(integrityCheckers);
    repoType.setIntegrityCheckers(integrityCheckers);
    logger.debug("Updated integrity checkers: {}", integrityCheckers.size());
  }

  private Map<String, Map<String, RepoTypeIntegrityChecker>> getIntegrityCheckerMap(
      Set<RepoTypeIntegrityChecker> integrityCheckers) {
    Map<String, Map<String, RepoTypeIntegrityChecker>> map = new HashMap<>();
    for (RepoTypeIntegrityChecker integrityChecker : integrityCheckers) {
      Map<String, RepoTypeIntegrityChecker> byType =
          map.computeIfAbsent(integrityChecker.getAssetExtension(), k -> new HashMap<>());
      byType.put(integrityChecker.getIntegrityCheckerType().name(), integrityChecker);
    }
    return map;
  }
}

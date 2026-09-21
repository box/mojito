package com.box.l10n.mojito.service.repotype;

import com.box.l10n.mojito.entity.RepoType;
import com.box.l10n.mojito.service.assetintegritychecker.integritychecker.IntegrityCheckerType;
import java.util.List;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/**
 * Spring Data access for {@link RepoType}.
 *
 * <p>Not exported as a Spring Data REST resource; callers go through {@link RepoTypeService} and
 * {@link com.box.l10n.mojito.rest.repotype.RepoTypeWS}.
 */
@RepositoryRestResource(exported = false)
public interface RepoTypeRepository extends JpaRepository<RepoType, Long> {

  /**
   * Looks up a repo type by its unique name.
   *
   * @param name exact name to match
   * @return the matching type, or {@code null} if none exists
   */
  RepoType findByName(String name);

  /**
   * Loads the type-owned checker types that apply to one repository and asset extension.
   *
   * <p>The query starts from the repository id so callers do not need to initialize the
   * repository's lazy {@code repoType} association.
   *
   * @param repositoryId repository whose assigned type supplies the checkers
   * @param assetExtension asset extension to match
   * @return matching checker types, or an empty set when the repository is untyped
   */
  @Query(
      """
      select checker.integrityCheckerType
      from Repository repository
      join repository.repoType repoType
      join repoType.integrityCheckers checker
      where repository.id = :repositoryId
        and checker.assetExtension = :assetExtension
      """)
  Set<IntegrityCheckerType> findIntegrityCheckerTypesByRepositoryIdAndAssetExtension(
      @Param("repositoryId") Long repositoryId, @Param("assetExtension") String assetExtension);

  /**
   * Returns all repo types ordered by name ascending.
   *
   * @return all types; empty list if none exist (never {@code null})
   */
  List<RepoType> findAllByOrderByNameAsc();
}

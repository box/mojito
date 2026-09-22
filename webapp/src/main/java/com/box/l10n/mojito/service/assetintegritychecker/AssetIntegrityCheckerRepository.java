package com.box.l10n.mojito.service.assetintegritychecker;

import com.box.l10n.mojito.entity.AssetIntegrityChecker;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.service.assetintegritychecker.integritychecker.IntegrityCheckerType;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/**
 * Persistence for repository-owned checkers, plus the type-owned half of check-time resolution.
 *
 * <p>{@link #findByRepositoryAndAssetExtension} loads checkers stored on the repository. {@link
 * #findTypeIntegrityCheckerTypesByRepositoryIdAndAssetExtension} loads the assigned type's checkers
 * for the same extension without initializing the repository's lazy {@code repoType} association.
 * {@code IntegrityCheckerFactory} unions the two.
 */
@RepositoryRestResource(exported = false)
public interface AssetIntegrityCheckerRepository
    extends JpaRepository<AssetIntegrityChecker, Long> {

  Set<AssetIntegrityChecker> findByRepository(Repository repository);

  Set<AssetIntegrityChecker> findByRepositoryAndAssetExtension(
      Repository repository, String assetExtension);

  /**
   * Type-owned checker types for one repository and asset extension.
   *
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
  Set<IntegrityCheckerType> findTypeIntegrityCheckerTypesByRepositoryIdAndAssetExtension(
      @Param("repositoryId") Long repositoryId, @Param("assetExtension") String assetExtension);

  void deleteByRepository(Repository repository);
}

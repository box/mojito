package com.box.l10n.mojito.service.repotype;

import com.box.l10n.mojito.entity.RepoType;
import com.box.l10n.mojito.entity.RepoTypeIntegrityChecker;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/**
 * Spring Data access for {@link RepoTypeIntegrityChecker} rows.
 *
 * <p>Not exported as a Spring Data REST resource. Used by {@link RepoTypeService} when creating,
 * updating, or deleting a type's checker set.
 */
@RepositoryRestResource(exported = false)
public interface RepoTypeIntegrityCheckerRepository
    extends JpaRepository<RepoTypeIntegrityChecker, Long> {

  /**
   * Loads all integrity checkers owned by the given repo type.
   *
   * @param repoType parent type (must be a persisted entity)
   * @return set of checkers; empty if the type has none (never {@code null})
   */
  Set<RepoTypeIntegrityChecker> findByRepoType(RepoType repoType);

  /**
   * Deletes every integrity checker owned by the given repo type.
   *
   * <p>Used when hard-deleting a type, or when replacing the full checker set. Does nothing if the
   * type has no checkers.
   *
   * @param repoType parent type whose checkers should be removed
   */
  void deleteByRepoType(RepoType repoType);
}

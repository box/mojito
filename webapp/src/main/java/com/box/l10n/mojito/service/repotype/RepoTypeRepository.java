package com.box.l10n.mojito.service.repotype;

import com.box.l10n.mojito.entity.RepoType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
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
   * Returns all repo types ordered by name ascending.
   *
   * @return all types; empty list if none exist (never {@code null})
   */
  List<RepoType> findAllByOrderByNameAsc();
}

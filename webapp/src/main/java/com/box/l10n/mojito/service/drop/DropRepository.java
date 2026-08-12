package com.box.l10n.mojito.service.drop;

import com.box.l10n.mojito.entity.Drop;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.EntityGraph.EntityGraphType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/**
 * @author jaurambault
 */
@RepositoryRestResource(exported = false)
public interface DropRepository extends JpaRepository<Drop, Long>, JpaSpecificationExecutor<Drop> {

  // Note: Cannot use an EntityGraph with pagination as it triggers the following warning:
  // HHH90003004: firstResult/maxResults specified with collection fetch; applying in memory
  // @EntityGraph(value = "Drop.legacy", type = EntityGraphType.FETCH)
  //
  // See {@link com.box.l10n.mojito.service.drop.DropService#findAll()} for manual initialization
  @Override
  Page<Drop> findAll(Specification<Drop> spec, Pageable pageable);

  @Override
  @EntityGraph(value = "Drop.legacy", type = EntityGraphType.FETCH)
  Optional<Drop> findById(Long aLong);
}

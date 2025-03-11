package com.box.l10n.mojito.service.repository;

import com.box.l10n.mojito.entity.Repository;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.EntityGraph.EntityGraphType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/**
 * @author wyau
 */
@RepositoryRestResource(exported = false)
public interface RepositoryRepository
    extends JpaRepository<Repository, Long>, JpaSpecificationExecutor<Repository> {
  @EntityGraph(value = "Repository.legacy", type = EntityGraphType.FETCH)
  Repository findByName(@Param("name") String name);

  List<Repository> findByDeletedFalseOrderByNameAsc();

  @EntityGraph(value = "Repository.legacy", type = EntityGraphType.FETCH)
  @Override
  Optional<Repository> findById(Long aLong);

  @Override
  @EntityGraph(value = "Repository.manualScreenshotRun", type = EntityGraph.EntityGraphType.LOAD)
  List<Repository> findAll(Specification<Repository> s, Sort sort);

  @EntityGraph(value = "Repository.legacy", type = EntityGraphType.FETCH)
  List<Repository>
      findByDeletedFalseAndCheckSLATrueAndRepositoryStatisticOoslaTextUnitCountGreaterThanOrderByNameAsc(
          long statisticsOoslaTextUnitCount);
}

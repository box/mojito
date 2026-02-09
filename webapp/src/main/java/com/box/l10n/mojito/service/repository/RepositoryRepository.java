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
import org.springframework.data.jpa.repository.Query;
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

  Optional<Repository> findNoGraphById(Long aLong);

  @Override
  List<Repository> findAll(Specification<Repository> s, Sort sort);

  @EntityGraph(value = "Repository.legacy", type = EntityGraphType.FETCH)
  List<Repository>
      findByDeletedFalseAndCheckSLATrueAndRepositoryStatisticOoslaTextUnitCountGreaterThanOrderByNameAsc(
          long statisticsOoslaTextUnitCount);

  @Query(
      """
      select new com.box.l10n.mojito.service.repository.RepositorySummaryRow(
        r.id,
        r.createdDate,
        r.name,
        r.description,
        r.sourceLocale.id,
        r.checkSLA,
        rs.id,
        rs.createdDate,
        rs.usedTextUnitCount,
        rs.usedTextUnitWordCount,
        rs.pluralTextUnitCount,
        rs.pluralTextUnitWordCount,
        rs.ooslaTextUnitCount,
        rs.ooslaTextUnitWordCount,
        rs.ooslaCreatedBefore
      )
      from Repository r
      join r.repositoryStatistic rs
      where r.deleted = false
      order by r.name asc
      """)
  List<RepositorySummaryRow> findRepositorySummaryRows();
}

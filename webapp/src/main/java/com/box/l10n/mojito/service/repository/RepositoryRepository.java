package com.box.l10n.mojito.service.repository;

import com.box.l10n.mojito.entity.Repository;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.EntityGraph.EntityGraphType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * @author wyau
 */
//TODO(spring2) review
//@RepositoryRestResource(exported = false)

public interface RepositoryRepository extends JpaRepository<Repository, Long>, JpaSpecificationExecutor<Repository> {
    Repository findByName(@Param("name") String name);
    List<Repository> findByDeletedFalseOrderByNameAsc();
     
    @EntityGraph(value = "Repository.statistics", type = EntityGraphType.LOAD)
    @Override
    public List<Repository> findAll(Specification<Repository> s, Sort sort);

    @EntityGraph(value = "Repository.statistics", type = EntityGraphType.LOAD)
    List<Repository> findByDeletedFalseAndCheckSLATrueAndRepositoryStatisticOoslaTextUnitCountGreaterThanOrderByNameAsc(long statisticsOoslaTextUnitCount);
}

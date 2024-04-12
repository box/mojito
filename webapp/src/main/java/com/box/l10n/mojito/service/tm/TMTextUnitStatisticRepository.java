package com.box.l10n.mojito.service.tm;

import com.box.l10n.mojito.entity.TMTextUnitStatistic;
import com.google.common.annotations.VisibleForTesting;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.EntityGraph.EntityGraphType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/**
 * @author garion
 */
@RepositoryRestResource(exported = false)
public interface TMTextUnitStatisticRepository extends JpaRepository<TMTextUnitStatistic, Long> {
  @VisibleForTesting
  @Override
  @EntityGraph(value = "TMTextUnitStatistic.legacy", type = EntityGraphType.FETCH)
  List<TMTextUnitStatistic> findAll();
}

package com.box.l10n.mojito.service.branch;

import com.box.l10n.mojito.entity.Branch;
import com.box.l10n.mojito.entity.BranchStatistic;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.EntityGraph.EntityGraphType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/**
 * @author jeanaurambault
 */
@RepositoryRestResource(exported = false)
public interface BranchStatisticRepository
    extends JpaRepository<BranchStatistic, Long>, JpaSpecificationExecutor<BranchStatistic> {

  @EntityGraph(value = "BranchStatisticGraph", type = EntityGraphType.FETCH)
  List<BranchStatistic> findByIdIn(List<Long> branchStatisticIds, Sort sort);

  @EntityGraph(
      value = "BranchStatisticGraphWithoutTextUnits",
      type = EntityGraph.EntityGraphType.LOAD)
  @Query(
      """
      select bs
      from BranchStatistic bs
      where bs.id in ?1 and bs.totalCount > ?2
      """)
  List<BranchStatistic> findAllGreaterThanById(List<Long> branchStatisticIds, Long totalCountLte);

  @EntityGraph(value = "BranchStatisticGraph")
  BranchStatistic findByBranch(Branch branch);
}

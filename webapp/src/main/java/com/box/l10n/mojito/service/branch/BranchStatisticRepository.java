package com.box.l10n.mojito.service.branch;

import com.box.l10n.mojito.entity.Branch;
import com.box.l10n.mojito.entity.BranchStatistic;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import java.util.List;

/**
 * @author jeanaurambault
 */
@RepositoryRestResource(exported = false)
public interface BranchStatisticRepository extends JpaRepository<BranchStatistic, Long>, JpaSpecificationExecutor<BranchStatistic> {

    @EntityGraph(value = "BranchStatisticGraph", type = EntityGraph.EntityGraphType.LOAD)
    List<BranchStatistic> findByIdIn(List<Long> branchStatisticIds, Sort sort);

    BranchStatistic findByBranch(Branch branch);
}

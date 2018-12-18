package com.box.l10n.mojito.service.branch;

import com.box.l10n.mojito.entity.Branch;
import com.box.l10n.mojito.entity.BranchStatistic;
import com.box.l10n.mojito.entity.BranchTextUnitStatistic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/**
 * @author jeanaurambault
 */
@RepositoryRestResource(exported = false)
public interface BranchTextUnitStatisticRepository extends JpaRepository<BranchTextUnitStatistic, Long>, JpaSpecificationExecutor<BranchTextUnitStatistic> {

    BranchTextUnitStatistic getByBranchStatisticIdAndTmTextUnitId(long id, long tmTextUnitId);

    @Transactional
    int deleteByBranchStatisticBranchIdAndTmTextUnitIdIn(long branchId, Set<Long> ids);

}

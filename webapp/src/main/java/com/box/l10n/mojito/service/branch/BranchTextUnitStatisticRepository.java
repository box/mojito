package com.box.l10n.mojito.service.branch;

import com.box.l10n.mojito.entity.BranchTextUnitStatistic;
import java.util.List;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.transaction.annotation.Transactional;

/** @author jeanaurambault */
@RepositoryRestResource(exported = false)
public interface BranchTextUnitStatisticRepository
    extends JpaRepository<BranchTextUnitStatistic, Long>,
        JpaSpecificationExecutor<BranchTextUnitStatistic> {

  BranchTextUnitStatistic getByBranchStatisticIdAndTmTextUnitId(long id, long tmTextUnitId);

  @Query("select btus.tmTextUnit.id from #{#entityName} btus where btus.branchStatistic.id = ?1")
  List<Long> findTmTextUnitIds(long branchStatisticId);

  @Query(
      "select count(btus.tmTextUnit.id) from #{#entityName} btus where btus.branchStatistic.branch.id = ?1")
  long countTmTextUnitIds(long branchId);

  @Transactional
  int deleteByBranchStatisticBranchIdAndTmTextUnitIdIn(long branchId, Set<Long> ids);

  @Query(
      "select new com.box.l10n.mojito.service.branch.BranchTextUnitStatisticWithCounts(btus.id, btus.branchStatistic.id, btus.tmTextUnit.id, btus.forTranslationCount, btus.totalCount) "
          + "from #{#entityName} btus where btus.branchStatistic.id = ?1 and btus.tmTextUnit.id in ?2")
  List<BranchTextUnitStatisticWithCounts> getByBranchStatisticIdAndTmTextUnitIdIn(
      long branchStatisticId, List<Long> tmTextUnitIds);
}

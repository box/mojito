package com.box.l10n.mojito.service.branch;

import com.box.l10n.mojito.entity.BranchStatistic;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.lang.Nullable;

/**
 * Interface that defines queries that only return subsets of the BranchStatistics columns.
 *
 * @author garion
 */
public interface SparseBranchStatisticRepository {
    Page<Long> findAllWithIdOnly(@Nullable Specification<BranchStatistic> specification, Pageable pageable);
}

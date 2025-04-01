package com.box.l10n.mojito.service.branch;

import com.box.l10n.mojito.entity.Branch;
import com.box.l10n.mojito.entity.BranchMergeTarget;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(exported = false)
public interface BranchMergeTargetRepository extends JpaRepository<BranchMergeTarget, Long> {
  Optional<BranchMergeTarget> findByBranch(Branch branch);
}

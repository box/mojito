package com.box.l10n.mojito.service.tm;

import com.box.l10n.mojito.entity.BranchSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface BranchSourceRepository extends JpaRepository<BranchSource, Long> {
  @Query(
      value =
          "SELECT bs.*"
              + " FROM branch_source bs"
              + " JOIN tm_text_unit_to_branch tutb ON tutb.branch_id = bs.branch_id"
              + " WHERE tutb.tm_text_unit_id = :textUnitId",
      nativeQuery = true)
  BranchSource findByTextUnitId(Long textUnitId);
}

package com.box.l10n.mojito.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(
    name = "branch_merge_target",
    indexes = {@Index(name = "I__BRANCH_MERGE_TARGET__BRANCH_ID", columnList = "branch_id")})
public class BranchMergeTarget extends BaseEntity {

  @ManyToOne
  @JoinColumn(
      name = "branch_id",
      foreignKey = @ForeignKey(name = "FK__BRANCH_MERGE_TARGET__BRANCH_ID"))
  private Branch branch;

  @Column(name = "targets_main")
  private boolean targetsMain;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "commit_id")
  private Commit commit;

  public Branch getBranch() {
    return branch;
  }

  public void setBranch(Branch branch) {
    this.branch = branch;
  }

  public boolean isTargetsMain() {
    return targetsMain;
  }

  public void setTargetsMain(boolean targetsMain) {
    this.targetsMain = targetsMain;
  }

  public Commit getCommit() {
    return commit;
  }

  public void setCommit(Commit commit) {
    this.commit = commit;
  }
}

package com.box.l10n.mojito.service.branch;

/** @author jaurambault */
public class DeleteBranchJobInput {
  long repositoryId;
  long branchId;

  public long getRepositoryId() {
    return repositoryId;
  }

  public void setRepositoryId(long repositoryId) {
    this.repositoryId = repositoryId;
  }

  public long getBranchId() {
    return branchId;
  }

  public void setBranchId(long branchId) {
    this.branchId = branchId;
  }
}

package com.box.l10n.mojito.service.asset;

import java.util.Set;

/**
 * @author jaurambault
 */
public class DeleteAssetsOfBranchJobInput {
  Set<Long> assetIds;
  long branchId;

  public Set<Long> getAssetIds() {
    return assetIds;
  }

  public void setAssetIds(Set<Long> assetIds) {
    this.assetIds = assetIds;
  }

  public long getBranchId() {
    return branchId;
  }

  public void setBranchId(long branchId) {
    this.branchId = branchId;
  }
}

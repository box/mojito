package com.box.l10n.mojito.service.asset;

import java.util.List;

/**
 * @author jaurambault
 */
public class VirtualTextUnitBatchUpdateJobInput {
  long assetId;
  List<VirtualAssetTextUnit> virtualAssetTextUnits;
  boolean replace;

  public long getAssetId() {
    return assetId;
  }

  public void setAssetId(long assetId) {
    this.assetId = assetId;
  }

  public List<VirtualAssetTextUnit> getVirtualAssetTextUnits() {
    return virtualAssetTextUnits;
  }

  public void setVirtualAssetTextUnits(List<VirtualAssetTextUnit> virtualAssetTextUnits) {
    this.virtualAssetTextUnits = virtualAssetTextUnits;
  }

  public boolean isReplace() {
    return replace;
  }

  public void setReplace(boolean replace) {
    this.replace = replace;
  }
}

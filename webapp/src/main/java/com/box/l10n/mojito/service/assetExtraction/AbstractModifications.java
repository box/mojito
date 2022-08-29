package com.box.l10n.mojito.service.assetExtraction;

import com.box.l10n.mojito.immutables.NoPrefixNoBuiltinContainer;
import com.box.l10n.mojito.localtm.merger.BranchStateTextUnit;
import com.google.common.collect.ImmutableSet;
import org.immutables.value.Value;

@Value.Immutable(singleton = true)
@NoPrefixNoBuiltinContainer
public abstract class AbstractModifications {
  @Value.Default
  public ImmutableSet<BranchStateTextUnit> getAdded() {
    return ImmutableSet.of();
  }

  @Value.Default
  public ImmutableSet<BranchStateTextUnit> getRemoved() {
    return ImmutableSet.of();
  }

  @Value.Default
  public ImmutableSet<BranchStateTextUnit> getUpdated() {
    return ImmutableSet.of();
  }
}

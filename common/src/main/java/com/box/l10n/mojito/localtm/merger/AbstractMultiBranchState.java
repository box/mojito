package com.box.l10n.mojito.localtm.merger;

import com.box.l10n.mojito.immutables.NoPrefixNoBuiltinContainer;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.immutables.value.Value;

@Value.Immutable(singleton = true)
@NoPrefixNoBuiltinContainer
public abstract class AbstractMultiBranchState {

  @Value.Default
  public ImmutableList<BranchStateTextUnit> getBranchStateTextUnits() {
    return ImmutableList.of();
  }

  @Value.Default
  public ImmutableSet<Branch> getBranches() {
    return ImmutableSet.of();
  }
}

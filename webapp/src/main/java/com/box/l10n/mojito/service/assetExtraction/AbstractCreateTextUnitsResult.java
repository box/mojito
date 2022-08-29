package com.box.l10n.mojito.service.assetExtraction;

import com.box.l10n.mojito.immutables.NoPrefixNoBuiltinContainer;
import com.box.l10n.mojito.localtm.merger.BranchStateTextUnit;
import com.box.l10n.mojito.localtm.merger.MultiBranchState;
import com.google.common.collect.ImmutableList;
import org.immutables.value.Value;

@Value.Immutable
@NoPrefixNoBuiltinContainer
public abstract class AbstractCreateTextUnitsResult {
  public abstract MultiBranchState getUpdatedState();

  public abstract ImmutableList<BranchStateTextUnit> getCreatedTextUnits();

  public abstract ImmutableList<TextUnitDTOMatch> getLeveragingMatches();
}

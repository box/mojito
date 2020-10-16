package com.box.l10n.mojito.service.assetExtraction;

import com.box.l10n.mojito.immutables.NoPrefixNoBuiltinContainer;
import com.box.l10n.mojito.localtm.merger.BranchStateTextUnit;
import com.box.l10n.mojito.localtm.merger.MultiBranchState;
import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import com.google.common.collect.ImmutableList;
import org.immutables.value.Value;

import java.util.List;

@Value.Immutable
@NoPrefixNoBuiltinContainer
public abstract class AbstractCreateTextUnitsResult {
    public abstract MultiBranchState getUpdatedState();
    public abstract ImmutableList<BranchStateTextUnit> getCreatedTextUnits();
    public abstract ImmutableList<TextUnitDTOMatch> getLeveragingMatches();
}

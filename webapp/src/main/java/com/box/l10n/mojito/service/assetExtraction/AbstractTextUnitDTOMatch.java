package com.box.l10n.mojito.service.assetExtraction;

import com.box.l10n.mojito.immutables.NoPrefixNoBuiltinContainer;
import com.box.l10n.mojito.localtm.merger.BranchStateTextUnit;
import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import org.immutables.value.Value;

@Value.Immutable
@NoPrefixNoBuiltinContainer
public abstract class AbstractTextUnitDTOMatch {
    public abstract BranchStateTextUnit getSource();
    public abstract TextUnitDTO getMatch();
    public abstract boolean getUniqueMatch();
    public abstract boolean getTranslationNeededIfUniqueMatch();
}

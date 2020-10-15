package com.box.l10n.mojito.service.branch;

import com.box.l10n.mojito.immutables.NoPrefixNoBuiltinContainer;
import com.box.l10n.mojito.localtm.merger.Branch;
import org.immutables.value.Value;

@Value.Immutable
@NoPrefixNoBuiltinContainer
public abstract class AbstractForTranslationCountForTmTextUnitId {
    abstract long getTmTextUnitId();
    abstract long getForTranslationCount();
    abstract long getTotalCount();
    abstract Branch getBranch();
}

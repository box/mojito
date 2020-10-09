package com.box.l10n.mojito.localtm.merger;

import com.box.l10n.mojito.immutables.NoPrefixNoBuiltinContainer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.collect.ImmutableSet;
import org.immutables.value.Value;

import java.time.Instant;
import java.util.Date;

@Value.Immutable(singleton = true)
@NoPrefixNoBuiltinContainer
@JsonDeserialize(builder = BranchData.Builder.class)
public abstract class AbstractBranchData {

    @Value.Default
    public ImmutableSet<String> getUsages() {
        return ImmutableSet.of();
    }
}

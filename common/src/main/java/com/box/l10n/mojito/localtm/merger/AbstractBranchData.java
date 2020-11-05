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

    public static String NULL_BRANCH_TEXT_PLACEHOLDER = "$$MOJITO_DEFAULT$$";
    public static Date NULL_BRANCH_DATE_PLACEHODLER = new Date(Instant.EPOCH.toEpochMilli());

    @Value.Default
    public ImmutableSet<String> getUsages() {
        return ImmutableSet.of();
    }
}

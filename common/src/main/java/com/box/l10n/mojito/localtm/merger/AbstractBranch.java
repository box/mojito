package com.box.l10n.mojito.localtm.merger;

import com.box.l10n.mojito.immutables.NoPrefixNoBuiltinContainer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;
import org.joda.time.DateTime;

import java.time.Instant;
import java.util.Date;

@Value.Immutable
@NoPrefixNoBuiltinContainer
@JsonDeserialize(builder = Branch.Builder.class)
public abstract class AbstractBranch {

    public abstract String getName();

    public abstract DateTime getCreatedAt();
}

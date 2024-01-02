package com.box.l10n.mojito.localtm.merger;

import com.box.l10n.mojito.immutables.NoPrefixNoBuiltinContainer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.time.ZonedDateTime;
import org.immutables.value.Value;

@Value.Immutable
@NoPrefixNoBuiltinContainer
@JsonDeserialize(builder = Branch.Builder.class)
public abstract class AbstractBranch {

  public abstract String getName();

  public abstract ZonedDateTime getCreatedAt();
}

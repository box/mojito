package com.box.l10n.mojito.cli.command.jenkinsstats;

import com.box.l10n.mojito.immutables.NoPrefixNoBuiltinContainer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import javax.annotation.Nullable;
import org.immutables.value.Value;

@Value.Immutable
@NoPrefixNoBuiltinContainer
@JsonDeserialize(builder = JenkinsJobResult.Builder.class)
public abstract class AbstractJenkinsJobResult {

  @Nullable
  abstract String getJobName();

  abstract long getDuration();

  abstract long getId();

  abstract long getNumber();

  @Nullable
  abstract String getResult();

  abstract long getTimestamp();
}

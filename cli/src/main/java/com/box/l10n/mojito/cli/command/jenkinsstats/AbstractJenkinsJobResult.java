package com.box.l10n.mojito.cli.command.jenkinsstats;

import com.box.l10n.mojito.immutables.NoPrefixNoBuiltinContainer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

import javax.annotation.Nullable;

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

package com.box.l10n.mojito.cli.command.jenkinsstats;

import com.box.l10n.mojito.immutables.NoPrefixNoBuiltinContainer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.List;
import org.immutables.value.Value;

@Value.Immutable
@NoPrefixNoBuiltinContainer
@JsonDeserialize(builder = JenkinsJobResults.Builder.class)
public abstract class AbstractJenkinsJobResults {

  abstract String getDisplayName();

  abstract List<JenkinsJobResult> getBuilds();
}

package com.box.l10n.mojito.cli.command.jenkinsstats;

import com.box.l10n.mojito.immutables.NoPrefixNoBuiltinContainer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.List;
import java.util.Map;
import org.immutables.value.Value;

@Value.Immutable
@NoPrefixNoBuiltinContainer
@JsonDeserialize(builder = JenkinsStatsConfig.Builder.class)
public abstract class AbstractJenkinsStatsConfig {

  abstract List<String> getJobs();

  abstract Map<String, String> getAuthCookies();
}

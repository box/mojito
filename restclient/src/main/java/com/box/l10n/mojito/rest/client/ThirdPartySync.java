package com.box.l10n.mojito.rest.client;

import java.util.List;
import java.util.Map;

public class ThirdPartySync {

    public enum Action {
        PUSH,
        PUSH_TRANSLATION,
        PULL,
        MAP_TEXTUNIT,
        PUSH_SCREENSHOT
    }

    Long repositoryId;
    String projectId;
    List<Action> actions;
    String pluralSeparator;
    Map<String, String> localeMappings;
    List<String> options;

    public Long getRepositoryId() {
        return repositoryId;
    }

    public void setRepositoryId(Long repositoryId) {
        this.repositoryId = repositoryId;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public List<Action> getActions() {
        return actions;
    }

    public void setActions(List<Action> actions) {
        this.actions = actions;
    }

    public String getPluralSeparator() {
        return pluralSeparator;
    }

    public void setPluralSeparator(String pluralSeparator) {
        this.pluralSeparator = pluralSeparator;
    }

    public Map<String, String> getLocaleMappings() {
        return localeMappings;
    }

    public void setLocaleMappings(Map<String, String> localeMappings) {
        this.localeMappings = localeMappings;
    }

    public List<String> getOptions() {
        return options;
    }

    public void setOptions(List<String> options) {
        this.options = options;
    }
}

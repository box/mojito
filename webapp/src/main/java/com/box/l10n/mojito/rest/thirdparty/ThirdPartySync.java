package com.box.l10n.mojito.rest.thirdparty;

import com.box.l10n.mojito.service.thirdparty.ThirdPartyService;

import java.util.List;

public class ThirdPartySync {

    Long repositoryId;
    String projectId;
    List<ThirdPartyService.Action> actions;
    String pluralSeparator;
    String localMapping;
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

    public List<ThirdPartyService.Action> getActions() {
        return actions;
    }

    public void setActions(List<ThirdPartyService.Action> actions) {
        this.actions = actions;
    }

    public String getPluralSeparator() {
        return pluralSeparator;
    }

    public void setPluralSeparator(String pluralSeparator) {
        this.pluralSeparator = pluralSeparator;
    }

    public String getLocalMapping() {
        return localMapping;
    }

    public void setLocalMapping(String localMapping) {
        this.localMapping = localMapping;
    }

    public List<String> getOptions() {
        return options;
    }

    public void setOptions(List<String> options) {
        this.options = options;
    }
}

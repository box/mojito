package com.box.l10n.mojito.rest.client;

import com.box.l10n.mojito.rest.ThirdPartySyncAction;

import java.util.ArrayList;
import java.util.List;

public class ThirdPartySync {

    Long repositoryId;
    String projectId;
    List<ThirdPartySyncAction> actions = new ArrayList<>();
    String pluralSeparator;
    String localeMapping;
    String skipTextUnitsWithPattern;
    String skipAssetsWithPathPattern;
    String includeTextUnitsWithPattern;
    List<String> options = new ArrayList<>();

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

    public List<ThirdPartySyncAction> getActions() {
        return actions;
    }

    public void setActions(List<ThirdPartySyncAction> actions) {
        this.actions = actions;
    }

    public String getPluralSeparator() {
        return pluralSeparator;
    }

    public void setPluralSeparator(String pluralSeparator) {
        this.pluralSeparator = pluralSeparator;
    }

    public String getLocaleMapping() {
        return localeMapping;
    }

    public void setLocaleMapping(String localeMapping) {
        this.localeMapping = localeMapping;
    }

    public String getSkipTextUnitsWithPattern() {
        return skipTextUnitsWithPattern;
    }

    public void setSkipTextUnitsWithPattern(String skipTextUnitsWithPattern) {
        this.skipTextUnitsWithPattern = skipTextUnitsWithPattern;
    }

    public String getSkipAssetsWithPathPattern() {
        return skipAssetsWithPathPattern;
    }

    public void setSkipAssetsWithPathPattern(String skipAssetsWithPathPattern) {
        this.skipAssetsWithPathPattern = skipAssetsWithPathPattern;
    }

    public List<String> getOptions() {
        return options;
    }

    public void setOptions(List<String> options) {
        this.options = options;
    }

    public String getIncludeTextUnitsWithPattern() {
        return includeTextUnitsWithPattern;
    }

    public void setIncludeTextUnitsWithPattern(String includeTextUnitsWithPattern) {
        this.includeTextUnitsWithPattern = includeTextUnitsWithPattern;
    }
}

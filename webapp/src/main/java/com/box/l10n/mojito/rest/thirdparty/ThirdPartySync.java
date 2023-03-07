package com.box.l10n.mojito.rest.thirdparty;

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
  Long timeout = 3600L;

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

  public String getIncludeTextUnitsWithPattern() {
    return includeTextUnitsWithPattern;
  }

  public void setIncludeTextUnitsWithPattern(String includeTextUnitsWithPattern) {
    this.includeTextUnitsWithPattern = includeTextUnitsWithPattern;
  }

  public List<String> getOptions() {
    return options;
  }

  public void setOptions(List<String> options) {
    this.options = options;
  }

  public Long getTimeout() {
    return timeout;
  }

  public void setTimeout(Long timeout) {
    this.timeout = timeout;
  }
}

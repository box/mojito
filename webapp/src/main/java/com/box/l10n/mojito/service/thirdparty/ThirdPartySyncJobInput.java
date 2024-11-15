package com.box.l10n.mojito.service.thirdparty;

import com.box.l10n.mojito.entity.ScheduledJob;
import com.box.l10n.mojito.rest.thirdparty.ThirdPartySyncAction;
import com.box.l10n.mojito.service.scheduledjob.jobs.ScheduledThirdPartySyncProperties;
import java.util.List;

/**
 * @author jaurambault
 */
public class ThirdPartySyncJobInput {

  Long repositoryId;
  String thirdPartyProjectId;
  List<ThirdPartySyncAction> actions;
  String pluralSeparator;
  String localeMapping;
  String skipTextUnitsWithPattern;
  String skipAssetsWithPathPattern;
  String includeTextUnitsWithPattern;
  List<String> options;

  public ThirdPartySyncJobInput() {}

  public ThirdPartySyncJobInput(ScheduledJob job, ScheduledThirdPartySyncProperties properties) {
    this.setRepositoryId(job.getRepository().getId());
    this.setThirdPartyProjectId(properties.getThirdPartyProjectId());
    this.setActions(properties.getActions());
    this.setPluralSeparator(properties.getPluralSeparator());
    this.setLocaleMapping(properties.getLocaleMapping());

    this.setSkipTextUnitsWithPattern(
        properties.getSkipTextUnitsWithPattern().isEmpty()
            ? null
            : properties.getSkipTextUnitsWithPattern());
    this.setSkipAssetsWithPathPattern(
        properties.getSkipAssetsWithPathPattern().isEmpty()
            ? null
            : properties.getSkipAssetsWithPathPattern());
    this.setIncludeTextUnitsWithPattern(
        properties.getIncludeTextUnitsWithPattern().isEmpty()
            ? null
            : properties.getIncludeTextUnitsWithPattern());

    this.setOptions(properties.getOptions());
  }

  public Long getRepositoryId() {
    return repositoryId;
  }

  public void setRepositoryId(Long repositoryId) {
    this.repositoryId = repositoryId;
  }

  public String getThirdPartyProjectId() {
    return thirdPartyProjectId;
  }

  public void setThirdPartyProjectId(String thirdPartyProjectId) {
    this.thirdPartyProjectId = thirdPartyProjectId;
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
}

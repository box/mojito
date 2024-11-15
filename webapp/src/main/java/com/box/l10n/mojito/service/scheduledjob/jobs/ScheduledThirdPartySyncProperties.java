package com.box.l10n.mojito.service.scheduledjob.jobs;

import com.box.l10n.mojito.rest.thirdparty.ThirdPartySyncAction;
import com.box.l10n.mojito.service.scheduledjob.ScheduledJobProperties;
import java.util.List;

public class ScheduledThirdPartySyncProperties extends ScheduledJobProperties {
  private String thirdPartyProjectId;
  private List<ThirdPartySyncAction> actions;
  private String pluralSeparator;
  private String localeMapping;
  private String skipTextUnitsWithPattern;
  private String skipAssetsWithPathPattern;
  private String includeTextUnitsWithPattern;
  private List<String> options;

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

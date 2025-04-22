package com.box.l10n.mojito.service.thirdparty;

import com.box.l10n.mojito.rest.thirdparty.ThirdPartySyncAction;
import com.box.l10n.mojito.service.scheduledjob.ScheduledJobType;
import java.util.List;

/**
 * @author jaurambault
 */
public class ThirdPartySyncJobConfig {

  String uuid;
  String cron;
  String repository;
  String thirdPartyProjectId;
  List<ThirdPartySyncAction> actions;
  String pluralSeparator;
  String localeMapping;
  String skipTextUnitsWithPattern;
  String skipAssetsWithPathPattern;
  String includeTextUnitsWithPattern;
  List<String> options;
  ScheduledJobType scheduledJobType = ScheduledJobType.THIRD_PARTY_SYNC;

  public String getUuid() {
    return uuid;
  }

  public void setUuid(String uuid) {
    this.uuid = uuid;
  }

  public String getCron() {
    return cron;
  }

  public void setCron(String cron) {
    this.cron = cron;
  }

  public String getRepository() {
    return repository;
  }

  public void setRepository(String repository) {
    this.repository = repository;
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

  public ScheduledJobType getScheduledJobType() {
    return scheduledJobType;
  }

  public void setScheduledJobType(ScheduledJobType scheduledJobType) {
    this.scheduledJobType = scheduledJobType;
  }
}

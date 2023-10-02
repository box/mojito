package com.box.l10n.mojito.service.thirdparty.smartling.quartz;

public class SmartlingPullLocaleFileJobInput {

  String repositoryName;

  long localeId;

  String localeBcp47Tag;
  String fileName;
  String pluralSeparator;

  String smartlingLocale;

  String smartlingProjectId;

  String smartlingFilePrefix;

  boolean isDeltaPull;

  boolean isPluralFixForLocale;

  boolean isDryRun;

  String schedulerName;

  public String getSmartlingProjectId() {
    return smartlingProjectId;
  }

  public void setSmartlingProjectId(String smartlingProjectId) {
    this.smartlingProjectId = smartlingProjectId;
  }

  public String getSmartlingLocale() {
    return smartlingLocale;
  }

  public void setSmartlingLocale(String smartlingLocale) {
    this.smartlingLocale = smartlingLocale;
  }

  public String getFileName() {
    return fileName;
  }

  public void setFileName(String fileName) {
    this.fileName = fileName;
  }

  public String getPluralSeparator() {
    return pluralSeparator;
  }

  public void setPluralSeparator(String pluralSeparator) {
    this.pluralSeparator = pluralSeparator;
  }

  public String getSmartlingFilePrefix() {
    return smartlingFilePrefix;
  }

  public void setSmartlingFilePrefix(String smartlingFilePrefix) {
    this.smartlingFilePrefix = smartlingFilePrefix;
  }

  public boolean isDeltaPull() {
    return isDeltaPull;
  }

  public void setDeltaPull(boolean deltaPull) {
    isDeltaPull = deltaPull;
  }

  public boolean isPluralFixForLocale() {
    return isPluralFixForLocale;
  }

  public void setPluralFixForLocale(boolean pluralFixForLocale) {
    isPluralFixForLocale = pluralFixForLocale;
  }

  public boolean isDryRun() {
    return isDryRun;
  }

  public void setDryRun(boolean dryRun) {
    isDryRun = dryRun;
  }

  public long getLocaleId() {
    return localeId;
  }

  public void setLocaleId(long localeId) {
    this.localeId = localeId;
  }

  public String getLocaleBcp47Tag() {
    return localeBcp47Tag;
  }

  public void setLocaleBcp47Tag(String localeBcp47Tag) {
    this.localeBcp47Tag = localeBcp47Tag;
  }

  public String getRepositoryName() {
    return repositoryName;
  }

  public void setRepositoryName(String repositoryName) {
    this.repositoryName = repositoryName;
  }

  public String getSchedulerName() {
    return schedulerName;
  }

  public void setSchedulerName(String schedulerName) {
    this.schedulerName = schedulerName;
  }
}

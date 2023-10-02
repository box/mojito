package com.box.l10n.mojito.service.thirdparty.smartling.quartz;

public class SmartlingPullTranslationsJobInput {

  long singularCount;

  long pluralCount;

  String repositoryName;

  String projectId;

  boolean isDeltaPull;

  String pluralFixForLocale;

  boolean isDryRun;

  String pluralSeparator;

  String schedulerName;

  String localeMapping;

  int batchSize;

  boolean isJsonSync;

  boolean isGlossarySync;

  public int getBatchSize() {
    return batchSize;
  }

  public void setBatchSize(int batchSize) {
    this.batchSize = batchSize;
  }

  public long getSingularCount() {
    return singularCount;
  }

  public void setSingularCount(long singularCount) {
    this.singularCount = singularCount;
  }

  public long getPluralCount() {
    return pluralCount;
  }

  public void setPluralCount(long pluralCount) {
    this.pluralCount = pluralCount;
  }

  public String getRepositoryName() {
    return repositoryName;
  }

  public void setRepositoryName(String repositoryName) {
    this.repositoryName = repositoryName;
  }

  public String getProjectId() {
    return projectId;
  }

  public void setProjectId(String projectId) {
    this.projectId = projectId;
  }

  public boolean isDeltaPull() {
    return isDeltaPull;
  }

  public void setDeltaPull(boolean deltaPull) {
    isDeltaPull = deltaPull;
  }

  public String getPluralFixForLocale() {
    return pluralFixForLocale;
  }

  public void setPluralFixForLocale(String pluralFixForLocale) {
    this.pluralFixForLocale = pluralFixForLocale;
  }

  public boolean isDryRun() {
    return isDryRun;
  }

  public void setDryRun(boolean dryRun) {
    isDryRun = dryRun;
  }

  public String getPluralSeparator() {
    return pluralSeparator;
  }

  public void setPluralSeparator(String pluralSeparator) {
    this.pluralSeparator = pluralSeparator;
  }

  public String getSchedulerName() {
    return schedulerName;
  }

  public void setSchedulerName(String schedulerName) {
    this.schedulerName = schedulerName;
  }

  public String getLocaleMapping() {
    return localeMapping;
  }

  public void setLocaleMapping(String localeMapping) {
    this.localeMapping = localeMapping;
  }

  public boolean isJsonSync() {
    return isJsonSync;
  }

  public void setJsonSync(boolean jsonSync) {
    isJsonSync = jsonSync;
  }

  public boolean isGlossarySync() {
    return isGlossarySync;
  }

  public void setGlossarySync(boolean glossarySync) {
    isGlossarySync = glossarySync;
  }
}

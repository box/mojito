package com.box.l10n.mojito.service.thirdparty.smartling.quartz;

public class SmartlingPullBatchFileJobInput {

  Long batchNumber;
  String pluralSeparator;
  String repositoryName;

  String projectId;
  boolean isDeltaPull;
  String localeMapping;
  String filePrefix;

  String pluralFixForLocale;

  String schedulerName;

  boolean isDryRun;

  public Long getBatchNumber() {
    return batchNumber;
  }

  public void setBatchNumber(Long batchNumber) {
    this.batchNumber = batchNumber;
  }

  public String getPluralSeparator() {
    return pluralSeparator;
  }

  public void setPluralSeparator(String pluralSeparator) {
    this.pluralSeparator = pluralSeparator;
  }

  public String getProjectId() {
    return projectId;
  }

  public void setProjectId(String projectId) {
    this.projectId = projectId;
  }

  public String getRepositoryName() {
    return repositoryName;
  }

  public void setRepositoryName(String repositoryName) {
    this.repositoryName = repositoryName;
  }

  public boolean isDeltaPull() {
    return isDeltaPull;
  }

  public void setDeltaPull(boolean deltaPull) {
    isDeltaPull = deltaPull;
  }

  public String getLocaleMapping() {
    return localeMapping;
  }

  public void setLocaleMapping(String localeMapping) {
    this.localeMapping = localeMapping;
  }

  public String getFilePrefix() {
    return filePrefix;
  }

  public void setFilePrefix(String filePrefix) {
    this.filePrefix = filePrefix;
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

  public String getSchedulerName() {
    return schedulerName;
  }

  public void setSchedulerName(String schedulerName) {
    this.schedulerName = schedulerName;
  }
}

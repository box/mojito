package com.box.l10n.mojito.service.asset;

public class AssetMetricsConfigurationProperties {
  private String repository;
  private String mainBranch;
  private int daysInterval;

  public String getRepository() {
    return repository;
  }

  public void setRepository(String repository) {
    this.repository = repository;
  }

  public String getMainBranch() {
    return mainBranch;
  }

  public void setMainBranch(String mainBranch) {
    this.mainBranch = mainBranch;
  }

  public int getDaysInterval() {
    return daysInterval;
  }

  public void setDaysInterval(int daysInterval) {
    this.daysInterval = daysInterval;
  }
}

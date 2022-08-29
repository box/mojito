package com.box.l10n.mojito.rest.entity;

import java.util.HashSet;
import java.util.Set;

/** @author jyi */
public class RepositoryStatistic {

  private Set<RepositoryLocaleStatistic> repositoryLocaleStatistics = new HashSet<>();

  public Set<RepositoryLocaleStatistic> getRepositoryLocaleStatistics() {
    return repositoryLocaleStatistics;
  }

  public void setRepositoryLocaleStatistics(
      Set<RepositoryLocaleStatistic> repositoryLocaleStatistics) {
    this.repositoryLocaleStatistics = repositoryLocaleStatistics;
  }
}

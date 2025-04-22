package com.box.l10n.mojito.service.evolve;

public class EvolveSyncJobInput {
  private Long repositoryId;

  private String localeMapping;

  public EvolveSyncJobInput() {}

  public EvolveSyncJobInput(Long repositoryId, String localeMapping) {
    this.repositoryId = repositoryId;
    this.localeMapping = localeMapping;
  }

  public Long getRepositoryId() {
    return repositoryId;
  }

  public void setRepositoryId(Long repositoryId) {
    this.repositoryId = repositoryId;
  }

  public String getLocaleMapping() {
    return localeMapping;
  }

  public void setLocaleMapping(String localeMapping) {
    this.localeMapping = localeMapping;
  }
}

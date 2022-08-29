package com.box.l10n.mojito.rest.entity;

/**
 * Entity that describes a Commit and PullRun association. This entity mirrors:
 * com.box.l10n.mojito.rest.commit.CommitToPullRunBody
 *
 * @author jaurambault
 */
public class CommitToPullRunBody {

  /** {@link Repository#id} */
  Long repositoryId;

  /** The name of the commit. */
  String commitName;

  /** The name of the PullRun, see: {@link PullRun#name}. */
  String pullRunName;

  public Long getRepositoryId() {
    return repositoryId;
  }

  public void setRepositoryId(Long repositoryId) {
    this.repositoryId = repositoryId;
  }

  public String getCommitName() {
    return commitName;
  }

  public void setCommitName(String commitName) {
    this.commitName = commitName;
  }

  public String getPullRunName() {
    return pullRunName;
  }

  public void setPullRunName(String pullRunName) {
    this.pullRunName = pullRunName;
  }
}

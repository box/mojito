package com.box.l10n.mojito.rest.commit;

import com.box.l10n.mojito.entity.Repository;
import java.util.List;

/** @author garion */
public class CommitListWithRepositoryIdBody {

  /** The candidate commit names to search. */
  List<String> commitNames;

  /** {@link Repository#id} */
  Long repositoryId;

  public List<String> getCommitNames() {
    return commitNames;
  }

  public void setCommitNames(List<String> commitNames) {
    this.commitNames = commitNames;
  }

  public Long getRepositoryId() {
    return repositoryId;
  }

  public void setRepositoryId(Long repositoryId) {
    this.repositoryId = repositoryId;
  }
}

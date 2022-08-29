package com.box.l10n.mojito.entity;

import com.box.l10n.mojito.rest.View;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonView;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import org.hibernate.annotations.BatchSize;

/**
 * Maps a commit to a single {@link PullRun}.
 *
 * @author garion
 */
@Entity
@Table(
    name = "commit_to_pull_run",
    indexes = {
      @Index(name = "UK__COMMIT_TO_PULL_RUN__COMMIT_ID", columnList = "commit_id", unique = true)
    })
@BatchSize(size = 1000)
public class CommitToPullRun extends SettableAuditableEntity {
  @OneToOne
  @JsonBackReference
  @JoinColumn(
      name = "commit_id",
      foreignKey = @ForeignKey(name = "FK__COMMIT_TO_PULL_RUN__COMMIT_ID"))
  private Commit commit;

  @JsonView(View.CommitDetailed.class)
  @JsonManagedReference
  @OneToOne
  @JoinColumn(
      name = "pull_run_id",
      foreignKey = @ForeignKey(name = "FK__COMMIT_TO_PULL_RUN__PULL_RUN_ID"))
  private PullRun pullRun;

  public Commit getCommit() {
    return commit;
  }

  public void setCommit(Commit commit) {
    this.commit = commit;
  }

  public PullRun getPullRun() {
    return pullRun;
  }

  public void setPullRun(PullRun pullRun) {
    this.pullRun = pullRun;
  }
}

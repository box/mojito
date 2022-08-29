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
 * Maps a commit to a single {@link PushRun}.
 *
 * @author garion
 */
@Entity
@Table(
    name = "commit_to_push_run",
    indexes = {
      @Index(name = "UK__COMMIT_TO_PUSH_RUN__COMMIT_ID", columnList = "commit_id", unique = true)
    })
@BatchSize(size = 1000)
public class CommitToPushRun extends SettableAuditableEntity {
  @OneToOne
  @JsonBackReference
  @JoinColumn(
      name = "commit_id",
      foreignKey = @ForeignKey(name = "FK__COMMIT_TO_PUSH_RUN__COMMIT_ID"))
  private Commit commit;

  @JsonView(View.CommitDetailed.class)
  @JsonManagedReference
  @OneToOne
  @JoinColumn(
      name = "push_run_id",
      foreignKey = @ForeignKey(name = "FK__COMMIT_TO_PUSH_RUN__PUSH_RUN_ID"))
  private PushRun pushRun;

  public Commit getCommit() {
    return commit;
  }

  public void setCommit(Commit commit) {
    this.commit = commit;
  }

  public PushRun getPushRun() {
    return pushRun;
  }

  public void setPushRun(PushRun pushRun) {
    this.pushRun = pushRun;
  }
}

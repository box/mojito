package com.box.l10n.mojito.entity;

import com.box.l10n.mojito.rest.View;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
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
  @OneToOne(fetch = FetchType.LAZY)
  @JsonBackReference
  @Schema(hidden = true)
  @JoinColumn(
      name = "commit_id",
      foreignKey = @ForeignKey(name = "FK__COMMIT_TO_PUSH_RUN__COMMIT_ID"))
  private Commit commit;

  @JsonView(View.CommitDetailed.class)
  @JsonManagedReference
  @OneToOne(fetch = FetchType.LAZY)
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

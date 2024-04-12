package com.box.l10n.mojito.entity;

import com.box.l10n.mojito.rest.View;
import com.fasterxml.jackson.annotation.JsonView;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * @author jaurambault
 */
@Entity
@Table(
    name = "git_blame",
    indexes = {
      @Index(name = "I__GIT_BLAME__AUTHOR_EMAIL", columnList = "author_email"),
      @Index(name = "UK__GIT_BLAME__TM_TEXT_UNIT_ID", columnList = "tm_text_unit_id", unique = true)
    })
public class GitBlame extends AuditableEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(
      name = "tm_text_unit_id",
      foreignKey = @ForeignKey(name = "FK__GIT_BLAME__TM_TEXT_UNIT__ID"))
  private TMTextUnit tmTextUnit;

  @JsonView(View.GitBlame.class)
  @Column(name = "author_email")
  private String authorEmail;

  @JsonView(View.GitBlame.class)
  @Column(name = "author_name")
  private String authorName;

  @JsonView(View.GitBlame.class)
  @Column(name = "commit_time")
  private String commitTime;

  @JsonView(View.GitBlame.class)
  @Column(name = "commit_name")
  private String commitName;

  public TMTextUnit getTmTextUnit() {
    return tmTextUnit;
  }

  public void setTmTextUnit(TMTextUnit tmTextUnit) {
    this.tmTextUnit = tmTextUnit;
  }

  public String getAuthorEmail() {
    return authorEmail;
  }

  public void setAuthorEmail(String authorEmail) {
    this.authorEmail = authorEmail;
  }

  public String getAuthorName() {
    return authorName;
  }

  public void setAuthorName(String authorName) {
    this.authorName = authorName;
  }

  public String getCommitTime() {
    return commitTime;
  }

  public void setCommitTime(String commitTime) {
    this.commitTime = commitTime;
  }

  public String getCommitName() {
    return commitName;
  }

  public void setCommitName(String commitName) {
    this.commitName = commitName;
  }
}

package com.box.l10n.mojito.rest.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.ZonedDateTime;

/**
 * Entity that describes a Commit entry. This entity mirrors: com.box.l10n.mojito.entity.Commit
 *
 * @author garion
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Commit {

  protected Long id;
  protected ZonedDateTime createdDate;
  private Repository repository;
  private String name;
  private String authorEmail;

  private String authorName;

  /**
   * The date when the commit was actually commited / merged to the target final branch, i.e.:
   * commit date instead of author date.
   */
  private ZonedDateTime sourceCreationDate;

  private CommitToPushRun commitToPushRun;

  private CommitToPullRun commitToPullRun;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public ZonedDateTime getCreatedDate() {
    return createdDate;
  }

  public void setCreatedDate(ZonedDateTime createdDate) {
    this.createdDate = createdDate;
  }

  public Repository getRepository() {
    return repository;
  }

  public void setRepository(Repository repository) {
    this.repository = repository;
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

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public ZonedDateTime getSourceCreationDate() {
    return sourceCreationDate;
  }

  public void setSourceCreationDate(ZonedDateTime sourceCreationDate) {
    this.sourceCreationDate = sourceCreationDate;
  }

  public CommitToPushRun getCommitToPushRun() {
    return commitToPushRun;
  }

  public void setCommitToPushRun(CommitToPushRun commitToPushRun) {
    this.commitToPushRun = commitToPushRun;
  }

  public CommitToPullRun getCommitToPullRun() {
    return commitToPullRun;
  }

  public void setCommitToPullRun(CommitToPullRun commitToPullRun) {
    this.commitToPullRun = commitToPullRun;
  }
}

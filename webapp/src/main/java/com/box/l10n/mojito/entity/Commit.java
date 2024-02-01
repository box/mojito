package com.box.l10n.mojito.entity;

import com.box.l10n.mojito.rest.View;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import java.time.ZonedDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import org.hibernate.annotations.BatchSize;

/**
 * @author garion
 */
@Entity
@Table(
    name = "commit",
    indexes = {
      @Index(
          name = "UK__COMMIT__NAME_REPOSITORY_ID",
          columnList = "name, repository_id",
          unique = true)
    })
@BatchSize(size = 1000)
public class Commit extends AuditableEntity {

  @JsonIgnore
  @ManyToOne
  @JoinColumn(
      name = "repository_id",
      foreignKey = @ForeignKey(name = "FK__COMMIT__NAME__REPOSITORY_ID"))
  private Repository repository;

  /** Avoid serialization of the full Repository object, include only the IDs. */
  @JsonView(View.Commit.class)
  @JsonProperty("repository_id")
  private Long getRepositoryId() {
    return repository.getId();
  }

  /**
   * The name (or logical ID) of the commit. This is expected to be unique within a Repository and
   * generally would be the commit hash.
   */
  @JsonView(View.IdAndName.class)
  @Column(name = "name")
  private String name;

  @JsonView(View.Commit.class)
  @Column(name = "author_email")
  private String authorEmail;

  @JsonView(View.Commit.class)
  @Column(name = "author_name")
  private String authorName;

  /**
   * The date when the commit was actually commited / merged to the target final branch, i.e.:
   * commit date instead of author date.
   */
  @JsonView(View.Commit.class)
  @Column(name = "source_creation_date")
  private ZonedDateTime sourceCreationDate;

  @JsonView(View.CommitDetailed.class)
  @JsonManagedReference
  @OneToOne(mappedBy = "commit")
  private CommitToPushRun commitToPushRun;

  @JsonView(View.CommitDetailed.class)
  @JsonManagedReference
  @OneToOne(mappedBy = "commit")
  private CommitToPullRun commitToPullRun;

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

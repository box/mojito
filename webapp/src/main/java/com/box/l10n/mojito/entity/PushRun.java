package com.box.l10n.mojito.entity;

import com.box.l10n.mojito.rest.View;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import java.time.ZonedDateTime;
import java.util.Set;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import org.hibernate.annotations.BatchSize;

/**
 * Represents a single instance/run of the push command against a repository.
 *
 * @author garion
 */
@Entity
@Table(
    name = "push_run",
    indexes = {@Index(name = "UK__PUSH_RUN__NAME", columnList = "name", unique = true)})
@BatchSize(size = 1000)
public class PushRun extends SettableAuditableEntity {

  @JsonIgnore
  @ManyToOne
  @JoinColumn(
      name = "repository_id",
      foreignKey = @ForeignKey(name = "FK__PUSH_RUN__REPOSITORY_ID"))
  private Repository repository;

  /** Avoid serialization of the full Repository object, include only the IDs. */
  @JsonView(View.PushRun.class)
  @JsonProperty("repository_id")
  private Long getRepositoryId() {
    return repository.getId();
  }

  /** A unique identifier that is provided for the push run, generally a UUID. */
  @JsonView({View.CommitDetailed.class, View.PushRun.class})
  @Column(name = "name")
  private String name;

  @JsonView(View.PushRun.class)
  @OneToMany(mappedBy = "pushRun")
  @JsonManagedReference
  private Set<PushRunAsset> pushRunAssets;

  public Repository getRepository() {
    return repository;
  }

  public void setRepository(Repository repository) {
    this.repository = repository;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public ZonedDateTime getCreatedDate() {
    return createdDate;
  }

  public void setCreatedDate(ZonedDateTime createdDate) {
    this.createdDate = createdDate;
  }

  public Set<PushRunAsset> getPushRunAssets() {
    return pushRunAssets;
  }

  public void setPushRunAssets(Set<PushRunAsset> pushRunAssetSet) {
    this.pushRunAssets = pushRunAssetSet;
  }
}

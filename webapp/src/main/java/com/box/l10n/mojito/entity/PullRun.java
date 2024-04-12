package com.box.l10n.mojito.entity;

import com.box.l10n.mojito.rest.View;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonView;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.Set;
import org.hibernate.annotations.BatchSize;

/**
 * Represents a single instance/run of the pull command against a repository.
 *
 * @author garion
 */
@Entity
@Table(
    name = "pull_run",
    indexes = {@Index(name = "UK__PULL_RUN__NAME", columnList = "name", unique = true)})
@BatchSize(size = 1000)
public class PullRun extends SettableAuditableEntity {
  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(
      name = "repository_id",
      foreignKey = @ForeignKey(name = "FK__PULL_RUN__REPOSITORY_ID"))
  private Repository repository;

  /** A unique identifier that is provided for the pull run, generally a UUID. */
  @JsonView(View.CommitDetailed.class)
  @Column(name = "name")
  private String name;

  @OneToMany(mappedBy = "pullRun")
  @JsonManagedReference
  private Set<PullRunAsset> pullRunAssets;

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

  public Set<PullRunAsset> getPullRunAssets() {
    return pullRunAssets;
  }

  public void setPullRunAssets(Set<PullRunAsset> pullRunAssets) {
    this.pullRunAssets = pullRunAssets;
  }
}

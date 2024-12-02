package com.box.l10n.mojito.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
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
 * Maps a {@link PullRun} to a set of {@link Asset} entities.
 *
 * @author garion
 */
@Entity
@Table(
    name = "pull_run_asset",
    indexes = {
      @Index(
          name = "UK__PULL_RUN_ASSET__PULL_RUN_ID__ASSET_ID",
          columnList = "pull_run_id, asset_id",
          unique = true)
    })
@BatchSize(size = 1000)
public class PullRunAsset extends SettableAuditableEntity {
  @ManyToOne(fetch = FetchType.EAGER)
  @JsonBackReference
  @JoinColumn(
      name = "pull_run_id",
      foreignKey = @ForeignKey(name = "FK__PULL_RUN_ASSET__PULL_RUN_ID"))
  private PullRun pullRun;

  @ManyToOne(fetch = FetchType.LAZY)
  @JsonBackReference
  @JoinColumn(name = "asset_id", foreignKey = @ForeignKey(name = "FK__PULL_RUN_ASSET__ASSET_ID"))
  private Asset asset;

  @OneToMany(mappedBy = "pullRunAsset")
  @JsonManagedReference
  private Set<PullRunTextUnitVariant> pullRunTextUnitVariants;

  public PullRun getPullRun() {
    return pullRun;
  }

  public void setPullRun(PullRun pullRun) {
    this.pullRun = pullRun;
  }

  public Asset getAsset() {
    return asset;
  }

  public void setAsset(Asset asset) {
    this.asset = asset;
  }

  public Set<PullRunTextUnitVariant> getPullRunTextUnitVariants() {
    return pullRunTextUnitVariants;
  }

  public void setPullRunTextUnitVariants(Set<PullRunTextUnitVariant> pullRunTextUnitVariants) {
    this.pullRunTextUnitVariants = pullRunTextUnitVariants;
  }
}

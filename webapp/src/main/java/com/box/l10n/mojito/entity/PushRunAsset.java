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
import java.util.HashSet;
import java.util.Set;
import org.hibernate.annotations.BatchSize;

/**
 * Maps a {@link PushRun} to a set of {@link Asset} entities.
 *
 * @author garion
 */
@Entity
@Table(
    name = "push_run_asset",
    indexes = {
      @Index(
          name = "UK__PUSH_RUN_ASSET__PUSH_RUN_ID__ASSET_ID",
          columnList = "push_run_id, asset_id",
          unique = true)
    })
@BatchSize(size = 1000)
public class PushRunAsset extends SettableAuditableEntity {
  @ManyToOne(fetch = FetchType.LAZY)
  @JsonBackReference
  @JoinColumn(
      name = "push_run_id",
      foreignKey = @ForeignKey(name = "FK__PUSH_RUN_ASSET__PUSH_RUN_ID"))
  private PushRun pushRun;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "asset_id", foreignKey = @ForeignKey(name = "FK__PUSH_RUN_ASSET__ASSET_ID"))
  private Asset asset;

  @JsonManagedReference
  @OneToMany(mappedBy = "pushRunAsset")
  private Set<PushRunAssetTmTextUnit> pushRunAssetTmTextUnits = new HashSet<>();

  public PushRun getPushRun() {
    return pushRun;
  }

  public void setPushRun(PushRun pushRun) {
    this.pushRun = pushRun;
  }

  public Asset getAsset() {
    return asset;
  }

  public void setAsset(Asset asset) {
    this.asset = asset;
  }

  public Set<PushRunAssetTmTextUnit> getPushRunAssetTmTextUnits() {
    return pushRunAssetTmTextUnits;
  }

  public void setPushRunAssetTmTextUnits(Set<PushRunAssetTmTextUnit> pushRunAssetTmTextUnits) {
    this.pushRunAssetTmTextUnits = pushRunAssetTmTextUnits;
  }
}

package com.box.l10n.mojito.rest.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Entity that describes a PushRun. This entity mirrors: com.box.l10n.mojito.entity.PushRunAsset
 *
 * @author garion
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class PushRunAsset {
  protected Long id;
  protected ZonedDateTime createdDate;

  @JsonBackReference private PushRun pushRun;

  private Asset asset;

  @JsonManagedReference
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

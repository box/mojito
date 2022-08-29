package com.box.l10n.mojito.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import org.hibernate.annotations.BatchSize;

/**
 * Maps an {@link Asset} from a {@link PullRun} to a list of {@link TMTextUnitVariant} entities.
 *
 * <p>The collection of all the Assets of a PullRun together with all of their Text Unit Variants
 * represents all the translations that were used as part of running that pull command instance -
 * and that were exported to the consumer/client/external repo.
 *
 * @author garion
 */
@Entity
@Table(
    name = "pull_run_text_unit_variant",
    indexes = {
      @Index(
          name = "UK__PULL_RUN_TEXT_UNIT_VARIANT__PRA_ID__TUV_ID__LOCALE_ID",
          columnList = "pull_run_asset_id, tm_text_unit_variant_id, locale_id",
          unique = true)
    })
@BatchSize(size = 1000)
public class PullRunTextUnitVariant extends SettableAuditableEntity {
  @ManyToOne
  @JsonBackReference
  @JoinColumn(
      name = "pull_run_asset_id",
      foreignKey = @ForeignKey(name = "FK__PULL_RUN_TEXT_UNIT_VARIANT__PULL_RUN_ASSET_ID"))
  private PullRunAsset pullRunAsset;

  @Basic(optional = false)
  @ManyToOne
  @JoinColumn(
      name = "locale_id",
      foreignKey = @ForeignKey(name = "FK__PULL_RUN_TEXT_UNIT_VARIANT__LOCALE__ID"))
  private Locale locale;

  @ManyToOne
  @JsonManagedReference
  @JoinColumn(
      name = "tm_text_unit_variant_id",
      foreignKey = @ForeignKey(name = "FK__PULL_RUN_TEXT_UNIT_VARIANT__TM_TEXT_UNIT_VARIANT_ID"))
  private TMTextUnitVariant tmTextUnitVariant;

  public PullRunAsset getPullRunAsset() {
    return pullRunAsset;
  }

  public void setPullRunAsset(PullRunAsset pullRunAsset) {
    this.pullRunAsset = pullRunAsset;
  }

  public TMTextUnitVariant getTmTextUnitVariant() {
    return tmTextUnitVariant;
  }

  public void setTmTextUnitVariant(TMTextUnitVariant tmTextUnitVariant) {
    this.tmTextUnitVariant = tmTextUnitVariant;
  }
}

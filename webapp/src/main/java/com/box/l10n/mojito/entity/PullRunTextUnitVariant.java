package com.box.l10n.mojito.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
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
          name = "UK__PULL_RUN_TEXT_UNIT_VARIANT__LOCALE_ID__PRA_ID__TUV_ID__TAG",
          columnList = "pull_run_asset_id, locale_id, output_bcp47_tag, tm_text_unit_variant_id",
          unique = true)
    })
@BatchSize(size = 1000)
public class PullRunTextUnitVariant extends SettableAuditableEntity {
  @ManyToOne(fetch = FetchType.LAZY)
  @JsonBackReference
  @Schema(hidden = true)
  @JoinColumn(
      name = "pull_run_asset_id",
      foreignKey = @ForeignKey(name = "FK__PULL_RUN_TEXT_UNIT_VARIANT__PULL_RUN_ASSET_ID"))
  private PullRunAsset pullRunAsset;

  @Basic(optional = false)
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "locale_id",
      foreignKey = @ForeignKey(name = "FK__PULL_RUN_TEXT_UNIT_VARIANT__LOCALE__ID"))
  private Locale locale;

  @ManyToOne(fetch = FetchType.LAZY)
  @JsonManagedReference
  @JoinColumn(
      name = "tm_text_unit_variant_id",
      foreignKey = @ForeignKey(name = "FK__PULL_RUN_TEXT_UNIT_VARIANT__TM_TEXT_UNIT_VARIANT_ID"))
  private TMTextUnitVariant tmTextUnitVariant;

  @Column(name = "output_bcp47_tag", length = 10)
  private String outputBcp47Tag;

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

  public String getOutputBcp47Tag() {
    return outputBcp47Tag;
  }

  public void setOutputBcp47Tag(String outputBcp47Tag) {
    this.outputBcp47Tag = outputBcp47Tag;
  }
}

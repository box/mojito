package com.box.l10n.mojito.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * @author garion
 */
@Entity
@Table(
        name = "pull_run_text_unit_variant",
        indexes = {
                @Index(name = "UK__PULL_RUN_TEXT_UNIT_VARIANT__EVA_ID__TM_TUV_ID", columnList = "pull_run_asset_id, tm_text_unit_variant_id", unique = true)
        }
)
public class PullRunTextUnitVariant extends SettableAuditableEntity {
    @ManyToOne
    @JsonBackReference
    @JoinColumn(name = "pull_run_asset_id",
            foreignKey = @ForeignKey(name = "FK__PULL_RUN_TEXT_UNIT_VARIANT__PULL_RUN_ASSET_ID"))
    private PullRunAsset pullRunAsset;

    @ManyToOne
    @JsonManagedReference
    @JoinColumn(name = "tm_text_unit_variant_id",
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

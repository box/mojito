package com.box.l10n.mojito.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;

import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * @author garion
 */
@Entity
@Table(name = "push_run_asset_tm_text_unit")
public class PushRunAssetTmTextUnit extends SettableAuditableEntity {
    @ManyToOne
    @JsonBackReference
    @JoinColumn(name = "push_run_asset_id",
            foreignKey = @ForeignKey(name = "FK__PUSH_RUN_ASSET_ID"))
    private PushRunAsset pushRunAsset;

    @ManyToOne
    @JsonBackReference
    @JoinColumn(name = "tm_text_unit_id",
            foreignKey = @ForeignKey(name = "FK__TM_TEXT_UNIT_ID"))
    private TMTextUnit tmTextUnit;

    public PushRunAsset getPushRunAsset() {
        return pushRunAsset;
    }

    public void setPushRunAsset(PushRunAsset pushRunAsset) {
        this.pushRunAsset = pushRunAsset;
    }

    public TMTextUnit getTmTextUnit() {
        return tmTextUnit;
    }

    public void setTmTextUnit(TMTextUnit tmTextUnit) {
        this.tmTextUnit = tmTextUnit;
    }
}

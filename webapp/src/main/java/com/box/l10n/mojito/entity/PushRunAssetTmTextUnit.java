package com.box.l10n.mojito.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;

import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * Maps an {@link Asset} from a {@link PushRun} to a list of {@link TMTextUnit} entities.
 *
 * The collection of all the Assets of a PushRun together with all of their
 * Text Units represents the full set of strings that were extracted by running
 * that Push command.
 *
 * @author garion
 */
@Entity
@Table(name = "push_run_asset_tm_text_unit",
        indexes = {
                @Index(name = "UK__PRATTU__PUSH_RUN_ASSET_ID__TM_TEXT_UNIT_ID",
                        columnList = "push_run_asset_id, tm_text_unit_id", unique = true)
        })
public class PushRunAssetTmTextUnit extends SettableAuditableEntity {
    @ManyToOne
    @JsonBackReference
    @JoinColumn(name = "push_run_asset_id",
            foreignKey = @ForeignKey(name = "FK__PUSH_RUN_ASSET_TM_TEXT_UNIT__PUSH_RUN_ASSET_ID"))
    private PushRunAsset pushRunAsset;

    @ManyToOne
    @JsonBackReference
    @JoinColumn(name = "tm_text_unit_id",
            foreignKey = @ForeignKey(name = "FK__PUSH_RUN_ASSET_TM_TEXT_UNIT__TM_TEXT_UNIT_ID"))
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

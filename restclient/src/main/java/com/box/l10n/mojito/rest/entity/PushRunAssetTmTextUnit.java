package com.box.l10n.mojito.rest.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.joda.time.DateTime;

/**
 * Entity that describes a PushRun.
 * This entity mirrors: com.box.l10n.mojito.entity.PushRunAssetTmTextUnit
 *
 * @author garion
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class PushRunAssetTmTextUnit {
    protected Long id;
    protected DateTime createdDate;

    @JsonBackReference
    private PushRunAsset pushRunAsset;

    private TmTextUnit tmTextUnit;

    public PushRunAsset getPushRunAsset() {
        return pushRunAsset;
    }

    public void setPushRunAsset(PushRunAsset pushRunAsset) {
        this.pushRunAsset = pushRunAsset;
    }

    public TmTextUnit getTmTextUnit() {
        return tmTextUnit;
    }

    public void setTmTextUnit(TmTextUnit tmTextUnit) {
        this.tmTextUnit = tmTextUnit;
    }
}

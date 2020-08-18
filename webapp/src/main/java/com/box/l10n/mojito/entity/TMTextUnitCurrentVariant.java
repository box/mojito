package com.box.l10n.mojito.entity;

import com.box.l10n.mojito.entity.security.user.User;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.hibernate.envers.Audited;
import org.hibernate.envers.RelationTargetAuditMode;
import org.springframework.data.annotation.CreatedBy;

import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * Entity used to mark a {@link TMTextUnitVariant} as current translation of
 * a {@link TMTextUnit} for a given locale.
 *
 * @author jaurambault
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Entity
@Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
@Table(
        name = "tm_text_unit_current_variant",
        indexes = {
                @Index(name = "UK__TM_TEXT_UNIT_ID__LOCALE_ID", columnList = "tm_text_unit_id, locale_id", unique = true)
        }
)
public class TMTextUnitCurrentVariant extends AuditableEntity {

    // This field has been added to be able to rollback a TM to a previous state.
    // Without this field, it would not be possible to filter on a TM, as Envers does not support joins
    @Basic(optional = false)
    @ManyToOne
    @JoinColumn(name = "tm_id", foreignKey = @ForeignKey(name = "FK__TM_TEXT_UNIT_CURRENT_VARIANT__TM__ID"))
    private TM tm;

    /**
     * Denormalization to optimize lookup by asset id, with no joins
     */
    @Basic(optional = false)
    @ManyToOne
    @JoinColumn(name = "asset_id", foreignKey = @ForeignKey(name = "FK__TM_TEXT_UNIT_CURRENT_VARIANT__ASSET__ID"))
    private Asset asset;

    @Basic(optional = false)
    @ManyToOne
    @JoinColumn(name = "tm_text_unit_id", foreignKey = @ForeignKey(name = "FK__TM_TEXT_UNIT_CURRENT_VARIANT__TM_TEXT_UNIT__ID"))
    private TMTextUnit tmTextUnit;

    @Basic(optional = false)
    @ManyToOne
    @JoinColumn(name = "tm_text_unit_variant_id", foreignKey = @ForeignKey(name = "FK__TM_TEXT_UNIT_CURRENT_VARIANT__TM_TEXT_UNIT_VARIANT__ID"))
    private TMTextUnitVariant tmTextUnitVariant;

    @Basic(optional = false)
    @ManyToOne
    @JoinColumn(name = "locale_id", foreignKey = @ForeignKey(name = "FK__TM_TEXT_UNIT_CURRENT_VARIANT__LOCALE__ID"))
    private Locale locale;

    @CreatedBy
    @ManyToOne
    @JoinColumn(name = BaseEntity.CreatedByUserColumnName, foreignKey = @ForeignKey(name = "FK__TM_TEXT_UNIT_CURRENT_VARIANT__USER__ID"))
    protected User createdByUser;

    public User getCreatedByUser() {
        return createdByUser;
    }

    public void setCreatedByUser(User createdByUser) {
        this.createdByUser = createdByUser;
    }

    public TM getTm() {
        return tm;
    }

    public void setTm(TM tm) {
        this.tm = tm;
    }

    public Asset getAsset() {
        return asset;
    }

    public void setAsset(Asset asset) {
        this.asset = asset;
    }

    public TMTextUnit getTmTextUnit() {
        return tmTextUnit;
    }

    public void setTmTextUnit(TMTextUnit tmTextUnit) {
        this.tmTextUnit = tmTextUnit;
    }

    public TMTextUnitVariant getTmTextUnitVariant() {
        return tmTextUnitVariant;
    }

    public void setTmTextUnitVariant(TMTextUnitVariant tmTextUnitVariant) {
        this.tmTextUnitVariant = tmTextUnitVariant;
    }

    public Locale getLocale() {
        return locale;
    }

    public void setLocale(Locale locale) {
        this.locale = locale;
    }
}

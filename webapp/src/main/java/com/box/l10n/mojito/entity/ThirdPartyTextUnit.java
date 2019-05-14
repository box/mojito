package com.box.l10n.mojito.entity;

import javax.persistence.*;

/**
 * Entity used to store the mapping between third party text unit ids and mojito text units.
 *
 * @author chidinmae
 */
@Entity
@Table(name = "third_party_text_unit",
        indexes = {@Index(name = "UK__THIRD_PARTY_TEXT_UNIT__THIRD_PARTY_TEXT_UNIT_ID", columnList = "third_party_text_unit_id", unique = true)})
public class ThirdPartyTextUnit extends AuditableEntity {

    @Column(name = "third_party_text_unit_id")
    private String thirdPartyTextUnitId;

    @Column(name = "mapping_key", length = Integer.MAX_VALUE)
    private String mappingKey;

    @OneToOne
    @JoinColumn(name = "tm_text_unit_id", foreignKey = @ForeignKey(name = "FK__THIRD_PARTY_TEXT_UNIT__TM_TEXT_UNIT__ID"))
    private TMTextUnit tmTextUnit;

    public String getThirdPartyTextUnitId() {
        return thirdPartyTextUnitId;
    }

    public void setThirdPartyTextUnitId(String thirdPartyTextUnitId) {
        this.thirdPartyTextUnitId = thirdPartyTextUnitId;

    }

    public String getMappingKey() {
        return mappingKey;
    }

    public void setMappingKey(String mappingKey) {
        this.mappingKey = mappingKey;
    }

    public TMTextUnit getTmTextUnit() {
        return tmTextUnit;
    }

    public void setTmTextUnit(TMTextUnit tmTextUnit) {
        this.tmTextUnit = tmTextUnit;
    }

}
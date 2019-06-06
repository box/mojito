package com.box.l10n.mojito.service.smartling;

import com.box.l10n.mojito.entity.Asset;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.TMTextUnit;

public class ThirdPartyTextUnitForBatchImport {

    String thirdPartyTextUnitId;
    String mappingKey;
    TMTextUnit tmTextUnit;
    ThirdPartyTextUnitDTO currentThirdPartyTextUnitDTO;
    Repository repository;
    Asset asset;

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

    public ThirdPartyTextUnitDTO getCurrentThirdPartyTextUnitDTO() {
        return currentThirdPartyTextUnitDTO;
    }

    public void setCurrentThirdPartyTextUnitDTO(ThirdPartyTextUnitDTO currentThirdPartyTextUnitDTO) {
        this.currentThirdPartyTextUnitDTO = currentThirdPartyTextUnitDTO;
    }

    public Repository getRepository() {
        return repository;
    }

    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    public Asset getAsset() {
        return asset;
    }

    public void setAsset(Asset asset) {
        this.asset = asset;
    }

}

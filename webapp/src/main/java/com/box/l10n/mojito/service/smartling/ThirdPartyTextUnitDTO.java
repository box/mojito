package com.box.l10n.mojito.service.smartling;

public class ThirdPartyTextUnitDTO {
    final private Long id;
    final String thirdPartyTextUnitId;
    final String mappingKey;
    final private Long tmTextUnitId;
    private String repositoryName;
    private String assetPath;
    private String tmTextUnitName;

    public ThirdPartyTextUnitDTO(Long id, String thirdPartyTextUnitId, String mappingKey, Long tmTextUnitId) {
        this.id = id;
        this.thirdPartyTextUnitId = thirdPartyTextUnitId;
        this.mappingKey = mappingKey;
        this.tmTextUnitId = tmTextUnitId;
    }

    public Long getId() {
        return id;
    }

    String getThirdPartyTextUnitId() {
        return thirdPartyTextUnitId;
    }

    String getMappingKey() {
        return mappingKey;
    }

    public Long getTmTextUnitId() {
        return tmTextUnitId;
    }

    public String getRepositoryName() {
        return repositoryName;
    }

    public void setRepositoryName(String repositoryName) {
        this.repositoryName = repositoryName;
    }

    public String getAssetPath() {
        return assetPath;
    }

    public void setAssetPath(String assetPath) {
        this.assetPath = assetPath;
    }

    String getTmTextUnitName() {
        return tmTextUnitName;
    }

    void setTmTextUnitName(String tmTextUnitName) {
        this.tmTextUnitName = tmTextUnitName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ThirdPartyTextUnitDTO)) return false;

        ThirdPartyTextUnitDTO thirdPartyTextUnitDTO = (ThirdPartyTextUnitDTO) o;

        if (id != null ? !id.equals(thirdPartyTextUnitDTO.id) : thirdPartyTextUnitDTO.id != null) return false;
        if (tmTextUnitId != null ? !tmTextUnitId.equals(thirdPartyTextUnitDTO.tmTextUnitId) : thirdPartyTextUnitDTO.tmTextUnitId != null) return false;
        if (mappingKey != null ? !mappingKey.equals(thirdPartyTextUnitDTO.mappingKey) : thirdPartyTextUnitDTO.mappingKey != null) return false;
        if (thirdPartyTextUnitId != null ? !thirdPartyTextUnitId.equals(thirdPartyTextUnitDTO.thirdPartyTextUnitId) : thirdPartyTextUnitDTO.thirdPartyTextUnitId != null) return false;
        if (repositoryName != null ? !repositoryName.equals(thirdPartyTextUnitDTO.repositoryName) : thirdPartyTextUnitDTO.repositoryName != null) return false;
        if (assetPath != null ? !assetPath.equals(thirdPartyTextUnitDTO.assetPath) : thirdPartyTextUnitDTO.assetPath != null) return false;
        if (tmTextUnitName != null ? !tmTextUnitName.equals(thirdPartyTextUnitDTO.tmTextUnitName) : thirdPartyTextUnitDTO.tmTextUnitName != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (tmTextUnitId != null ? tmTextUnitId.hashCode() : 0);
        result = 31 * result + (mappingKey != null ? mappingKey.hashCode() : 0);
        result = 31 * result + (thirdPartyTextUnitId != null ? thirdPartyTextUnitId.hashCode() : 0);
        result = 31 * result + (repositoryName != null ? repositoryName.hashCode() : 0);
        result = 31 * result + (assetPath != null ? assetPath.hashCode() : 0);
        result = 31 * result + (tmTextUnitName != null ? tmTextUnitName.hashCode() : 0);
        return result;
    }

}
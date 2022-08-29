package com.box.l10n.mojito.service.assetExtraction;

/** @author jyi */
public class AssetMappingDTO {

  private Long assetExtractionId;
  private Long assetTextUnitId;
  private Long tmTextUnitId;

  public AssetMappingDTO(Long assetExtractionId, Long assetTextUnitId, Long tmTextUnitId) {
    this.assetExtractionId = assetExtractionId;
    this.assetTextUnitId = assetTextUnitId;
    this.tmTextUnitId = tmTextUnitId;
  }

  public Long getAssetExtractionId() {
    return assetExtractionId;
  }

  public void setAssetExtractionId(Long assetExtractionId) {
    this.assetExtractionId = assetExtractionId;
  }

  public Long getAssetTextUnitId() {
    return assetTextUnitId;
  }

  public void setAssetTextUnitId(Long assetTextUnitId) {
    this.assetTextUnitId = assetTextUnitId;
  }

  public Long getTmTextUnitId() {
    return tmTextUnitId;
  }

  public void setTmTextUnitId(Long tmTextUnitId) {
    this.tmTextUnitId = tmTextUnitId;
  }
}

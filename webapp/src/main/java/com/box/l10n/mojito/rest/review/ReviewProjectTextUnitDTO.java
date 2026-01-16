package com.box.l10n.mojito.rest.review;

import com.box.l10n.mojito.entity.TMTextUnitVariant;

public class ReviewProjectTextUnitDTO {

  private Long reviewProjectTextUnitId;
  private Long tmTextUnitId;
  private Long tmTextUnitVariantId;
  private Long selectedTmTextUnitVariantId;
  private Long currentTmTextUnitVariantId;
  private String name;
  private String source;
  private String target;
  private TMTextUnitVariant.Status status;
  private Long repositoryId;
  private String repositoryName;
  private String assetPath;
  private boolean includedInLocalizedFile;

  public Long getReviewProjectTextUnitId() {
    return reviewProjectTextUnitId;
  }

  public void setReviewProjectTextUnitId(Long reviewProjectTextUnitId) {
    this.reviewProjectTextUnitId = reviewProjectTextUnitId;
  }

  public Long getTmTextUnitId() {
    return tmTextUnitId;
  }

  public void setTmTextUnitId(Long tmTextUnitId) {
    this.tmTextUnitId = tmTextUnitId;
  }

  public Long getTmTextUnitVariantId() {
    return tmTextUnitVariantId;
  }

  public void setTmTextUnitVariantId(Long tmTextUnitVariantId) {
    this.tmTextUnitVariantId = tmTextUnitVariantId;
  }

  public Long getSelectedTmTextUnitVariantId() {
    return selectedTmTextUnitVariantId;
  }

  public void setSelectedTmTextUnitVariantId(Long selectedTmTextUnitVariantId) {
    this.selectedTmTextUnitVariantId = selectedTmTextUnitVariantId;
  }

  public Long getCurrentTmTextUnitVariantId() {
    return currentTmTextUnitVariantId;
  }

  public void setCurrentTmTextUnitVariantId(Long currentTmTextUnitVariantId) {
    this.currentTmTextUnitVariantId = currentTmTextUnitVariantId;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getSource() {
    return source;
  }

  public void setSource(String source) {
    this.source = source;
  }

  public String getTarget() {
    return target;
  }

  public void setTarget(String target) {
    this.target = target;
  }

  public TMTextUnitVariant.Status getStatus() {
    return status;
  }

  public void setStatus(TMTextUnitVariant.Status status) {
    this.status = status;
  }

  public Long getRepositoryId() {
    return repositoryId;
  }

  public void setRepositoryId(Long repositoryId) {
    this.repositoryId = repositoryId;
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

  public boolean isIncludedInLocalizedFile() {
    return includedInLocalizedFile;
  }

  public void setIncludedInLocalizedFile(boolean includedInLocalizedFile) {
    this.includedInLocalizedFile = includedInLocalizedFile;
  }
}

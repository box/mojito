package com.box.l10n.mojito.rest.entity;

import com.box.l10n.mojito.okapi.extractor.AssetExtractorTextUnit;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AICheckRequest {

  List<AssetExtractorTextUnit> textUnits;
  String repositoryName;

  public AICheckRequest() {}

  public AICheckRequest(List<AssetExtractorTextUnit> textUnits, String repositoryName) {
    this.textUnits = textUnits;
    this.repositoryName = repositoryName;
  }

  public String getRepositoryName() {
    return repositoryName;
  }

  public void setRepositoryName(String repositoryName) {
    this.repositoryName = repositoryName;
  }

  public List<AssetExtractorTextUnit> getTextUnits() {
    return textUnits;
  }

  public void setTextUnits(List<AssetExtractorTextUnit> textUnits) {
    this.textUnits = textUnits;
  }
}

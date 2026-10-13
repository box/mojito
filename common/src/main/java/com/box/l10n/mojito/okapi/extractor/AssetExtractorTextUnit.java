package com.box.l10n.mojito.okapi.extractor;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.io.Serializable;
import java.util.Set;

public class AssetExtractorTextUnit implements Serializable {
  String name;
  String source;
  String comments;
  String pluralForm;
  String pluralFormOther;
  Set<String> usages;

  /**
   * Location of the line that declares the text unit in the asset, for example {@code
   * res/values/strings.xml:4}
   */
  @JsonInclude(JsonInclude.Include.NON_NULL)
  String declarationLocation;

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

  public String getComments() {
    return comments;
  }

  public void setComments(String comments) {
    this.comments = comments;
  }

  public String getPluralForm() {
    return pluralForm;
  }

  public void setPluralForm(String pluralForm) {
    this.pluralForm = pluralForm;
  }

  public String getPluralFormOther() {
    return pluralFormOther;
  }

  public void setPluralFormOther(String pluralFormOther) {
    this.pluralFormOther = pluralFormOther;
  }

  public Set<String> getUsages() {
    return usages;
  }

  public void setUsages(Set<String> usages) {
    this.usages = usages;
  }

  public String getDeclarationLocation() {
    return declarationLocation;
  }

  public void setDeclarationLocation(String declarationLocation) {
    this.declarationLocation = declarationLocation;
  }
}

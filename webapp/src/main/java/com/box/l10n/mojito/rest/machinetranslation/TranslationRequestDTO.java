package com.box.l10n.mojito.rest.machinetranslation;

import com.google.common.base.Objects;
import java.io.Serializable;
import java.util.List;

/**
 * DTO to translate one source string for one target language.
 *
 * @author garion
 */
public class TranslationRequestDTO implements Serializable {
  String textSource;
  String sourceBcp47Tag;
  String targetBcp47Tag;
  boolean skipFunctionalProtection;
  boolean skipLeveraging;
  List<Long> repositoryIds;
  List<String> repositoryNames;

  public String getTextSource() {
    return textSource;
  }

  public void setTextSource(String textSource) {
    this.textSource = textSource;
  }

  public String getSourceBcp47Tag() {
    return sourceBcp47Tag;
  }

  public void setSourceBcp47Tag(String sourceBcp47Tag) {
    this.sourceBcp47Tag = sourceBcp47Tag;
  }

  public String getTargetBcp47Tag() {
    return targetBcp47Tag;
  }

  public void setTargetBcp47Tag(String targetBcp47Tag) {
    this.targetBcp47Tag = targetBcp47Tag;
  }

  public boolean isSkipFunctionalProtection() {
    return skipFunctionalProtection;
  }

  public void setSkipFunctionalProtection(boolean skipFunctionalProtection) {
    this.skipFunctionalProtection = skipFunctionalProtection;
  }

  public boolean isSkipLeveraging() {
    return skipLeveraging;
  }

  public void setSkipLeveraging(boolean skipLeveraging) {
    this.skipLeveraging = skipLeveraging;
  }

  public List<Long> getRepositoryIds() {
    return repositoryIds;
  }

  public void setRepositoryIds(List<Long> repositoryIds) {
    this.repositoryIds = repositoryIds;
  }

  public List<String> getRepositoryNames() {
    return repositoryNames;
  }

  public void setRepositoryNames(List<String> repositoryNames) {
    this.repositoryNames = repositoryNames;
  }

  @Override
  public boolean equals(Object another) {
    if (this == another) {
      return true;
    }

    if (another == null || getClass() != another.getClass()) {
      return false;
    }

    TranslationRequestDTO that = (TranslationRequestDTO) another;

    return skipFunctionalProtection == that.skipFunctionalProtection
        && skipLeveraging == that.skipLeveraging
        && Objects.equal(textSource, that.textSource)
        && Objects.equal(sourceBcp47Tag, that.sourceBcp47Tag)
        && Objects.equal(targetBcp47Tag, that.targetBcp47Tag)
        && Objects.equal(repositoryIds, that.repositoryIds)
        && Objects.equal(repositoryNames, that.repositoryNames);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(
        textSource,
        sourceBcp47Tag,
        targetBcp47Tag,
        skipFunctionalProtection,
        skipLeveraging,
        repositoryIds,
        repositoryNames);
  }
}

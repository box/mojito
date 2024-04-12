package com.box.l10n.mojito.entity;

import com.box.l10n.mojito.entity.security.user.User;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedAttributeNode;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.Table;
import org.springframework.data.annotation.CreatedBy;

/**
 * @author wyau
 */
@Entity
@Table(name = "translation_kit_text_unit")
@NamedEntityGraph(
    name = "TranslationKitTextUnit.legacy",
    attributeNodes = {@NamedAttributeNode("tmTextUnit")})
public class TranslationKitTextUnit extends AuditableEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "translation_kit_id",
      foreignKey = @ForeignKey(name = "FK__TRANSLATION_KIT_TEXT_UNIT__TRANSLATION_KIT__ID"))
  private TranslationKit translationKit;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "tm_text_unit_id",
      foreignKey = @ForeignKey(name = "FK__TRANSLATION_KIT_TEXT_UNIT__TM_TEXT_UNIT__ID"))
  private TMTextUnit tmTextUnit;

  /** The {@link TMTextUnitVariant} created when importing the translation kit */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "imported_tm_text_unit_variant_id",
      foreignKey =
          @ForeignKey(name = "FK__TRANSLATION_KIT_TEXT_UNIT__IMPORTED_TM_TEXT_UNIT_VARIANT__ID"))
  private TMTextUnitVariant importedTmTextUnitVariant;

  /**
   * Stores the current {@link TMTextUnitVariant} if a translation exists for the {@link
   * TMTextUnit}.
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "exported_tm_text_unit_variant_id",
      foreignKey =
          @ForeignKey(name = "FK__TRANSLATION_KIT_TEXT_UNIT__EXPORTED_TM_TEXT_UNIT_VARIANT__ID"))
  private TMTextUnitVariant exportedTmTextUnitVariant;

  @CreatedBy
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = BaseEntity.CreatedByUserColumnName,
      foreignKey = @ForeignKey(name = "FK__TRANSLATION_KIT_TEXT_UNIT__USER__ID"))
  protected User createdByUser;

  public User getCreatedByUser() {
    return createdByUser;
  }

  public void setCreatedByUser(User createdByUser) {
    this.createdByUser = createdByUser;
  }

  /**
   * Indicates if the target (translation) in the imported translation kit is the same as the source
   * (string sent for translation).
   *
   * <p>If {@code true} it might indicate that the text unit wasn't translated by mistake. To detect
   * likely import issue, this attribute should be analyzed at the {@link TranslationKit} level. If
   * all units are untranslated, it's likely that the kit was imported without being modified as
   * expected and it should be reviewed.
   */
  @JoinColumn(name = "source_equals_target")
  private Boolean sourceEqualsTarget;

  // TODO(P1) Move this into its own entity? or keep it here, to much info?
  // can also be set on the text unit not only for TK...
  @JoinColumn(name = "detected_language")
  private String detectedLanguage;

  @JoinColumn(name = "detected_language_expected")
  private String detectedLanguageExpected;

  @JoinColumn(name = "detected_language_probability")
  private Double detectedLanguageProbability;

  @JoinColumn(name = "detected_language_exception")
  private String detectedLanguageException;

  public TranslationKit getTranslationKit() {
    return translationKit;
  }

  public void setTranslationKit(TranslationKit translationKit) {
    this.translationKit = translationKit;
  }

  public TMTextUnit getTmTextUnit() {
    return tmTextUnit;
  }

  public void setTmTextUnit(TMTextUnit tmTextUnit) {
    this.tmTextUnit = tmTextUnit;
  }

  public TMTextUnitVariant getImportedTmTextUnitVariant() {
    return importedTmTextUnitVariant;
  }

  public void setImportedTmTextUnitVariant(TMTextUnitVariant importedTmTextUnitVariant) {
    this.importedTmTextUnitVariant = importedTmTextUnitVariant;
  }

  public TMTextUnitVariant getExportedTmTextUnitVariant() {
    return exportedTmTextUnitVariant;
  }

  public void setExportedTmTextUnitVariant(TMTextUnitVariant exportedTmTextUnitVariant) {
    this.exportedTmTextUnitVariant = exportedTmTextUnitVariant;
  }

  public Boolean getSourceEqualsTarget() {
    return sourceEqualsTarget;
  }

  public void setSourceEqualsTarget(Boolean sourceEqualsTarget) {
    this.sourceEqualsTarget = sourceEqualsTarget;
  }

  public String getDetectedLanguage() {
    return detectedLanguage;
  }

  public void setDetectedLanguage(String detectedLanguage) {
    this.detectedLanguage = detectedLanguage;
  }

  public String getDetectedLanguageExpected() {
    return detectedLanguageExpected;
  }

  public void setDetectedLanguageExpected(String detectedLanguageExpected) {
    this.detectedLanguageExpected = detectedLanguageExpected;
  }

  public Double getDetectedLanguageProbability() {
    return detectedLanguageProbability;
  }

  public void setDetectedLanguageProbability(Double detectedLanguageProbability) {
    this.detectedLanguageProbability = detectedLanguageProbability;
  }

  public String getDetectedLanguageException() {
    return detectedLanguageException;
  }

  public void setDetectedLanguageException(String detectedLanguageException) {
    this.detectedLanguageException = detectedLanguageException;
  }
}

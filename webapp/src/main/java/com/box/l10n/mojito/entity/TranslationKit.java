package com.box.l10n.mojito.entity;

import com.box.l10n.mojito.entity.security.user.User;
import com.box.l10n.mojito.rest.View;
import com.box.l10n.mojito.service.tm.search.StatusFilter;
import com.box.l10n.mojito.service.translationkit.TranslationKitExportedImportedAndCurrentTUV;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonView;
import java.util.Set;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ColumnResult;
import javax.persistence.ConstructorResult;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.SqlResultSetMapping;
import javax.persistence.Table;
import org.hibernate.annotations.NamedNativeQueries;
import org.hibernate.annotations.NamedNativeQuery;
import org.springframework.data.annotation.CreatedBy;

/** @author wyau */
@SqlResultSetMapping(
    name = "TranslationKit.exportedAndCurrentTuvs",
    classes = {
      @ConstructorResult(
          targetClass = TranslationKitExportedImportedAndCurrentTUV.class,
          columns = {
            @ColumnResult(name = "tu_id", type = Long.class),
            @ColumnResult(name = "exported_tuv_id", type = Long.class),
            @ColumnResult(name = "imported_tuv_id", type = Long.class),
            @ColumnResult(name = "current_tuv_id", type = Long.class)
          })
    })
@NamedNativeQueries(
    @NamedNativeQuery(
        name = "TranslationKit.exportedAndCurrentTuvs",
        query =
            "select "
                + "    tktu.tm_text_unit_id                  as tu_id, "
                + "    tktu.exported_tm_text_unit_variant_id as exported_tuv_id, "
                + "    tktu.imported_tm_text_unit_variant_id as imported_tuv_id, "
                + "    tuvc.tm_text_unit_variant_id          as current_tuv_id "
                + "from translation_kit tk"
                + "    inner join translation_kit_text_unit tktu on tk.id = tktu.translation_kit_id "
                + "    left join tm_text_unit_current_variant tuvc on tuvc.tm_text_unit_id = tktu.tm_text_unit_id and tk.locale_id = tuvc.locale_id "
                + "where tktu.translation_kit_id = ?1",
        resultSetMapping = "TranslationKit.exportedAndCurrentTuvs"))
@Entity
@Table(name = "translation_kit")
public class TranslationKit extends AuditableEntity {

  /**
   * Type of the translation kit to be created. Defines which content (text units) will be included
   * in the kit.
   *
   * @author jaurambault
   */
  public enum Type {

    /**
     * To create a kit for the translation process. it includes text units with filter {@link
     * StatusFilter#UNTRANSLATED_OR_TRANSLATION_NEEDED}
     */
    TRANSLATION,
    /**
     * To create a kit for the review process. It includes only text units with filter {@link
     * StatusFilter#REVIEW_NEEDED}
     */
    REVIEW
  }

  @OneToOne
  @JoinColumn(
      name = "locale_id",
      foreignKey = @ForeignKey(name = "FK__TRANSLATION_KIT__LOCALE__ID"))
  @JsonView(View.DropSummary.class)
  private Locale locale;

  @JsonBackReference
  @ManyToOne
  @JoinColumn(name = "drop_id", foreignKey = @ForeignKey(name = "FK__TRANSLATION_KIT__DROP__ID"))
  private Drop drop;

  @JsonView(View.DropSummary.class)
  private Type type = Type.TRANSLATION;

  @Column(name = "num_translation_kit_units")
  private int numTranslationKitUnits;

  @Column(name = "num_translated_translation_kit_units")
  private int numTranslatedTranslationKitUnits;

  @Column(name = "num_source_equals_target")
  private int numSourceEqualsTarget;

  @Column(name = "num_bad_language_detections")
  private int numBadLanguageDetections;

  @JsonView(View.DropSummary.class)
  @Column(name = "word_count")
  private Long wordCount;

  @JsonView(View.DropSummary.class)
  @Column(name = "imported")
  private Boolean imported = false;

  @ElementCollection
  @CollectionTable(
      name = "translation_kit_not_found_text_unit_ids",
      joinColumns = @JoinColumn(name = "TRANSLATION_KIT_ID"),
      foreignKey =
          @ForeignKey(name = "FK__TRANSLATION_KIT_NOT_FOUND_TEXT_UNIT_IDS__TRANSLATION_KIT__ID"))
  private Set<String> notFoundTextUnitIds;

  @CreatedBy
  @ManyToOne
  @JoinColumn(
      name = BaseEntity.CreatedByUserColumnName,
      foreignKey = @ForeignKey(name = "FK__TRANSLATION_KIT__USER__ID"))
  protected User createdByUser;

  public Boolean getImported() {
    return imported;
  }

  public void setImported(Boolean imported) {
    this.imported = imported;
  }

  public User getCreatedByUser() {
    return createdByUser;
  }

  public void setCreatedByUser(User createdByUser) {
    this.createdByUser = createdByUser;
  }

  public Locale getLocale() {
    return locale;
  }

  public void setLocale(Locale locale) {
    this.locale = locale;
  }

  public Drop getDrop() {
    return drop;
  }

  public void setDrop(Drop drop) {
    this.drop = drop;
  }

  public Type getType() {
    return type;
  }

  public void setType(Type type) {
    this.type = type;
  }

  public int getNumTranslationKitUnits() {
    return numTranslationKitUnits;
  }

  public void setNumTranslationKitUnits(int numTranslationKitUnits) {
    this.numTranslationKitUnits = numTranslationKitUnits;
  }

  public int getNumTranslatedTranslationKitUnits() {
    return numTranslatedTranslationKitUnits;
  }

  public void setNumTranslatedTranslationKitUnits(int numTranslatedTranslationKitUnits) {
    this.numTranslatedTranslationKitUnits = numTranslatedTranslationKitUnits;
  }

  public int getNumSourceEqualsTarget() {
    return numSourceEqualsTarget;
  }

  public void setNumSourceEqualsTarget(int numSourceEqualsTarget) {
    this.numSourceEqualsTarget = numSourceEqualsTarget;
  }

  public int getNumBadLanguageDetections() {
    return numBadLanguageDetections;
  }

  public void setNumBadLanguageDetections(int numBadLanguageDetections) {
    this.numBadLanguageDetections = numBadLanguageDetections;
  }

  public Set<String> getNotFoundTextUnitIds() {
    return notFoundTextUnitIds;
  }

  public void setNotFoundTextUnitIds(Set<String> notFoundTextUnitIds) {
    this.notFoundTextUnitIds = notFoundTextUnitIds;
  }

  public Long getWordCount() {
    return wordCount;
  }

  public void setWordCount(Long wordCount) {
    this.wordCount = wordCount;
  }
}

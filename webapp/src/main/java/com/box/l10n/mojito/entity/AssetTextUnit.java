package com.box.l10n.mojito.entity;

import com.box.l10n.mojito.entity.security.user.User;
import com.fasterxml.jackson.annotation.JsonBackReference;
import java.util.Set;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import org.springframework.data.annotation.CreatedBy;

/**
 * @author wyau
 */
@Entity
@Table(
    name = "asset_text_unit",
    indexes = {
      @Index(
          name = "UK__ASSET_TEXT_UNIT__MD5__ASSET_EXTRACTION_ID",
          columnList = "md5, asset_extraction_id",
          unique = true),
      @Index(name = "I__ASSET_TEXT_UNIT__BRANCH_ID", columnList = "branch_id")
    })
public class AssetTextUnit extends AuditableEntity {

  @Column(name = "name", length = 4000)
  private String name;

  @Column(name = "content", length = Integer.MAX_VALUE)
  private String content;

  /** should be built from the name, content and the comment field */
  @Column(name = "md5", length = 32)
  String md5;

  /** should be built from the content only */
  @Column(name = "content_md5", length = 32)
  private String contentMd5;

  @Column(name = "comment", length = Integer.MAX_VALUE)
  private String comment;

  @JsonBackReference("assetTextUnits")
  @ManyToOne
  @JoinColumn(
      name = "asset_extraction_id",
      foreignKey = @ForeignKey(name = "FK__ASSET_TEXT_UNIT__ASSET_EXTRACTION__ID"))
  private AssetExtraction assetExtraction;

  @CreatedBy
  @ManyToOne
  @JoinColumn(
      name = BaseEntity.CreatedByUserColumnName,
      foreignKey = @ForeignKey(name = "FK__ASSET_TEXT_UNIT__USER__ID"))
  private User createdByUser;

  @ManyToOne
  @JoinColumn(
      name = "plural_form_id",
      foreignKey = @ForeignKey(name = "FK__ASSET_TEXT_UNIT__PLURAL_FORM__ID"))
  private PluralForm pluralForm;

  @Column(name = "plural_form_other", length = Integer.MAX_VALUE)
  private String pluralFormOther;

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(
      name = "asset_text_unit_usages",
      joinColumns = @JoinColumn(name = "asset_text_unit_id"),
      foreignKey = @ForeignKey(name = "FK__ASSET_TEXT_UNIT_USAGES__ASSET_TEXT_UNIT__ID"))
  private Set<String> usages;

  @Column(name = "do_not_translate", nullable = false)
  private boolean doNotTranslate = false;

  @ManyToOne
  @JoinColumn(
      name = "branch_id",
      foreignKey = @ForeignKey(name = "FK__ASSET_TEXT_UNIT__BRANCH__ID"))
  protected Branch branch;

  public User getCreatedByUser() {
    return createdByUser;
  }

  public void setCreatedByUser(User createdByUser) {
    this.createdByUser = createdByUser;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }

  public String getMd5() {
    return md5;
  }

  public void setMd5(String md5) {
    this.md5 = md5;
  }

  public String getContentMd5() {
    return contentMd5;
  }

  public void setContentMd5(String contentMd5) {
    this.contentMd5 = contentMd5;
  }

  public String getComment() {
    return comment;
  }

  public void setComment(String comment) {
    this.comment = comment;
  }

  public AssetExtraction getAssetExtraction() {
    return assetExtraction;
  }

  public void setAssetExtraction(AssetExtraction assetExtraction) {
    this.assetExtraction = assetExtraction;
  }

  public PluralForm getPluralForm() {
    return pluralForm;
  }

  public void setPluralForm(PluralForm pluralForm) {
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

  public boolean isDoNotTranslate() {
    return doNotTranslate;
  }

  public void setDoNotTranslate(boolean doNotTranslate) {
    this.doNotTranslate = doNotTranslate;
  }

  public Branch getBranch() {
    return branch;
  }

  public void setBranch(Branch branch) {
    this.branch = branch;
  }
}

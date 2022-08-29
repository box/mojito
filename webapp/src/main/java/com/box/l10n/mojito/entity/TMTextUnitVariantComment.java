package com.box.l10n.mojito.entity;

import com.box.l10n.mojito.entity.security.user.User;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.ForeignKey;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import org.springframework.data.annotation.CreatedBy;

/**
 * Contains comments for a {@link TMTextUnitVariant} with different levels of severity which are
 * linked implicitly to {@link TMTextUnitVariant#reviewNeeded} and {@link
 * TMTextUnitVariant#includedInLocalizedFile}.
 *
 * @author jaurambault
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Entity
@Table(name = "tm_text_unit_variant_comment")
public class TMTextUnitVariantComment extends AuditableEntity {

  /** Types of comment */
  public enum Type {
    LEVERAGING,
    INTEGRITY_CHECK,
    QUALITY_CHECK
  }

  public enum Severity {

    /** Provides information that doesn't require reviews. */
    INFO,
    /** Should be reviewed but the {@link TMTextUnitVariant} can be included. */
    WARNING,
    /**
     * Must be reviewed, {@link TMTextUnitVariant} must not be included in generated files, see
     * {@link TMTextUnitVariant#includedInLocalizedFile}.
     */
    ERROR,
  }

  @JsonBackReference
  @Basic(optional = false)
  @ManyToOne
  @JoinColumn(
      name = "tm_text_unit_variant_id",
      foreignKey = @ForeignKey(name = "FK__TM_TEXT_UNIT_VARIANT_COMMENT__TM_TEXT_UNIT_VARIANT__ID"))
  private TMTextUnitVariant tmTextUnitVariant;

  @Column(name = "severity")
  @Enumerated(EnumType.STRING)
  private Severity severity;

  @Column(name = "type")
  @Enumerated(EnumType.STRING)
  private Type type;

  @Column(name = "content", length = Integer.MAX_VALUE)
  private String content;

  @CreatedBy
  @ManyToOne
  @JoinColumn(
      name = BaseEntity.CreatedByUserColumnName,
      foreignKey = @ForeignKey(name = "FK__TM_TEXT_UNIT_VARIANT_COMMENT__USER__ID"))
  protected User createdByUser;

  public User getCreatedByUser() {
    return createdByUser;
  }

  public void setCreatedByUser(User createdByUser) {
    this.createdByUser = createdByUser;
  }

  public TMTextUnitVariant getTmTextUnitVariant() {
    return tmTextUnitVariant;
  }

  public void setTmTextUnitVariant(TMTextUnitVariant tmTextUnitVariant) {
    this.tmTextUnitVariant = tmTextUnitVariant;
  }

  public Severity getSeverity() {
    return severity;
  }

  public void setSeverity(Severity severity) {
    this.severity = severity;
  }

  public Type getType() {
    return type;
  }

  public void setType(Type type) {
    this.type = type;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }
}

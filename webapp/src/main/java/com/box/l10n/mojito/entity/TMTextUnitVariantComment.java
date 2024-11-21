package com.box.l10n.mojito.entity;

import com.box.l10n.mojito.entity.security.user.User;
import com.box.l10n.mojito.rest.View;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedAttributeNode;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.NamedSubgraph;
import jakarta.persistence.Table;
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
@NamedEntityGraph(
    name = "TMTextUnitVariantComment.legacy",
    attributeNodes = {
      @NamedAttributeNode(
          value = "createdByUser",
          subgraph = "TMTextUnitVariantComment.legacy.createdByUser")
    },
    subgraphs = {
      @NamedSubgraph(
          name = "TMTextUnitVariantComment.legacy.createdByUser",
          attributeNodes = {@NamedAttributeNode(value = "authorities")})
    })
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
  @Schema(hidden = true)
  @Basic(optional = false)
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "tm_text_unit_variant_id",
      foreignKey = @ForeignKey(name = "FK__TM_TEXT_UNIT_VARIANT_COMMENT__TM_TEXT_UNIT_VARIANT__ID"))
  private TMTextUnitVariant tmTextUnitVariant;

  @Column(name = "severity")
  @Enumerated(EnumType.STRING)
  @JsonView(View.TranslationHistorySummary.class)
  private Severity severity;

  @Column(name = "type")
  @Enumerated(EnumType.STRING)
  @JsonView(View.TranslationHistorySummary.class)
  private Type type;

  @Column(name = "content", length = Integer.MAX_VALUE)
  @JsonView(View.TranslationHistorySummary.class)
  private String content;

  @CreatedBy
  @ManyToOne(fetch = FetchType.LAZY)
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

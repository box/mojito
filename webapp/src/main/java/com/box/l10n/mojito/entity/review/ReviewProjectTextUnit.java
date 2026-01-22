package com.box.l10n.mojito.entity.review;

import com.box.l10n.mojito.entity.SettableAuditableEntity;
import com.box.l10n.mojito.entity.TMTextUnit;
import com.box.l10n.mojito.entity.TMTextUnitVariant;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedAttributeNode;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.NamedSubgraph;
import jakarta.persistence.Table;

@Entity
@Table(name = "review_project_text_unit")
@NamedEntityGraph(
    name = "ReviewProjectTextUnit.detail",
    attributeNodes = {
      @NamedAttributeNode(
          value = "tmTextUnit", subgraph = "ReviewProjectTextUnit.detail.tmTextUnit"),
      @NamedAttributeNode("tmTextUnitVariant")
    },
    subgraphs = {
      @NamedSubgraph(
          name = "ReviewProjectTextUnit.detail.tmTextUnit",
          attributeNodes = {
            @NamedAttributeNode("tmTextUnitStatistic"), // TODO(ja) remove eventually see ReviewProjectTextUnitRepository
            @NamedAttributeNode(
                value = "asset", subgraph = "ReviewProjectTextUnit.detail.tmTextUnit.asset")
          }),
      @NamedSubgraph(
          name = "ReviewProjectTextUnit.detail.tmTextUnit.asset",
          attributeNodes = {@NamedAttributeNode("repository")}) // TODO(ja) this does not seem to work. It does a second query to fetch the Repository info
    })
public class ReviewProjectTextUnit extends SettableAuditableEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(
      name = "review_project_id",
      foreignKey = @ForeignKey(name = "FK__REVIEW_PROJECT_TEXT_UNIT__PROJECT"))
  private ReviewProject reviewProject;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "tm_text_unit_variant_id",
      foreignKey = @ForeignKey(name = "FK__REVIEW_PROJECT_TEXT_UNIT__VARIANT"))
  private TMTextUnitVariant tmTextUnitVariant;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(
      name = "tm_text_unit_id",
      foreignKey = @ForeignKey(name = "FK__REVIEW_PROJECT_TEXT_UNIT__TM_TEXT_UNIT"))
  private TMTextUnit tmTextUnit;

  public ReviewProject getReviewProject() {
    return reviewProject;
  }

  public void setReviewProject(ReviewProject reviewProject) {
    this.reviewProject = reviewProject;
  }

  public TMTextUnitVariant getTmTextUnitVariant() {
    return tmTextUnitVariant;
  }

  public void setTmTextUnitVariant(TMTextUnitVariant tmTextUnitVariant) {
    this.tmTextUnitVariant = tmTextUnitVariant;
  }

  public TMTextUnit getTmTextUnit() {
    return tmTextUnit;
  }

  public void setTmTextUnit(TMTextUnit tmTextUnit) {
    this.tmTextUnit = tmTextUnit;
  }
}

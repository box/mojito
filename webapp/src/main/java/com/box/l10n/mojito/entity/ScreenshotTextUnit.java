package com.box.l10n.mojito.entity;

import com.box.l10n.mojito.rest.View;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonView;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedAttributeNode;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.NamedSubgraph;
import jakarta.persistence.Table;

/**
 * Contains the list of text unit names associated with a screenshot
 *
 * @author jaurambault
 */
@Entity
@Table(name = "screenshot_text_unit")
@NamedEntityGraph(
    name = "ScreenshotTextUnit.legacy",
    attributeNodes = {
      @NamedAttributeNode(value = "screenshot", subgraph = "ScreenshotTextUnit.legacy.screenshot"),
      @NamedAttributeNode(value = "tmTextUnitVariant"),
      @NamedAttributeNode(value = "tmTextUnit"),
    },
    subgraphs = {
      @NamedSubgraph(
          name = "ScreenshotTextUnit.legacy.screenshot",
          attributeNodes = {@NamedAttributeNode("screenshotTextUnits")})
    })
public class ScreenshotTextUnit extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JsonBackReference
  @JoinColumn(
      name = "screenshot_id",
      foreignKey = @ForeignKey(name = "FK__SCREENSHOT_TEXT_UNIT__SCREENSHOT__ID"),
      nullable = false)
  private Screenshot screenshot;

  @JsonView(View.Screenshots.class)
  @Column(name = "name")
  private String name;

  @JsonView(View.Screenshots.class)
  @Column(name = "source", length = Integer.MAX_VALUE)
  private String source;

  @JsonView(View.Screenshots.class)
  @Column(name = "target", length = Integer.MAX_VALUE)
  private String target;

  @JsonView(View.Screenshots.class)
  @Column(name = "rendered_target", length = Integer.MAX_VALUE)
  private String renderedTarget;

  @JsonView(View.Screenshots.class)
  @Column(name = "number_of_match")
  private Integer numberOfMatch;

  @JsonView({View.Screenshots.class, View.BranchStatistic.class, View.GitBlameWithUsage.class})
  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(
      name = "tm_text_unit_id",
      foreignKey = @ForeignKey(name = "FK__SCREENSHOT_TEXT_UNIT__TM_TEXT_UNIT__ID"))
  private TMTextUnit tmTextUnit;

  @JsonView(View.Screenshots.class)
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "tm_text_unit_variant_id",
      foreignKey = @ForeignKey(name = "FK__SCREENSHOT_TEXT_UNIT__TM_TEXT_UNIT_VARIANT__ID"))
  private TMTextUnitVariant tmTextUnitVariant;

  public Screenshot getScreenshot() {
    return screenshot;
  }

  public void setScreenshot(Screenshot screenshot) {
    this.screenshot = screenshot;
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

  public String getRenderedTarget() {
    return renderedTarget;
  }

  public void setRenderedTarget(String renderedTarget) {
    this.renderedTarget = renderedTarget;
  }

  public TMTextUnit getTmTextUnit() {
    return tmTextUnit;
  }

  public void setTmTextUnit(TMTextUnit tmTextUnit) {
    this.tmTextUnit = tmTextUnit;
  }

  public TMTextUnitVariant getTmTextUnitVariant() {
    return tmTextUnitVariant;
  }

  public void setTmTextUnitVariant(TMTextUnitVariant tmTextUnitVariant) {
    this.tmTextUnitVariant = tmTextUnitVariant;
  }

  public Integer getNumberOfMatch() {
    return numberOfMatch;
  }

  public void setNumberOfMatch(Integer numberOfMatch) {
    this.numberOfMatch = numberOfMatch;
  }
}

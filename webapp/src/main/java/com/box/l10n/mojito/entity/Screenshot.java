package com.box.l10n.mojito.entity;

import com.box.l10n.mojito.rest.View;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import org.hibernate.annotations.BatchSize;

/**
 * Represents a screenshot.
 *
 * @author jaurambault
 */
@Entity
@Table(
    name = "screenshot",
    indexes = {
      @Index(
          name = "UK__SCREENSHOT__NAME__LOCALE__SCREENSHOT_RUN_ID",
          columnList = "name, locale_id, screenshot_run_id",
          unique = true)
    })
public class Screenshot extends SettableAuditableEntity {

  public enum Status {
    REJECTED,
    NEEDS_REVIEW,
    ACCEPTED;
  };

  @Column(name = "name")
  private String name;

  @JsonView(View.Screenshots.class)
  @Basic(optional = false)
  @ManyToOne
  @JoinColumn(
      name = "screenshot_run_id",
      foreignKey = @ForeignKey(name = "FK__SCREENSHOT__SCREENSHOT_RUN__ID"))
  private ScreenshotRun screenshotRun;

  @JsonView(View.Screenshots.class)
  @ManyToOne
  @JoinColumn(name = "branch_id", foreignKey = @ForeignKey(name = "FK__SCREENSHOT__BRANCH__ID"))
  private Branch branch;

  @JsonView(View.Screenshots.class)
  @Basic(optional = false)
  @ManyToOne
  @JoinColumn(name = "locale_id", foreignKey = @ForeignKey(name = "FK__SCREENSHOT__LOCALE__ID"))
  private Locale locale;

  @JsonView({View.Screenshots.class, View.BranchStatistic.class, View.GitBlameWithUsage.class})
  @Column(name = "src", length = Integer.MAX_VALUE)
  private String src;

  @JsonView(View.Screenshots.class)
  @Column(name = "status", length = 32)
  @Enumerated(EnumType.STRING)
  private Status status = Status.NEEDS_REVIEW;

  @JsonView(View.Screenshots.class)
  @Column(name = "sequence")
  private Long sequence;

  @JsonView(View.Screenshots.class)
  @Column(name = "comment", length = Integer.MAX_VALUE)
  private String comment;

  @JsonView({View.Screenshots.class, View.BranchStatistic.class, View.GitBlameWithUsage.class})
  @JsonManagedReference
  @OneToMany(mappedBy = "screenshot", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
  @JsonProperty("textUnits")
  @JsonDeserialize(as = LinkedHashSet.class)
  @BatchSize(size = 1000)
  private Set<ScreenshotTextUnit> screenshotTextUnits = new LinkedHashSet<>();

  @OneToMany(mappedBy = "screenshot")
  private Set<ThirdPartyScreenshot> thirdPartyScreenshots = new HashSet<>();

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Locale getLocale() {
    return locale;
  }

  public void setLocale(Locale locale) {
    this.locale = locale;
  }

  public String getSrc() {
    return src;
  }

  public void setSrc(String src) {
    this.src = src;
  }

  public String getComment() {
    return comment;
  }

  public void setComment(String comment) {
    this.comment = comment;
  }

  public ScreenshotRun getScreenshotRun() {
    return screenshotRun;
  }

  public void setScreenshotRun(ScreenshotRun screenshotRun) {
    this.screenshotRun = screenshotRun;
  }

  public Status getStatus() {
    return status;
  }

  public void setStatus(Status status) {
    this.status = status;
  }

  public Set<ScreenshotTextUnit> getScreenshotTextUnits() {
    return screenshotTextUnits;
  }

  public void setScreenshotTextUnits(Set<ScreenshotTextUnit> screenshotTextUnits) {
    this.screenshotTextUnits = screenshotTextUnits;
  }

  public Long getSequence() {
    return sequence;
  }

  public void setSequence(Long sequence) {
    this.sequence = sequence;
  }

  public Branch getBranch() {
    return branch;
  }

  public void setBranch(Branch branch) {
    this.branch = branch;
  }

  public Set<ThirdPartyScreenshot> getThirdPartyScreenshots() {
    return thirdPartyScreenshots;
  }

  public void setThirdPartyScreenshots(Set<ThirdPartyScreenshot> thirdPartyScreenshots) {
    this.thirdPartyScreenshots = thirdPartyScreenshots;
  }
}

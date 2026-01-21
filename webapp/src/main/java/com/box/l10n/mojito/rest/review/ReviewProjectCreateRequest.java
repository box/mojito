package com.box.l10n.mojito.rest.review;

import com.box.l10n.mojito.entity.review.ReviewProjectType;
import java.util.List;

public class ReviewProjectCreateRequest {

  private List<Long> repositoryIds;

  private List<String> localeTags;

  private String notes;

  private java.util.List<Long> tmTextUnitIds;

  private ReviewProjectType type = ReviewProjectType.NORMAL;

  private java.time.ZonedDateTime dueDate;

  private java.util.List<String> screenshotImageIds;

  private String name;

  public List<Long> getRepositoryIds() {
    return repositoryIds;
  }

  public void setRepositoryIds(List<Long> repositoryIds) {
    this.repositoryIds = repositoryIds;
  }

  public List<String> getLocaleTags() {
    return localeTags;
  }

  public void setLocaleTags(List<String> localeTags) {
    this.localeTags = localeTags;
  }

  public String getNotes() {
    return notes;
  }

  public void setNotes(String notes) {
    this.notes = notes;
  }

  public java.util.List<Long> getTmTextUnitIds() {
    return tmTextUnitIds;
  }

  public void setTmTextUnitIds(java.util.List<Long> tmTextUnitIds) {
    this.tmTextUnitIds = tmTextUnitIds;
  }

  public ReviewProjectType getType() {
    return type;
  }

  public void setType(ReviewProjectType type) {
    this.type = type;
  }

  public java.time.ZonedDateTime getDueDate() {
    return dueDate;
  }

  public void setDueDate(java.time.ZonedDateTime dueDate) {
    this.dueDate = dueDate;
  }

  public java.util.List<String> getScreenshotImageIds() {
    return screenshotImageIds;
  }

  public void setScreenshotImageIds(java.util.List<String> screenshotImageIds) {
    this.screenshotImageIds = screenshotImageIds;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }
}

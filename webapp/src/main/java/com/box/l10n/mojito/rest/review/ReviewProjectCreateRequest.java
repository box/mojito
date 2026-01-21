package com.box.l10n.mojito.rest.review;

import com.box.l10n.mojito.entity.review.ReviewProjectType;
import java.util.List;
import java.time.ZonedDateTime;

public class ReviewProjectCreateRequest {

  private List<String> localeTags;

  private String notes;

  private List<Long> tmTextUnitIds;

  private ReviewProjectType type = ReviewProjectType.NORMAL;

  private ZonedDateTime dueDate;

  private List<String> screenshotImageIds;

  private String name;

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

  public List<Long> getTmTextUnitIds() {
    return tmTextUnitIds;
  }

  public void setTmTextUnitIds(List<Long> tmTextUnitIds) {
    this.tmTextUnitIds = tmTextUnitIds;
  }

  public ReviewProjectType getType() {
    return type;
  }

  public void setType(ReviewProjectType type) {
    this.type = type;
  }

  public ZonedDateTime getDueDate() {
    return dueDate;
  }

  public void setDueDate(ZonedDateTime dueDate) {
    this.dueDate = dueDate;
  }

  public List<String> getScreenshotImageIds() {
    return screenshotImageIds;
  }

  public void setScreenshotImageIds(List<String> screenshotImageIds) {
    this.screenshotImageIds = screenshotImageIds;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }
}

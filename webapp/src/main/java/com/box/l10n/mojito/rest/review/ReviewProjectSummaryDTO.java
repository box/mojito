package com.box.l10n.mojito.rest.review;

import com.box.l10n.mojito.entity.review.ReviewProjectStatus;
import com.box.l10n.mojito.entity.review.ReviewProjectType;
import java.time.ZonedDateTime;
import java.util.List;

public class ReviewProjectSummaryDTO {

  private Long id;
  private ZonedDateTime createdDate;
  private ZonedDateTime dueDate;
  private String closeReason;
  private Integer textUnitCount;
  private Integer wordCount;
  private ReviewProjectType type;
  private ReviewProjectStatus status;
  private Long requestId;
  private String requestUuid;
  private int totalSelected;
  private long acceptedCount;
  private String name;
  private List<ReviewProjectRepositorySummaryDTO> repositories;
  private List<ReviewProjectLocaleSummaryDTO> locales;
  private List<String> screenshotImageIds;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public ZonedDateTime getCreatedDate() {
    return createdDate;
  }

  public void setCreatedDate(ZonedDateTime createdDate) {
    this.createdDate = createdDate;
  }

  public ZonedDateTime getDueDate() {
    return dueDate;
  }

  public void setDueDate(ZonedDateTime dueDate) {
    this.dueDate = dueDate;
  }

  public String getCloseReason() {
    return closeReason;
  }

  public void setCloseReason(String closeReason) {
    this.closeReason = closeReason;
  }

  public Integer getTextUnitCount() {
    return textUnitCount;
  }

  public void setTextUnitCount(Integer textUnitCount) {
    this.textUnitCount = textUnitCount;
  }

  public Integer getWordCount() {
    return wordCount;
  }

  public void setWordCount(Integer wordCount) {
    this.wordCount = wordCount;
  }

  public ReviewProjectType getType() {
    return type;
  }

  public void setType(ReviewProjectType type) {
    this.type = type;
  }

  public ReviewProjectStatus getStatus() {
    return status;
  }

  public void setStatus(ReviewProjectStatus status) {
    this.status = status;
  }

  public int getTotalSelected() {
    return totalSelected;
  }

  public void setTotalSelected(int totalSelected) {
    this.totalSelected = totalSelected;
  }

  public long getAcceptedCount() {
    return acceptedCount;
  }

  public void setAcceptedCount(long acceptedCount) {
    this.acceptedCount = acceptedCount;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public List<ReviewProjectRepositorySummaryDTO> getRepositories() {
    return repositories;
  }

  public void setRepositories(List<ReviewProjectRepositorySummaryDTO> repositories) {
    this.repositories = repositories;
  }

  public List<ReviewProjectLocaleSummaryDTO> getLocales() {
    return locales;
  }

  public void setLocales(List<ReviewProjectLocaleSummaryDTO> locales) {
    this.locales = locales;
  }

  public Long getRequestId() {
    return requestId;
  }

  public void setRequestId(Long requestId) {
    this.requestId = requestId;
  }

  public String getRequestUuid() {
    return requestUuid;
  }

  public void setRequestUuid(String requestUuid) {
    this.requestUuid = requestUuid;
  }

  public List<String> getScreenshotImageIds() {
    return screenshotImageIds;
  }

  public void setScreenshotImageIds(List<String> screenshotImageIds) {
    this.screenshotImageIds = screenshotImageIds;
  }
}

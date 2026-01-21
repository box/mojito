package com.box.l10n.mojito.entity.review;

import com.box.l10n.mojito.entity.AuditableEntity;
import com.box.l10n.mojito.entity.BaseEntity;
import com.box.l10n.mojito.entity.Locale;
import com.box.l10n.mojito.entity.security.user.User;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.ZonedDateTime;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;

@Entity
@Table(name = "review_project")
public class ReviewProject extends AuditableEntity {

  @Convert(converter = ReviewProjectTypeConverter.class)
  @Column(name = "type", nullable = false)
  private ReviewProjectType type = ReviewProjectType.NORMAL;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private ReviewProjectStatus status = ReviewProjectStatus.OPEN;

  @Column(name = "due_date")
  private ZonedDateTime dueDate;

  @Column(name = "close_reason", length = 512)
  private String closeReason;

  @Column(name = "text_unit_count", nullable = false)
  private Integer textUnitCount = 0;

  @Column(name = "word_count", nullable = false)
  private Integer wordCount = 0;

  // TODO(ja) that seems to be a project request field at this point
  @Column(name = "notes", length = Integer.MAX_VALUE)
  private String notes;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "review_project_request_id",
      foreignKey = @ForeignKey(name = "FK__REVIEW_PROJECT__REQUEST"))
  private ReviewProjectRequest reviewProjectRequest;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "locale_id", foreignKey = @ForeignKey(name = "FK__REVIEW_PROJECT__LOCALE"))
  private Locale locale;

  @CreatedBy
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = BaseEntity.CreatedByUserColumnName,
      foreignKey = @ForeignKey(name = "FK__REVIEW_PROJECT__CREATED_BY"))
  @JsonIgnore
  private User createdByUser;

  // TODO(ja) why do we have save that? can't we just infer from the changes, plus i would think that a project is chnaged only by one person
//    so it would be more like "assigned". if change by multiple user we can see by tracking the change at the text unit level accepted by user, no?
  @LastModifiedBy
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "last_modified_by_user_id",
      foreignKey = @ForeignKey(name = "FK__REVIEW_PROJECT__LAST_MODIFIED_BY"))
  @JsonIgnore
  private User lastModifiedByUser;

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

  public String getNotes() {
    return notes;
  }

  public void setNotes(String notes) {
    this.notes = notes;
  }

  public User getCreatedByUser() {
    return createdByUser;
  }

  public void setCreatedByUser(User createdByUser) {
    this.createdByUser = createdByUser;
  }

  public User getLastModifiedByUser() {
    return lastModifiedByUser;
  }

  public void setLastModifiedByUser(User lastModifiedByUser) {
    this.lastModifiedByUser = lastModifiedByUser;
  }

  public Locale getLocale() {
    return locale;
  }

  public void setLocale(Locale locale) {
    this.locale = locale;
  }

  public ReviewProjectRequest getReviewProjectRequest() {
    return reviewProjectRequest;
  }

  public void setReviewProjectRequest(ReviewProjectRequest reviewProjectRequest) {
    this.reviewProjectRequest = reviewProjectRequest;
  }
}

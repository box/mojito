package com.box.l10n.mojito.entity;

import com.box.l10n.mojito.aspect.JsonRawString;
import com.box.l10n.mojito.entity.security.user.User;
import com.box.l10n.mojito.rest.View;
import com.box.l10n.mojito.service.pollableTask.InjectCurrentTask;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.annotation.JsonView;
import java.time.ZonedDateTime;
import java.util.Set;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import org.hibernate.annotations.BatchSize;
import org.springframework.data.annotation.CreatedBy;

/** @author jaurambault */
@JsonIgnoreProperties(ignoreUnknown = true)
@Entity
@Table(
    name = "pollable_task",
    indexes = {
      @Index(name = "I__POLLABLE_TASK__NAME", columnList = "name"),
      @Index(name = "I__POLLABLE_TASK__FINISHED_DATE", columnList = "finished_date")
    })
@BatchSize(size = 1000)
public class PollableTask extends AuditableEntity {

  /**
   * Constant that can be passed to functions that use {@link InjectCurrentTask} for readability
   * purpose.
   */
  public static final PollableTask INJECT_CURRENT_TASK = null;

  @Basic(optional = false)
  @Column(name = "name")
  private String name;

  @Column(name = "finished_date")
  private ZonedDateTime finishedDate;

  @JsonIgnore
  @Column(name = "message", length = Integer.MAX_VALUE)
  private String message;

  @JsonIgnore
  @Column(name = "error_message", length = Integer.MAX_VALUE)
  private String errorMessage;

  @Column(name = "error_stacks", length = Integer.MAX_VALUE)
  private String errorStack;

  @Basic(optional = false)
  @Column(name = "expected_sub_task_number")
  private int expectedSubTaskNumber = 0;

  @OneToMany(mappedBy = "parentTask", fetch = FetchType.EAGER)
  @OrderBy("id")
  @BatchSize(size = 1000)
  private Set<PollableTask> subTasks;

  @JsonBackReference
  @ManyToOne
  @JoinColumn(
      name = "parent_task_id",
      foreignKey = @ForeignKey(name = "FK__POLLABLE_TASK__POLLABLE_TASK__ID"))
  private PollableTask parentTask;

  @JsonIgnore
  @Column(name = "timeout")
  private Long timeout;

  @CreatedBy
  @ManyToOne
  @JoinColumn(
      name = BaseEntity.CreatedByUserColumnName,
      foreignKey = @ForeignKey(name = "FK__POLLABLE_TASK__USER__ID"))
  protected User createdByUser;

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

  public ZonedDateTime getFinishedDate() {
    return finishedDate;
  }

  public void setFinishedDate(ZonedDateTime finishedDate) {
    this.finishedDate = finishedDate;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public void setErrorMessage(String errorMessage) {
    this.errorMessage = errorMessage;
  }

  public String getErrorStack() {
    return errorStack;
  }

  public void setErrorStack(String errorStack) {
    this.errorStack = errorStack;
  }

  public int getExpectedSubTaskNumber() {
    return expectedSubTaskNumber;
  }

  public void setExpectedSubTaskNumber(int expectedSubTaskNumber) {
    this.expectedSubTaskNumber = expectedSubTaskNumber;
  }

  public Set<PollableTask> getSubTasks() {
    return subTasks;
  }

  public void setSubTasks(Set<PollableTask> subTasks) {
    this.subTasks = subTasks;
  }

  public PollableTask getParentTask() {
    return parentTask;
  }

  public void setParentTask(PollableTask parentTask) {
    this.parentTask = parentTask;
  }

  public Long getTimeout() {
    return timeout;
  }

  public void setTimeout(Long timeout) {
    this.timeout = timeout;
  }

  @JsonProperty(value = "message")
  @JsonRawValue
  @JsonRawString
  public String getMessageAsJson() {
    return message;
  }

  @JsonProperty(value = "errorMessage")
  @JsonRawValue
  @JsonRawString
  public String getErrorMessageAsJson() {
    return errorMessage;
  }

  /**
   * NOTE review, don't like to have business logic here... but overkill to move somewhere else
   *
   * <p>Indicates if this task and all its sub tasks are finished.
   *
   * @return {@code true} if this task and all its subtasks are finished else {@code false}
   */
  @JsonProperty
  @JsonView(View.PollableSummary.class)
  public boolean isAllFinished() {

    boolean currentTaskFinished = (getFinishedDate() != null);
    boolean allSubtasksFinished = true;

    // If parent task is finished, check all its subtasks
    if (currentTaskFinished) {

      if (getSubTasks() != null) {
        int numSubtasks = getSubTasks().size();

        // if not all expected subtasks have run => not finished
        if (numSubtasks < getExpectedSubTaskNumber()) {
          allSubtasksFinished = false;
        } else {
          // a task is finished only if all its subtasks are finished
          for (PollableTask pollableTask : subTasks) {
            if (!pollableTask.isAllFinished()) {
              allSubtasksFinished = false;
              break;
            }
          }
        }
      }
    }

    return currentTaskFinished && allSubtasksFinished;
  }
}

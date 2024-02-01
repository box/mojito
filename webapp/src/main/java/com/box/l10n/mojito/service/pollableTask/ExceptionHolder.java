package com.box.l10n.mojito.service.pollableTask;

import com.box.l10n.mojito.entity.PollableTask;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * @author jaurambault
 */
@JsonAutoDetect(
    fieldVisibility = JsonAutoDetect.Visibility.NONE,
    getterVisibility = JsonAutoDetect.Visibility.NONE,
    isGetterVisibility = JsonAutoDetect.Visibility.NONE)
@JsonPropertyOrder(alphabetic = true)
public class ExceptionHolder {

  Exception exception;
  boolean expected;
  PollableTask pollableTask;

  public ExceptionHolder(PollableTask pollableTask) {
    this.pollableTask = pollableTask;
  }

  @JsonProperty("expected")
  public boolean isExpected() {
    return expected;
  }

  public void setExpected(boolean expected) {
    this.expected = expected;
  }

  public Exception getException() {
    return exception;
  }

  public void setException(Exception exception) {
    this.exception = exception;
  }

  public PollableTask getPollableTask() {
    return pollableTask;
  }

  public void setPollableTask(PollableTask pollableTask) {
    this.pollableTask = pollableTask;
  }

  @JsonProperty("type")
  public String getType() {
    String type;

    if (isExpected()) {
      type = exception.getClass().getName();
    } else {
      type = "unexpected";
    }

    return type;
  }

  @JsonProperty("message")
  public String getMessage() {
    String type;

    if (isExpected()) {
      type = exception.getMessage();
    } else {
      type = "An unexpected error happened, task=" + pollableTask.getId();
    }

    return type;
  }
}

package com.box.l10n.mojito.quartz;

import java.util.Date;

public class QuartzJobInfo<I, O> {
  Class<? extends QuartzPollableJob<I, O>> clazz;
  I input;
  Long parentId;
  String message;
  int expectedSubTaskNumber;
  Date triggerStartDate;
  String uniqueId;
  boolean inlineInput;
  long timeout;

  private QuartzJobInfo(Builder<I, O> builder) {
    clazz = builder.clazz;
    input = builder.input;
    parentId = builder.parentId;
    message = builder.message;
    expectedSubTaskNumber = builder.expectedSubTaskNumber;
    triggerStartDate = builder.triggerStartDate;
    uniqueId = builder.uniqueId;
    inlineInput = builder.inlineInput;
    timeout = builder.timeout;
  }

  public Class<? extends QuartzPollableJob<I, O>> getClazz() {
    return clazz;
  }

  public I getInput() {
    return input;
  }

  public Long getParentId() {
    return parentId;
  }

  public String getMessage() {
    return message;
  }

  public int getExpectedSubTaskNumber() {
    return expectedSubTaskNumber;
  }

  public Date getTriggerStartDate() {
    return triggerStartDate;
  }

  public String getUniqueId() {
    return uniqueId;
  }

  public boolean isInlineInput() {
    return inlineInput;
  }

  public long getTimeout() {
    return timeout;
  }

  public static <I, O> Builder<I, O> newBuilder(Class<? extends QuartzPollableJob<I, O>> clazz) {
    Builder<I, O> builder = new Builder<I, O>();
    builder.clazz = clazz;
    return builder;
  }

  public static final class Builder<I, O> {
    private Class<? extends QuartzPollableJob<I, O>> clazz;
    private I input;
    private Long parentId;
    private String message;
    private int expectedSubTaskNumber = 0;
    private Date triggerStartDate = new Date();
    private String uniqueId;
    private boolean inlineInput = true;
    private long timeout = 3600;

    private Builder() {}

    public Builder<I, O> withInput(I val) {
      input = val;
      return this;
    }

    public Builder<I, O> withParentId(Long val) {
      parentId = val;
      return this;
    }

    public Builder<I, O> withMessage(String val) {
      message = val;
      return this;
    }

    public Builder<I, O> withExpectedSubTaskNumber(int val) {
      expectedSubTaskNumber = val;
      return this;
    }

    public Builder<I, O> withTriggerStartDate(Date val) {
      triggerStartDate = val;
      return this;
    }

    public Builder<I, O> withUniqueId(String val) {
      uniqueId = val;
      return this;
    }

    public Builder<I, O> withInlineInput(boolean val) {
      inlineInput = val;
      return this;
    }

    public Builder<I, O> withTimeout(long val) {
      timeout = val;
      return this;
    }

    public QuartzJobInfo<I, O> build() {
      return new QuartzJobInfo<I, O>(this);
    }
  }
}

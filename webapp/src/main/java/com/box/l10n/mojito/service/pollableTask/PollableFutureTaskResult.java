package com.box.l10n.mojito.service.pollableTask;

import com.box.l10n.mojito.entity.PollableTask;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Implementation of {@link PollableFuture} to be used in function instrumented with {@link
 * Pollable} to return the result.
 *
 * @author jaurambault
 * @param <T> result type
 */
public class PollableFutureTaskResult<T> implements PollableFuture<T> {

  /** The returned result */
  T result;

  /** Keep track of the function execution */
  PollableTask pollableTask;

  /** if not {@code null}, the new {@link PollableTask#message} to be saved by the aspect */
  String messageOverride;

  /**
   * if not {@code null}, the new {@link PollableTask#expectedSubTaskNumber} to be saved by the
   * aspect
   */
  Integer expectedSubTaskNumberOverride;

  public PollableFutureTaskResult() {}

  public PollableFutureTaskResult(T result) {
    this.result = result;
  }

  @Override
  public T get() throws InterruptedException, ExecutionException {
    return getResult();
  }

  @Override
  public T get(long timeout, TimeUnit unit)
      throws InterruptedException, ExecutionException, TimeoutException {
    return getResult();
  }

  @Override
  public PollableTask getPollableTask() {
    return pollableTask;
  }

  public void setPollableTask(PollableTask pollableTask) {
    this.pollableTask = pollableTask;
  }

  public T getResult() {
    return result;
  }

  public void setResult(T result) {
    this.result = result;
  }

  public String getMessageOverride() {
    return messageOverride;
  }

  public void setMessageOverride(String messageOverride) {
    this.messageOverride = messageOverride;
  }

  public Integer getExpectedSubTaskNumberOverride() {
    return expectedSubTaskNumberOverride;
  }

  public void setExpectedSubTaskNumberOverride(Integer expectedSubTaskNumberOverride) {
    this.expectedSubTaskNumberOverride = expectedSubTaskNumberOverride;
  }
}

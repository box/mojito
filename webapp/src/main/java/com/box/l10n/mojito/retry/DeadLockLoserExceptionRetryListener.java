package com.box.l10n.mojito.retry;

import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.listener.RetryListenerSupport;
import org.springframework.stereotype.Component;

@Component
public class DeadLockLoserExceptionRetryListener extends RetryListenerSupport {

  static Logger logger = LoggerFactory.getLogger(DeadLockLoserExceptionRetryListener.class);

  MeterRegistry meterRegistry;

  public DeadLockLoserExceptionRetryListener(MeterRegistry meterRegistry) {
    this.meterRegistry = meterRegistry;
  }

  @Override
  public <T, E extends Throwable> boolean open(RetryContext context, RetryCallback<T, E> callback) {
    logger.debug("Attempting initial retry");
    meterRegistry.counter("DeadLockLoserExceptionRetryCounter").increment();
    return true;
  }

  @Override
  public <T, E extends Throwable> void onError(
      RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {
    logger.debug("Retry attempt failed, retrying again");
    meterRegistry.counter("DeadLockLoserExceptionRetryCounter").increment();
  }
}

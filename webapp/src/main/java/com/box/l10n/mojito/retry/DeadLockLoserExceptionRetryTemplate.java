package com.box.l10n.mojito.retry;

import com.google.common.collect.ImmutableMap;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DeadlockLoserDataAccessException;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.backoff.ExponentialRandomBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;

@Component
public class DeadLockLoserExceptionRetryTemplate {

  RetryTemplate retryTemplate;

  public DeadLockLoserExceptionRetryTemplate() {
    SimpleRetryPolicy retryPolicy =
        new SimpleRetryPolicy(
            5,
            ImmutableMap.of(
                DeadlockLoserDataAccessException.class,
                true,
                CannotAcquireLockException.class,
                true));

    ExponentialRandomBackOffPolicy exponentialRandomBackOffPolicy =
        new ExponentialRandomBackOffPolicy();
    exponentialRandomBackOffPolicy.setInitialInterval(10);
    exponentialRandomBackOffPolicy.setMultiplier(3);
    exponentialRandomBackOffPolicy.setMaxInterval(5000);

    retryTemplate = new RetryTemplate();
    retryTemplate.setRetryPolicy(retryPolicy);
    retryTemplate.setBackOffPolicy(exponentialRandomBackOffPolicy);
    retryTemplate.setThrowLastExceptionOnExhausted(true);
  }

  public <T, E extends Throwable> T execute(RetryCallback<T, E> retryCallback) throws E {
    return retryTemplate.execute(retryCallback);
  }
}

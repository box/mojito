package com.box.l10n.mojito.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.support.RetryTemplate;

/**
 * Simple aspect to retry.
 *
 * <p>Note: @{@link org.springframework.retry.annotation.Retryable} doesn't seem to work with
 * Aspectj CTW. This is over simplified to be really useful, would need more work and support
 * similar to @Retryable. Keep it as a base for potential future work and as a reminder
 * that @Retryable is not currently usable with AspectJ in this project.
 *
 * @author jeanaurambault
 */
@Aspect
public class RetryAspect {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(RetryAspect.class);

  @Autowired RetryTemplate retryTemplate;

  @Around("methods()")
  public Object withRetry(ProceedingJoinPoint pjp) throws Throwable {
    return retryTemplate.execute(
        context -> {
          return pjp.proceed();
        });
  }

  @Pointcut("execution(@com.box.l10n.mojito.aspect.Retry * *(..))")
  private void methods() {}
}

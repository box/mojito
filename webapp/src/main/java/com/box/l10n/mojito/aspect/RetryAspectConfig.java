package com.box.l10n.mojito.aspect;

import org.aspectj.lang.Aspects;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author jaurambault
 */
@Configuration
public class RetryAspectConfig {

  @Bean
  public RetryAspect getRetryAspect() {
    return Aspects.aspectOf(RetryAspect.class);
  }
}

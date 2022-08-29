package com.box.l10n.mojito.service.pollableTask;

import org.aspectj.lang.Aspects;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** @author aloison */
@Configuration
public class PollableAspectConfig {

  @Bean
  public PollableAspect getPollableAspect() {
    return Aspects.aspectOf(PollableAspect.class);
  }
}

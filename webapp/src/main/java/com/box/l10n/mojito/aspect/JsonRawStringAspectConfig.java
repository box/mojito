package com.box.l10n.mojito.aspect;

import org.aspectj.lang.Aspects;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** @author jaurambault */
@Configuration
public class JsonRawStringAspectConfig {

  @Bean
  public JsonRawStringAspect getJsonRawStringAspect() {
    return Aspects.aspectOf(JsonRawStringAspect.class);
  }
}

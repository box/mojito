package com.box.l10n.mojito.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.CommonsRequestLoggingFilter;

@Configuration
public class RequestLoggingConfig {

  @Value("${l10n.logging.requests.includesQueryString:false}")
  public boolean includesQueryString;

  @Value("${l10n.logging.requests.includesHeader:false}")
  public boolean includesHeader;

  @Value("${l10n.logging.requests.includesPayload:true}")
  public boolean includesPayload;

  @Value("${l10n.logging.requests.maxPayloadLength:10000}")
  public int maxPayloadLength;

  @Value("${l10n.logging.requests.beforeMessagePrefix:}")
  public String beforeMessagePrefix;

  @Value("${l10n.logging.requests.afterMessagePrefix:Request Data: }")
  public String afterMessagePrefix;

  @Bean
  @ConditionalOnProperty(value = "l10n.logging.requests.enabled", havingValue = "true")
  public CommonsRequestLoggingFilter logFilter() {
    CommonsRequestLoggingFilter filter = new CommonsRequestLoggingFilter();
    filter.setIncludeQueryString(includesQueryString);
    filter.setIncludePayload(includesPayload);
    filter.setMaxPayloadLength(maxPayloadLength);
    filter.setIncludeHeaders(includesHeader);
    filter.setBeforeMessagePrefix(beforeMessagePrefix);
    filter.setAfterMessagePrefix(afterMessagePrefix);
    return filter;
  }
}

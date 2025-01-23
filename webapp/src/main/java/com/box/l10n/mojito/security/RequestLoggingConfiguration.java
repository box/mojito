package com.box.l10n.mojito.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
public class RequestLoggingConfiguration {

  private final RequestLoggingConfig requestLoggingConfig;
  private final RequestLoggerHelper requestLoggerHelper;

  @Autowired
  public RequestLoggingConfiguration(
      RequestLoggingConfig requestLoggingConfig,
      @Autowired(required = false) RequestLoggerHelper requestLoggerHelper) {
    this.requestLoggingConfig = requestLoggingConfig;
    this.requestLoggerHelper = requestLoggerHelper;
  }

  @Bean
  @ConditionalOnProperty(name = "l10n.logging.requests.enabled", havingValue = "true")
  public FilterRegistrationBean<RequestLoggingFilter> loggingFilter() {
    FilterRegistrationBean<RequestLoggingFilter> registrationBean = new FilterRegistrationBean<>();

    registrationBean.setFilter(new RequestLoggingFilter(requestLoggerHelper, requestLoggingConfig));
    registrationBean.setOrder(Ordered.HIGHEST_PRECEDENCE); // Ensure it runs before auth filters
    return registrationBean;
  }
}

package com.box.l10n.mojito.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;

public class RequestLoggingFilter extends OncePerRequestFilter {

  private static final Logger logger = LoggerFactory.getLogger(RequestLoggingFilter.class);

  private final RequestLoggingConfig requestLoggingConfig;
  private final RequestLoggerHelper requestLoggerHelper;

  public RequestLoggingFilter(
      RequestLoggerHelper requestLoggerHelper, RequestLoggingConfig requestLoggingConfig) {
    this.requestLoggingConfig = requestLoggingConfig;
    this.requestLoggerHelper = requestLoggerHelper;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws jakarta.servlet.ServletException, IOException {

    if (requestLoggingConfig.isLoggingEnabled() && requestLoggerHelper != null) {
      String msg = requestLoggerHelper.summarizeRequest(request, requestLoggingConfig);
      logger.info(msg);
    }

    filterChain.doFilter(request, response);
  }
}

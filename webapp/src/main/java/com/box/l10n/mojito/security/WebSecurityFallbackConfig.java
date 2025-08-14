package com.box.l10n.mojito.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;

@EnableWebSecurity
@Configuration
public class WebSecurityFallbackConfig {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(WebSecurityFallbackConfig.class);

  @Bean
  @Order(99)
  SecurityFilterChain securityFallbackBlock(HttpSecurity http) throws Exception {
    WebSecurityJWTConfig.applyStatelessSharedConfig(http);
    HttpStatusEntryPoint httpStatusEntryPoint = new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED);
    http.exceptionHandling(
        e -> {
          e.authenticationEntryPoint(httpStatusEntryPoint);
        });
    return http.build();
  }
}

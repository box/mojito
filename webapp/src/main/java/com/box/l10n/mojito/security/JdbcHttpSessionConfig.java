package com.box.l10n.mojito.security;


import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.jdbc.config.annotation.web.http.EnableJdbcHttpSession;

@ConditionalOnProperty(value = "l10n.spring.session.store-type", havingValue = "jdbc")
@Configuration
@EnableJdbcHttpSession
public class JdbcHttpSessionConfig {
}

package com.box.l10n.mojito.test;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Configuration;

/**
 * When running test want to have access to properties, whether coming from properties, env variable
 * or some other param
 *
 * <p>For now, we use @EnableAutoConfiguration to enable that in Spring but it is also turning
 * auto-conf for a lot of things that are not needed. Ideally it should be replaced with just needed
 * configuration
 */
@Configuration
@EnableAutoConfiguration
public class TestWithEnableAutoConfiguration {}

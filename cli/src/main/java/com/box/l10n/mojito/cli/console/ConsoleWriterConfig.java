package com.box.l10n.mojito.cli.console;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Configuration for {@link ConsoleWriter}.
 *
 * @author jaurambault
 */
@Configuration
public class ConsoleWriterConfig {

    @Autowired
    ConsoleWriterConfigurationProperties consoleWriterConfigurationProperties;

    @Primary
    @Bean
    public ConsoleWriter consoleWriter() {
        ConsoleWriter consoleWriter = new ConsoleWriter(
                consoleWriterConfigurationProperties.isAnsiCodeEnabled(),
                consoleWriterConfigurationProperties.getOutputType());
        return consoleWriter;
    }

    @Bean(name = "ansiCodeEnabledFalse")
    public ConsoleWriter consoleWriterNoAnsi() {
        ConsoleWriter consoleWriter = new ConsoleWriter(
                false,
                consoleWriterConfigurationProperties.getOutputType());
        return consoleWriter;
    }

}

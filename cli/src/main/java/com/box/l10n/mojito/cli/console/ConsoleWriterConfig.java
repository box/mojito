package com.box.l10n.mojito.cli.console;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for {@link ConsoleWriter}.
 *
 * @author jaurambault
 */
@Configuration
public class ConsoleWriterConfig {

    @Autowired
    ConsoleWriterConfigurationProperties consoleWriterConfigurationProperties;

    @Bean
    public ConsoleWriter consoleWriter() {
        ConsoleWriter consoleWriter = new ConsoleWriter(
                consoleWriterConfigurationProperties.isAnsiCodeEnabled(),
                consoleWriterConfigurationProperties.getOutputType());
        return consoleWriter;
    }

}

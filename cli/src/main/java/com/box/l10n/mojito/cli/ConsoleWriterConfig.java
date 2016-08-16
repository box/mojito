package com.box.l10n.mojito.cli;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration for {@link ConsoleWriter}.
 *
 * @author jaurambault
 */
@Component
@ConfigurationProperties("l10n.consoleWriter")
public class ConsoleWriterConfig {

    /**
     * Configure the output type of ConsoleWritter
     */
    public enum OutputType {

        /**
         * Output in ANSI mode only to the logger (console is skipped).
         */
        ANSI_LOGGER,
        /**
         * Ouput in ANSI mode to the console and in plain mode to the logger.
         */
        ANSI_CONSOLE_AND_LOGGER
    };

    /**
     * To enable or disable printing of ANSI escape codes in the standard
     * output.
     */
    boolean ansiCodeEnabled = true;

    OutputType outputType = OutputType.ANSI_CONSOLE_AND_LOGGER;

    public boolean isAnsiCodeEnabled() {
        return ansiCodeEnabled;
    }

    public void setAnsiCodeEnabled(boolean ansiCodeEnabled) {
        this.ansiCodeEnabled = ansiCodeEnabled;
    }

    public OutputType getOutputType() {
        return outputType;
    }

    public void setOutputType(OutputType outputType) {
        this.outputType = outputType;
    }

}

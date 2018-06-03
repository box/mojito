package com.box.l10n.mojito.cli;

import com.box.l10n.mojito.cli.ConsoleWriterConfig.OutputType;
import com.google.common.base.CharMatcher;
import org.fusesource.jansi.Ansi;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Builds and prints messages with ANSI escape codes to standard output and
 * logger.
 *
 * Printing of ANSI escape codes can be disabled for standard output (Logger
 * messages never contain escape codes).
 *
 * @author jaurambault
 */
@Component
public class ConsoleWriter {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger("com.box.l10n.mojito.cli.application");
   
    @Autowired
    ConsoleWriterConfig consoleWriterConfig;
    
    /**
     * Contains the message without ANSI escape codes.
     */
    StringBuilder stringBuilder;

    /**
     * To generate the message with ANSI escape codes.
     */
    Ansi ansi;

    /**
     * The number of new lines in last printed string.
     */
    int numberOfNewLinesInLastPrintedString = 0;

    /**
     * Keep track of the number of new lines in current string to be printed.
     */
    int numberOfNewLinesInCurrentString = 0;

    public ConsoleWriter() {
        resetOutputBuilders();
    }

    /**
     * See {@link Ansi#fg(Ansi.Color) }
     *
     * @param color
     * @return this instance
     */
    public ConsoleWriter fg(Ansi.Color color) {
        if (consoleWriterConfig.isAnsiCodeEnabled()) {
            ansi.fg(color);
        }
        return this;
    }

    /**
     * See {@link Ansi#reset() }
     *
     * @return this instance
     */
    public ConsoleWriter reset() {
        if (consoleWriterConfig.isAnsiCodeEnabled()) {
            ansi.reset();
        }
        return this;
    }

    /**
     * See {@link Ansi#a() }
     *
     * @param value
     * @return this instance
     */
    public ConsoleWriter a(String value) {
        stringBuilder.append(value);
        ansi.a(value);
        return this;
    }

    /**
     * See {@link Ansi#a() }
     *
     * @param value
     * @return this instance
     */
    public ConsoleWriter a(DateTime value) {
        stringBuilder.append(value.toString());
        ansi.a(value);
        return this;
    }

    /**
     * See {@link Ansi#a() }
     *
     * @param value
     * @return this instance
     */
    public ConsoleWriter a(Integer value) {
        stringBuilder.append(value);
        ansi.a(value);
        return this;
    }
    
    /**
     * See {@link Ansi#a() }
     *
     * @param value
     * @return this instance
     */
    public ConsoleWriter a(Double value) {
        stringBuilder.append(value);
        ansi.a(value);
        return this;
    }

    /**
     * See {@link Ansi#a() }
     *
     * @param value
     * @return this instance
     */
    public ConsoleWriter a(Long value) {
        stringBuilder.append(value);
        ansi.a(value);
        return this;
    }

    /**
     * See {@link Ansi#a() }
     *
     * @param value
     * @return this instance
     */
    public ConsoleWriter a(StringBuilder value) {
        stringBuilder.append(value);
        ansi.a(value);
        return this;
    }

    /**
     * See {@link Ansi#a() }
     *
     * @param value
     * @return this instance
     */
    public ConsoleWriter a(Boolean value) {
        stringBuilder.append(value);
        ansi.a(value);
        return this;
    }

    /**
     * See {@link Ansi#newLine() }
     *
     * @return this instance
     */
    public ConsoleWriter newLine() {
        ++numberOfNewLinesInCurrentString;
        stringBuilder.append(System.getProperty("line.separator"));
        ansi.newline();
        return this;
    }

    /**
     * See {@link Ansi#newLine() }
     *
     * @param numberOfNewLines number of new lines to be added
     * @return this instance
     */
    public ConsoleWriter newLine(int numberOfNewLines) {

        for (int i = 0; i < numberOfNewLines; i++) {
            newLine();
        }
        return this;
    }

    /**
     * Prints the message to the standard output and to logger.
     *
     * @return this instance
     */
    public ConsoleWriter print() {
        return println(0);
    }

    /**
     * Appends a new line at the end of the message then prints the message to
     * the standard output and to logger.
     *
     * @return this instance
     */
    public ConsoleWriter println() {
        return println(1);
    }

    /**
     * Appends new lines at the end of the message then prints the message to
     * the standard output and to logger.
     *
     * @param numberOfNewLines number of new lines to append at then end of the
     * message.
     * @return this instance
     */
    public ConsoleWriter println(int numberOfNewLines) {

        reset();
        newLine(numberOfNewLines);

        numberOfNewLinesInLastPrintedString = numberOfNewLinesInCurrentString;
        numberOfNewLinesInCurrentString = 0;

        if (OutputType.ANSI_CONSOLE_AND_LOGGER.equals(consoleWriterConfig.getOutputType())) {
            System.out.print(ansi.toString());
            logger.info(trimReturnLine(stringBuilder.toString()));
        } else if (OutputType.ANSI_LOGGER.equals(consoleWriterConfig.getOutputType())) {
            logger.info(trimReturnLine(ansi.toString()));
        }

        resetOutputBuilders();
        return this;
    }

    /**
     * Trims the return line from the string
     *
     * @param string string to be trimmed
     * @return trimmed string
     */
    private String trimReturnLine(String string) {
        return CharMatcher.anyOf("\n").trimFrom(string);
    }

    /**
     * Reset both output builders.
     */
    private void resetOutputBuilders() {
        ansi = Ansi.ansi();
        stringBuilder = new StringBuilder();
    }

    /**
     * Erases the previous printed lines from the console.
     *
     * @return this instance
     */
    public ConsoleWriter erasePreviouslyPrintedLines() {

        if (consoleWriterConfig.isAnsiCodeEnabled()) {
            for (int i = 0; i < numberOfNewLinesInLastPrintedString; i++) {
                ansi.cursorUp(1);
                ansi.eraseLine();
            }
        }

        return this;
    }

}

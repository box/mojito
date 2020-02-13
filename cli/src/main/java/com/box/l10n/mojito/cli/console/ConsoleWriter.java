package com.box.l10n.mojito.cli.console;

import com.google.common.base.CharMatcher;
import org.fusesource.jansi.Ansi;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Builds and prints messages with ANSI escape codes to standard output and
 * logger.
 * <p>
 * Printing of ANSI escape codes can be disabled for standard output (Logger
 * messages never contain escape codes).
 *
 * @author jaurambault
 */
public class ConsoleWriter {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger("com.box.l10n.mojito.cli.application");

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
     * To enable or disable ainsi codes in the ainsi output
     */
    boolean isAnsiCodeEnabled;

    /**
     * To choose the output type
     */
    OutputType outputType;

    /**
     * Keep track of the number of new lines in current string to be printed.
     */
    int numberOfNewLinesInCurrentString = 0;

    public ConsoleWriter(boolean isAnsiCodeEnabled, OutputType outputType) {
        this.isAnsiCodeEnabled = isAnsiCodeEnabled;
        this.outputType = outputType;
        resetOutputBuilders();
    }

    /**
     * See {@link Ansi#fg(Ansi.Color) }
     *
     * @param color
     * @return this instance
     */
    public ConsoleWriter fg(Ansi.Color color) {
        if (isAnsiCodeEnabled) {
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
        if (isAnsiCodeEnabled) {
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
     *                         message.
     * @return this instance
     */
    public ConsoleWriter println(int numberOfNewLines) {

        reset();
        newLine(numberOfNewLines);

        numberOfNewLinesInLastPrintedString = numberOfNewLinesInCurrentString;
        numberOfNewLinesInCurrentString = 0;

        if (OutputType.ANSI_CONSOLE_AND_LOGGER.equals(outputType)) {
            System.out.print(ansi.toString());
            logger.info(trimReturnLine(stringBuilder.toString()));
        } else if (OutputType.ANSI_LOGGER.equals(outputType)) {
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

        if (isAnsiCodeEnabled) {
            for (int i = 0; i < numberOfNewLinesInLastPrintedString; i++) {
                ansi.cursorUp(1);
                ansi.eraseLine();
            }
        }

        return this;
    }

    /**
     * Types of output for the ConsoleWritter
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
    }
}

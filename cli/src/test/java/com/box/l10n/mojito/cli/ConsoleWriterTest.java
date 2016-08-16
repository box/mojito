package com.box.l10n.mojito.cli;

import java.io.IOException;
import org.fusesource.jansi.Ansi;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Rule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.OutputCapture;

/**
 *
 * @author jaurambault
 */
public class ConsoleWriterTest {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(ConsoleWriterTest.class);

    @Rule
    public OutputCapture outputCapture = new OutputCapture();

    @Test
    public void ansiCode() throws IOException {
        ConsoleWriter consoleWriter = new ConsoleWriter();
        consoleWriter.consoleWriterConfig = new ConsoleWriterConfig();
        consoleWriter.consoleWriterConfig.setAnsiCodeEnabled(true);

        consoleWriter.newLine(2).fg(Ansi.Color.CYAN).a("ansiCode: ").a(1L).println(3);

        assertTrue(outputCapture.toString().contains("[36mansiCode: 1"));

        assertTrue(outputCapture.toString().contains("ansiCode: 1"));
    }

    @Test
    public void disableAnsiCode() {
        ConsoleWriter consoleWriter = new ConsoleWriter();
        consoleWriter.consoleWriterConfig = new ConsoleWriterConfig();
        consoleWriter.consoleWriterConfig.setAnsiCodeEnabled(false);

        consoleWriter.newLine(2).fg(Ansi.Color.CYAN).a("disableAnsiCode: ").a(1L).println(3);

        assertFalse(outputCapture.toString().contains("[36mdisableAnsiCode: 1"));

        assertTrue(outputCapture.toString().contains("\n"
                + "\n"
                + "disableAnsiCode: 1\n"
                + "\n"
                + "\n"
                + ""));
    }

}

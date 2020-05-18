package com.box.l10n.mojito.cli.command;

import com.box.l10n.mojito.cli.console.ConsoleWriter;
import com.box.l10n.mojito.rest.client.WaitForPollableTaskListener;
import com.box.l10n.mojito.rest.entity.PollableTask;
import com.google.common.base.Strings;
import org.fusesource.jansi.Ansi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

/**
 * Listener that displays the status of {@link PollableTask}.
 *
 * @author jaurambault
 */
@Configurable
public class CommandWaitForPollableTaskListener implements WaitForPollableTaskListener {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(CommandWaitForPollableTaskListener.class);

    @Autowired
    ConsoleWriter consoleWriter;

    /**
     * Keeps track of the number of callback to {@link #afterPoll(com.box.l10n.mojito.rest.entity.PollableTask)
     * }
     */
    int numberAfterPollCallback = 0;

    @Override
    public void afterPoll(PollableTask pollableTask) {

        if (numberAfterPollCallback++ > 0) {
            consoleWriter.erasePreviouslyPrintedLines();
        }

        printPollableTaskMessages(pollableTask, 0);

        consoleWriter.print();
    }

    /**
     * Recursively prints messages of a {@link PollableTask} and its sub tasks.
     *
     * <p>
     * The indentation level is increased when a {@link PollableTask} contains a
     * non-empty message (as no message is printed in that case, it is not
     * needed indent sub task messages).
      *
     * @param pollableTask contains the messages to be printed
     * @param indentationLevel indentation level used to determine the number of
     * spaces used to indent the printed message
     */
    void printPollableTaskMessages(PollableTask pollableTask, int indentationLevel) {

        int newIndentationLevel = indentationLevel;

        if (!Strings.isNullOrEmpty(pollableTask.getMessage())) {

            String linePrefix = getLinePrefix(indentationLevel);

            consoleWriter.a(linePrefix).a(pollableTask.getMessage()).fg(Ansi.Color.MAGENTA).a(" (").a(pollableTask.getId()).a(") ");

            if (pollableTask.getErrorMessage() != null) {
                if (!pollableTask.getErrorMessage().isExpected()) {
                    consoleWriter.fg(Ansi.Color.RED).a("Failed").newLine().a(linePrefix).a(pollableTask.getErrorMessage().getMessage());
                } else {
                    consoleWriter.fg(Ansi.Color.RED)
                            .a("Failed").newLine()
                            .a(linePrefix).a("An unexpected error happened, task=" + pollableTask.getId()).newLine()
                            .a(linePrefix).a(pollableTask.getErrorMessage().getType()).newLine()
                            .a(linePrefix).a(pollableTask.getErrorMessage().getMessage());
                }
            } else if (pollableTask.getFinishedDate() != null) {
                consoleWriter.fg(Ansi.Color.GREEN).a("Done");
            } else {
                consoleWriter.fg(Ansi.Color.YELLOW).a("Running");
            }

            consoleWriter.reset().newLine();

            newIndentationLevel += 1;
        }

        for (PollableTask subTask : pollableTask.getSubTasks()) {
            printPollableTaskMessages(subTask, newIndentationLevel);
        }
    }

    /**
     * Builds the line prefix to be used for displaying a message based on the
     * indentation level.
     *
     * @param indentationLevel indentation level used to determine the number of
     * spaces used to indent the printed message
     * @return the line prefix
     */
    private String getLinePrefix(int indentationLevel) {
        StringBuilder sb = new StringBuilder();

        for (int numSpaces = 0; numSpaces < indentationLevel * 2; numSpaces++) {
            sb.append(" ");
        }

        return sb.toString();
    }

}

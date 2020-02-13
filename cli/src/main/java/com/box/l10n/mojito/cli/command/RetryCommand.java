package com.box.l10n.mojito.cli.command;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.box.l10n.mojito.cli.console.ConsoleWriter;
import org.fusesource.jansi.Ansi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.util.backoff.BackOffExecution;
import org.springframework.util.backoff.ExponentialBackOff;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Command to execute shell command with retry and exponential back off. Can
 * optional tell in which directory the command should be executed.
 *
 * @author jaurambault
 */
@Component
@Scope("prototype")
@Parameters(commandNames = {"retry"}, commandDescription = "Execute a shell command with retry")
public class RetryCommand extends Command {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(RetryCommand.class);

    static final String COMMAND_SHORT = "-c";
    static final String COMMAND_LONG = "--command";

    @Autowired
    ConsoleWriter consoleWriter;

    @Parameter(names = {"-d", "--directory"}, arity = 1, required = false, description = "The directory in which to execute the command")
    String directory;

    @Parameter(names = {"-a", "--attempts"}, arity = 1, required = false, description = "The directory in which to execute the command")
    int maxNumberOfAttempts = 3;

    @Parameter(names = {"-i", "--initial-interval"}, arity = 1, required = false, description = "Initial interval for the back off policy in milliseconds")
    long initialInterval = 2000L;

    @Parameter(names = {"-m", "--multiplier"}, arity = 1, required = false, description = "Multiplier interval for the back off policy")
    double multiplier = 1.5;

    @Parameter(names = {COMMAND_SHORT, COMMAND_LONG}, required = true, description
            = "To specify the command to execute. Anything coming after this option will constitue the command to run. Other options must be placed before.")
    Boolean command;

    /**
     * For better error message from JCommander
     */
    @Parameter
    List<String> parameters;

    @Override
    public void execute() throws CommandException {

        List<String> commandToExecute = getCommandToExecute();

        ExponentialBackOff exponentialBackOff = new ExponentialBackOff(initialInterval, multiplier);
        BackOffExecution backOffExecution = exponentialBackOff.start();

        int numberOfAttempts = 0;
        int exitCode = -1;

        while (exitCode !=0 && numberOfAttempts < maxNumberOfAttempts) {
            try {
                numberOfAttempts++;
                exitCode = executeCommand(commandToExecute);

                if (exitCode != 0) {
                    consoleWriter.a("Attempt: ").fg(Ansi.Color.CYAN)
                            .a(numberOfAttempts).a("/").a(maxNumberOfAttempts).reset()
                            .a(" failed. ");
                            
                    if (numberOfAttempts < maxNumberOfAttempts) {
                        long nextBackOff = backOffExecution.nextBackOff();
                        consoleWriter.a("Retry in ").fg(Ansi.Color.CYAN).a(nextBackOff / 1000.0).reset().a(" seconds").println();
                        Thread.sleep(nextBackOff);
                    } else {
                        consoleWriter.a("Abort").println();
                    }
                }
            } catch (CommandException ce) {
                throw ce;
            } catch (Throwable t) {
                throw new CommandException("Couldn't run command with retry", t);
            }
        }

        if (exitCode != 0) {
            throw new CommandWithExitStatusException(exitCode);
        }
    }

    int executeCommand(List<String> commandToExecute) throws CommandException {
        ProcessBuilder builder = new ProcessBuilder();
        builder.command(commandToExecute).inheritIO();
        if (directory != null) {
            builder.directory(new File(directory));
        }

        Process process;
        try {
            process = builder.start();
        } catch (IOException ioe) {
            throw new CommandException(ioe.getMessage());
        }

        try {
            return process.waitFor();
        } catch (InterruptedException ie) {
            throw new CommandException("Error waiting for the process to finsih", ie);
        }
    }

    @Override
    public boolean shouldShowUsage() {
        int indexOfCommandParameter = getIndexOfCommandParameter();
        int indexOfHelpParameter = getIndexOfHelpParameter();
        return (indexOfHelpParameter >= 0 && (indexOfHelpParameter < indexOfCommandParameter || indexOfCommandParameter == -1));
    }

    @Override
    void showUsage() {
        if (getIndexOfCommandParameter() == -1) {
            consoleWriter.newLine().fg(Ansi.Color.RED).a("The following option is required: ").a(COMMAND_SHORT).a(",").a(COMMAND_LONG).println(2);
        }
        super.showUsage();
    }

    List<String> getCommandToExecute() {
        int indexOfCommandParameter = getIndexOfCommandParameter();
        List<String> commandToExecute = originalArgs.subList(indexOfCommandParameter + 1, originalArgs.size());
        return commandToExecute;
    }

    int getIndexOfCommandParameter() {
        int indexOf = originalArgs.indexOf(COMMAND_SHORT);
        if (indexOf == -1) {
            indexOf = originalArgs.indexOf(COMMAND_LONG);
        }

        return indexOf;
    }

    int getIndexOfHelpParameter() {
        int indexOf = originalArgs.indexOf(HELP_SHORT);
        if (indexOf == -1) {
            indexOf = originalArgs.indexOf(HELP_LONG);
        }
        return indexOf;
    }

}

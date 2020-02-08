package com.box.l10n.mojito.cli.command;

import com.beust.jcommander.JCommander;
import com.box.l10n.mojito.cli.ConsoleWriter;
import com.box.l10n.mojito.rest.resttemplate.AuthenticatedRestTemplate;
import com.google.common.base.Strings;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.fusesource.jansi.Ansi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.context.ApplicationContext;
import org.springframework.security.web.authentication.session.SessionAuthenticationException;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

/**
 * Wrapper around {@link JCommander} to provide CLI parsing, {@link Command}
 * registration and dispatching.
 *
 * <p>
 * Note the use of a wrapper {@link #jCommander} to allow multiple sub-sequent
 * {@link #run(java.lang.String[])}. See also {@link #createJCommanderForRun() }
 *
 * @author jaurambault
 */
@Configurable
public class L10nJCommander {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(L10nJCommander.class);

    @Autowired
    ConsoleWriter consoleWriter;

    @Autowired
    ApplicationContext applicationContext;

    @Autowired
    MainCommand mainCommand;

    @Autowired
    AuthenticatedRestTemplate authenticatedRestTemplate;

    static final String PROGRAM_NAME = "mojito";

    boolean systemExitEnabled = true;

    int exitCode;

    /**
     * Wrapped {@link JCommander} that is re-created for each call of
     * {@link #run}.
     */
    JCommander jCommander;

    /**
     * Map of commands keyed by their first name (by convention it should be the
     * long name).
     */
    Map<String, Command> commands = new TreeMap<>();

    /**
     * Gets a {@link Command} for a given command name. The name must be the
     * first name specified for the command, see {@link Command#getName() }.
     *
     * @param parsedCommandName command name (null is treated as an empty
     * string, ie. maps to the {@link MainCommand)
     * @return {@link Command} for the given name or the main command.
     */
    public Command getCommand(String parsedCommandName) {
        return commands.get(Strings.nullToEmpty(parsedCommandName));
    }

    /**
     * Gets a {@link Command} for a given type.
     *
     * @param <T> type of the command
     * @param clazz class of the command
     * @return the command
     */
    public <T extends Command> T getCommand(Class<T> clazz) {

        T command = null;

        for (Command candidate : commands.values()) {
            if (candidate.getClass().isAssignableFrom(clazz)) {
                command = (T) candidate;
            }
        }

        return command;
    }

    @PostConstruct
    public void init() {
        createJCommanderForRun();
    }

    public void run(String... args) {

        Exception parsingException = null;

        try {
            logger.debug("Parse arguments");
            jCommander.parse(args);
        } catch (Exception e) {
            parsingException = e;
            logger.debug("Parsing failed", e);
        }

        String parsedCommand = jCommander.getParsedCommand();

        if (parsingException != null) {
            logger.debug("Parsing failed, show help");
            printErrorMessage(parsingException.getMessage());

            if (parsedCommand == null) {
                usage();
            } else {
                usage(parsedCommand);
            }
        } else {
            logger.debug("Execute commands for parsed command: {}", parsedCommand);
            Command command = getCommand(parsedCommand);
            command.setOriginalArgs(Arrays.asList(args));

            try {
                command.run();
            } catch (SessionAuthenticationException ae) {
                logger.debug("Exit with Invalid username or password", ae);
                printErrorMessage("Invalid username or password");
                exitWithError();
            } catch (CommandWithExitStatusException cwese) {
                logger.error("Exit with error for command: " + command.getName(), cwese);
                exitWithError(cwese.getExitCode());
            } catch (CommandException ce) {
                printErrorMessage(ce.getMessage());
                logger.error("Exit with error for command: " + command.getName(), ce);
                exitWithError();
            } catch (ResourceAccessException rae) {
                String msg = "Is a server running on: " + authenticatedRestTemplate.getURIForResource("") + "?";
                printErrorMessage(msg);
                logger.error(msg, rae);
                exitWithError();
            } catch (HttpClientErrorException | HttpServerErrorException e) {
                String msg = "Unexpected error: " + e.getMessage() + "\n" + ExceptionUtils.getStackTrace(e) + "\n" +
                        e.getResponseBodyAsString();
                printErrorMessage(msg);
                logger.error("Unexpected error", e);
                logger.error(e.getResponseBodyAsString());
                exitWithError();
            } catch (Throwable t) {
                String msg = "Unexpected error: " + t.getMessage() + "\n" + ExceptionUtils.getStackTrace(t);
                printErrorMessage(msg);
                logger.error("Unexpected error", t);
                exitWithError();
            }
        }
    }

    /**
     * Display the usage in the appLogger (instead of System.out).
     */
    public void usage() {

        consoleWriter.a("Usage: ").a(PROGRAM_NAME).fg(Ansi.Color.CYAN).a(" <command>").reset().a(" [options]").println(2);
        consoleWriter.a("Commands:").println();

        for (Command command : commands.values()) {

            if (!command.getName().isEmpty()) {
                String paddedCommandName = Strings.padStart(command.getName(), 20, ' ');
                consoleWriter.fg(Ansi.Color.CYAN).a(paddedCommandName).reset().a(" ").a(command.getDescription()).println();
            }
        }

        consoleWriter.newLine().a("Get help for a specific command: mojito <command> -h").println(2);
    }

    /**
     * Display the command usage in the appLogger (instead of System.out).
     *
     * @param commandName the command name
     */
    public void usage(String commandName) {
        StringBuilder stringBuilder = new StringBuilder();
        jCommander.usage(commandName, stringBuilder, "");
        consoleWriter.a(stringBuilder).println();
    }

    /**
     * Creates a {@link JCommander} instance for a single run (sub-sequent
     * parsing are not supported but required for testing).
     */
    public void createJCommanderForRun() {
        logger.debug("Create JCommander instance");
        jCommander = new JCommander();

        jCommander.setAcceptUnknownOptions(true);

        logger.debug("Initialize the JCommander instance");
        jCommander.setProgramName(PROGRAM_NAME);

        logger.debug("Set the main command for version/help directly on the JCommander");
        jCommander.addObject(mainCommand);

        logger.debug("Register Commands retreived using Spring");
        for (Command command : applicationContext.getBeansOfType(Command.class).values()) {

            Map<String, JCommander> jCommands = jCommander.getCommands();

            for (String name : command.getNames()) {
                if (jCommands.keySet().contains(name)) {
                    throw new RuntimeException("There must be only one module with name: " + name);
                }
            }

            commands.put(command.getName(), command);
            jCommander.addCommand(command);
        }
    }
    
    public void exitWithError() {
        exitWithError(1);
    }

    public void exitWithError(int exitCode) {
        this.exitCode = exitCode;

        if (isSystemExitEnabled()) {
            System.exit(exitCode);
        } else {
            logger.info("System exit disabled");
        }
    }

    /**
     * Prints the error message with ANSI escape codee.
     *
     * @param errorMsg the error message
     */
    private void printErrorMessage(String errorMsg) {
        consoleWriter.newLine().fg(Ansi.Color.RED).a(errorMsg).println(2);
    }

    public boolean isSystemExitEnabled() {
        return systemExitEnabled;
    }

    public void setSystemExitEnabled(boolean systemExitEnabled) {
        this.systemExitEnabled = systemExitEnabled;
    }

    public int getExitCode() {
        return exitCode;
    }
}

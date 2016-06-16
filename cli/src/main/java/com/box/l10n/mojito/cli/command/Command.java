package com.box.l10n.mojito.cli.command;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.google.common.base.Preconditions;
import java.util.Arrays;
import java.util.List;

/**
 * Base class for Commands, provides basic support for usage display.
 *
 * @author jaurambault
 */
public abstract class Command {

    @Parameter(names = {"--help", "-h"}, help = true, description = "Show help")
    private boolean help;

    /**
     * Method to be overridden to implement the business logic of this command
     *
     * @throws CommandException
     */
    protected abstract void execute() throws CommandException;

    /**
     * Runs the command. Implements usage display or and calls {@link #execute()
     * }
     * which actually contains the business logic of the command.
     *
     * @throws CommandException
     */
    public void run() throws CommandException {
        if (help) {
            showUsage();
        } else {
            execute();
        }
    }

    /**
     * Shows the command usage.
     */
    void showUsage() {
        new L10nJCommander().usage(getName());
    }

    /**
     * Gets the {@link Parameters} annotation from the command.
     * 
     * @return  the {@link Parameters} annotation
     */
    Parameters getParameters() {
        Parameters parameters = this.getClass().getAnnotation(Parameters.class);
        Preconditions.checkNotNull(parameters, "There must be @Parameters on the Command class: " + this.getClass());
        return parameters;
    }

    /**
     * Gets the command description from the {@link Parameters} annotation.
     * 
     * @return the command description 
     */
    public String getDescription() {
        return getParameters().commandDescription();
    }

    /**
     * Gets the names of this command (should be long name first followed by
     * short name).
     *
     * @return list of command names
     */
    public List<String> getNames() {
        Parameters parameters = getParameters();
        String[] commandNames = parameters.commandNames();
        return Arrays.asList(commandNames);
    }

    /**
     * Gets the first name of this command (should be the long name).
     *
     * @return name of this command
     */
    public String getName() {
        List<String> names = getNames();

        if (names.isEmpty()) {
            throw new RuntimeException("A command must have a name see @Parameters");
        }

        return names.get(0);
    }

}

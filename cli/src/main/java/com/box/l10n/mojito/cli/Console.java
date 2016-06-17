package com.box.l10n.mojito.cli;

import com.box.l10n.mojito.cli.command.CommandException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.springframework.stereotype.Component;

/**
 * Wrapper around {@link java.io.Console} (helpful for for mocking) and provides
 * helpers.
 *
 * @author jaurambault
 */
@Component
public class Console {

    /**
     * Gets the system console.
     *
     * @return the system console
     * @throws CommandException if system console is not available
     */
    private java.io.Console getSystemConsole() throws CommandException {

        java.io.Console systemConsole = System.console();

        if (systemConsole == null) {
            throw new CommandException("System console must be availalbe");
        }

        return systemConsole;
    }

    /**
     * Wraps {@link java.io.Console#readLine() } (can be used for mocking).
     *
     * @return A string containing the line read from the console, not including
     * any line-termination characters, or <tt>null</tt>
     * if an end of stream has been reached.
     * @throws CommandException if system console is not available
     */
    public String readLine() throws CommandException {
        return getSystemConsole().readLine();
    }

    /**
     * Reads a line from the system console, casts it to specified type and
     * returned the casted value.
     *
     * @param <T> The type of object to be returned
     * @param clazz The class of object to be returned
     * @return the line converted in specified type
     * @throws CommandException if it was not possible to read the input or cast
     * it to specified type.
     */
    public <T> T readLine(Class<T> clazz) throws CommandException {

        String readLine = readLine().trim();

        Method valueOfMethod = null;

        try {
            valueOfMethod = clazz.getMethod("valueOf", String.class);
        } catch (NoSuchMethodException nsme) {
            throw new RuntimeException("Type (" + clazz.getName() + ") must implement valueOf with single argument of type String", nsme);
        }

        try {
            return (T) valueOfMethod.invoke(null, readLine);
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            throw new CommandException("Invalid input", e);
        }
    }

    /**
     * Reads a password from the system console.
     *
     * @return the password
     * @throws CommandException if system console is not available
     */
    public String readPassword() throws CommandException {
        char[] readPassword = getSystemConsole().readPassword();
        return new String(readPassword);
    }

}

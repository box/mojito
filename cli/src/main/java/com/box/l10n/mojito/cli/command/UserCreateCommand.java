package com.box.l10n.mojito.cli.command;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.box.l10n.mojito.cli.Console;
import com.box.l10n.mojito.cli.ConsoleWriter;
import com.box.l10n.mojito.cli.command.param.Param;
import com.box.l10n.mojito.rest.client.UserClient;
import com.box.l10n.mojito.rest.client.exception.ResourceNotCreatedException;
import com.box.l10n.mojito.rest.entity.Role;
import com.box.l10n.mojito.rest.entity.User;
import org.fusesource.jansi.Ansi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Command to add a user
 * 
 * @author jyi
 */
@Component
@Scope("prototype")
@Parameters(commandNames = {"user-create"}, commandDescription = "Creates a user")
public class UserCreateCommand extends Command {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(UserCreateCommand.class);
    
    @Autowired
    ConsoleWriter consoleWriter;
    
    @Autowired
    UserClient userClient;
    
    @Parameter(names = {Param.USERNAME_LONG, Param.USERNAME_SHORT}, arity = 1, required = true, description = Param.USERNAME_DESCRIPTION)
    String username;
 
    @Parameter(names = {Param.ROLE_LONG, Param.ROLE_SHORT}, arity = 1, required = false, description = Param.ROLE_DESCRIPTION)
    String rolename;
    
    @Parameter(names = {Param.SURNAME_LONG, Param.SURNAME_SHORT}, arity = 1, required = true, description = Param.SURNAME_DESCRIPTION)
    String surname;
    
    @Parameter(names = {Param.GIVEN_NAME_LONG, Param.GIVEN_NAME_SHORT}, arity = 1, required = true, description = Param.GIVEN_NAME_DESCRIPTION)
    String givenName;
    
    @Parameter(names = {Param.COMMON_NAME_LONG, Param.COMMON_NAME_SHORT}, arity = 1, required = true, description = Param.COMMON_NAME_DESCRIPTION)
    String commonName;
       
    @Autowired
    Console console;
    
    @Override
    protected void execute() throws CommandException {
        consoleWriter.a("Create user: ").fg(Ansi.Color.CYAN).a(username).println();

        try {
            consoleWriter.a("Enter user password: ").println();
            String password = console.readPassword();
            
            Role role = rolename == null ? null : Role.valueOf(rolename);
            User user = userClient.createUser(username, password, role, surname, givenName, commonName);
            consoleWriter.newLine().a("created --> user: ").fg(Ansi.Color.MAGENTA).a(user.getUsername()).println();
        } catch (ResourceNotCreatedException ex) {
            throw new CommandException("Error creating user: " + username, ex);
        }
    }
    
}

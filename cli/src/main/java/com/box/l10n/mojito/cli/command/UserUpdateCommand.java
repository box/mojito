package com.box.l10n.mojito.cli.command;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.box.l10n.mojito.cli.command.param.Param;
import com.box.l10n.mojito.cli.console.Console;
import com.box.l10n.mojito.cli.console.ConsoleWriter;
import com.box.l10n.mojito.rest.client.UserClient;
import com.box.l10n.mojito.rest.client.exception.ResourceNotFoundException;
import com.box.l10n.mojito.rest.entity.Role;
import org.fusesource.jansi.Ansi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Command to update existing user
 *
 * @author jyi
 */
@Component
@Scope("prototype")
@Parameters(
    commandNames = {"user-update"},
    commandDescription = "Updates a user")
public class UserUpdateCommand extends Command {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(UserUpdateCommand.class);

  @Autowired ConsoleWriter consoleWriter;

  @Autowired UserClient userClient;

  @Parameter(
      names = {Param.USERNAME_LONG, Param.USERNAME_SHORT},
      arity = 1,
      required = true,
      description = Param.USERNAME_DESCRIPTION)
  String username;

  @Parameter(
      names = {Param.PASSWORD_LONG, Param.PASSWORD_SHORT},
      required = false,
      description = Param.PASSWORD_DESCRIPTION)
  boolean passwordPrompt = false;

  @Parameter(
      names = {Param.ROLE_LONG, Param.ROLE_SHORT},
      arity = 1,
      required = false,
      description = Param.ROLE_DESCRIPTION)
  String rolename;

  @Parameter(
      names = {Param.SURNAME_LONG, Param.SURNAME_SHORT},
      arity = 1,
      required = false,
      description = Param.SURNAME_DESCRIPTION)
  String surname;

  @Parameter(
      names = {Param.GIVEN_NAME_LONG, Param.GIVEN_NAME_SHORT},
      arity = 1,
      required = false,
      description = Param.GIVEN_NAME_DESCRIPTION)
  String givenName;

  @Parameter(
      names = {Param.COMMON_NAME_LONG, Param.COMMON_NAME_SHORT},
      arity = 1,
      required = false,
      description = Param.COMMON_NAME_DESCRIPTION)
  String commonName;

  @Autowired Console console;

  @Override
  protected void execute() throws CommandException {
    consoleWriter.a("Update user: ").fg(Ansi.Color.CYAN).a(username).println();

    try {
      Role role = rolename == null ? null : Role.valueOf(rolename);
      userClient.updateUserByUsername(
          username, getPassword(), role, surname, givenName, commonName);
      consoleWriter.newLine().a("updated --> user: ").fg(Ansi.Color.MAGENTA).a(username).println();
    } catch (ResourceNotFoundException ex) {
      throw new CommandException(ex.getMessage(), ex);
    }
  }

  /**
   * Read password from the console if password param was passed
   *
   * @return the password if password param was passed else {@code null}.
   * @throws CommandException
   */
  String getPassword() throws CommandException {
    String password = null;

    if (passwordPrompt) {
      consoleWriter.a("Enter new password for " + username + ":").println();
      password = console.readPassword();
    }

    return password;
  }
}

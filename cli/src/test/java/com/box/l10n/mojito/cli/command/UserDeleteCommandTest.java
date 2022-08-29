package com.box.l10n.mojito.cli.command;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.box.l10n.mojito.cli.CLITestBase;
import com.box.l10n.mojito.cli.command.param.Param;
import com.box.l10n.mojito.entity.security.user.User;
import com.box.l10n.mojito.security.Role;
import com.box.l10n.mojito.service.security.user.UserRepository;
import com.box.l10n.mojito.service.security.user.UserService;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/** @author jyi */
public class UserDeleteCommandTest extends CLITestBase {
  /** logger */
  static Logger logger = LoggerFactory.getLogger(UserDeleteCommandTest.class);

  @Autowired UserRepository userRepository;

  @Autowired UserService userService;

  @Test
  public void testDelete() {
    String username = testIdWatcher.getEntityName("user");
    String password = "test";
    String surname = "Mojito";
    String givenName = "Test";
    String commonName = "Test Mojito";

    userService.createUserWithRole(
        username, password, Role.USER, givenName, surname, commonName, false);
    User user = userRepository.findByUsername(username);
    assertNotNull(user);

    getL10nJCommander().run("user-delete", Param.USERNAME_SHORT, username);
    assertTrue(
        "User is not deleted successfully", outputCapture.toString().contains("deleted --> user:"));
  }

  @Test
  public void testDeleteNonExistingUser() {
    String username = testIdWatcher.getEntityName("nonExistingUser");
    getL10nJCommander().run("user-delete", Param.USERNAME_SHORT, username);
    assertTrue(
        outputCapture.toString().contains("User with username [" + username + "] is not found"));
  }
}

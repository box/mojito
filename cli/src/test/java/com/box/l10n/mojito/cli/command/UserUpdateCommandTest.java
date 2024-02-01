package com.box.l10n.mojito.cli.command;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.box.l10n.mojito.cli.CLITestBase;
import com.box.l10n.mojito.cli.command.param.Param;
import com.box.l10n.mojito.cli.console.Console;
import com.box.l10n.mojito.entity.security.user.User;
import com.box.l10n.mojito.security.Role;
import com.box.l10n.mojito.service.security.user.UserRepository;
import com.box.l10n.mojito.service.security.user.UserService;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author jyi
 */
public class UserUpdateCommandTest extends CLITestBase {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(UserUpdateCommandTest.class);

  @Autowired UserRepository userRepository;

  @Autowired UserService userService;

  @Test
  public void testUpdateUserNames() throws Exception {
    String username = testIdWatcher.getEntityName("user");
    User user = createTestUserUsingUserService(username, null);

    String givenName = user.getGivenName() + "_updated";
    String surname = user.getSurname() + "_updated";
    String commonName = user.getCommonName() + " Updated";

    getL10nJCommander()
        .run(
            "user-update",
            Param.USERNAME_SHORT,
            user.getUsername(),
            Param.GIVEN_NAME_SHORT,
            givenName,
            Param.SURNAME_SHORT,
            surname,
            Param.COMMON_NAME_SHORT,
            commonName);
    assertTrue(outputCapture.toString().contains("updated --> user: "));

    User updatedUser = userRepository.findByUsername(username);
    assertEquals(user.getId(), updatedUser.getId());
    assertEquals(user.getUsername(), updatedUser.getUsername());
    assertEquals(
        user.getAuthorities().iterator().next().getAuthority(),
        updatedUser.getAuthorities().iterator().next().getAuthority());
    assertEquals(givenName, updatedUser.getGivenName());
    assertEquals(surname, updatedUser.getSurname());
    assertEquals(commonName, updatedUser.getCommonName());
  }

  @Test
  public void testUpdateUserRole() throws Exception {
    String role = "PM";
    String username = testIdWatcher.getEntityName("user");
    User user = createTestUserUsingUserService(username, null);
    assertFalse(user.getAuthorities().iterator().next().getAuthority().contains(role));

    getL10nJCommander()
        .run("user-update", Param.USERNAME_SHORT, user.getUsername(), Param.ROLE_SHORT, role);
    assertTrue(outputCapture.toString().contains("updated --> user: "));

    User updatedUser = userRepository.findByUsername(username);
    assertEquals(user.getId(), updatedUser.getId());
    assertEquals(user.getUsername(), updatedUser.getUsername());
    assertEquals(user.getGivenName(), updatedUser.getGivenName());
    assertEquals(user.getSurname(), updatedUser.getSurname());
    assertEquals(user.getCommonName(), updatedUser.getCommonName());
    assertTrue(updatedUser.getAuthorities().iterator().next().getAuthority().contains(role));
  }

  @Test
  public void testUpdateUserPassword() throws Exception {
    String username = testIdWatcher.getEntityName("user");
    User user = createTestUserUsingUserService(username, null);

    logger.debug("Mocking the console input for password");
    Console mockConsole = mock(Console.class);
    when(mockConsole.readPassword())
        .thenAnswer(
            new Answer<String>() {
              @Override
              public String answer(InvocationOnMock invocation) throws Throwable {
                return "test";
              }
            });

    L10nJCommander l10nJCommander = getL10nJCommander();
    UserUpdateCommand userUpdateCommand = l10nJCommander.getCommand(UserUpdateCommand.class);
    userUpdateCommand.console = mockConsole;

    l10nJCommander.run("user-update", Param.USERNAME_SHORT, user.getUsername());
    assertTrue(outputCapture.toString().contains("updated --> user: "));

    User updatedUser = userRepository.findByUsername(username);
    assertEquals(user.getId(), updatedUser.getId());
    assertEquals(user.getUsername(), updatedUser.getUsername());
    assertEquals(
        user.getAuthorities().iterator().next().getAuthority(),
        updatedUser.getAuthorities().iterator().next().getAuthority());
    assertEquals(user.getGivenName(), updatedUser.getGivenName());
    assertEquals(user.getSurname(), updatedUser.getSurname());
    assertEquals(user.getCommonName(), updatedUser.getCommonName());
  }

  private User createTestUserUsingUserService(String username, String rolename) {
    String password = "test";
    String surname = "Mojito";
    String givenName = "Test";
    String commonName = "Test Mojito " + username;

    Role role = Role.USER;
    if (rolename != null) {
      role = Role.valueOf(rolename);
    }
    User user =
        userService.createUserWithRole(
            username, password, role, givenName, surname, commonName, false);
    return user;
  }
}

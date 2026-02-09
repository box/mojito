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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author jyi
 */
public class UserCreateCommandTest extends CLITestBase {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(UserCreateCommandTest.class);

  @Autowired UserRepository userRepository;

  @Test
  public void testCreateUserWithDefaultRole() throws Exception {

    String username = testIdWatcher.getEntityName("user");
    String commonName = createTestUser(username, null);

    User user = userRepository.findByUsername(username);
    assertEquals(commonName, user.getCommonName());
    assertTrue(
        user.getAuthorities().iterator().next().getAuthority().contains(Role.ROLE_USER.toString()));
  }

  @Test
  public void testCreateUserWithRole() throws Exception {

    String username = testIdWatcher.getEntityName("user");
    String role = "ADMIN";
    String commonName = createTestUser(username, role);

    User user = userRepository.findByUsername(username);
    assertEquals(commonName, user.getCommonName());
    assertTrue(user.getAuthorities().iterator().next().getAuthority().contains(role));
  }

  @Test
  public void testCreateUserWithDuplicatedUsername() throws Exception {
    String username = testIdWatcher.getEntityName("user");
    createTestUser(username, null);

    createTestUser(username, null);
    assertTrue(
        outputCapture.toString().contains("User with username [" + username + "] already exists"));
  }

  @Test
  public void testCreateUserWithGeneratedPassword() throws Exception {

    String username = testIdWatcher.getEntityName("user");
    String commonName = createTestUser(username, null, null, true);

    User user = userRepository.findByUsername(username);
    assertEquals(commonName, user.getCommonName());
    assertTrue(outputCapture.toString().contains("Generated password for " + username + ":"));
  }

  @Test
  public void testCreateTranslatorWithLocales() throws Exception {

    String username = testIdWatcher.getEntityName("user");
    List<String> locales = Arrays.asList("fr-FR", "ja-JP");
    String commonName = createTestUser(username, "TRANSLATOR", locales);

    User user = userRepository.findByUsername(username);
    assertEquals(commonName, user.getCommonName());
    assertFalse(user.getCanTranslateAllLocales());
    assertEquals(
        locales.stream().collect(Collectors.toSet()),
        user.getUserLocales().stream()
            .map(userLocale -> userLocale.getLocale().getBcp47Tag())
            .collect(Collectors.toSet()));
  }

  private String createTestUser(String username, String role) throws Exception {
    return createTestUser(username, role, null);
  }

  private String createTestUser(String username, String role, List<String> localeTags)
      throws Exception {
    return createTestUser(username, role, localeTags, false);
  }

  private String createTestUser(
      String username, String role, List<String> localeTags, boolean generatePassword)
      throws Exception {
    String surname = "Mojito";
    String givenName = "Test";
    String commonName = "Test Mojito " + username;

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
    UserCreateCommand userCreateCommand = l10nJCommander.getCommand(UserCreateCommand.class);
    userCreateCommand.console = mockConsole;

    logger.debug("Creating user with username: {}", username);
    List<String> params =
        new ArrayList<>(
            Arrays.asList(
                "user-create",
                Param.USERNAME_SHORT,
                username,
                Param.SURNAME_SHORT,
                surname,
                Param.GIVEN_NAME_SHORT,
                givenName,
                Param.COMMON_NAME_SHORT,
                commonName));

    if (role != null) {
      params.addAll(Arrays.asList(Param.ROLE_SHORT, role));
    }

    if (localeTags != null && !localeTags.isEmpty()) {
      params.add("-l");
      params.addAll(localeTags);
    }

    if (generatePassword) {
      params.add("-gp");
    }

    l10nJCommander.run(params.toArray(new String[0]));

    assertTrue(outputCapture.toString().contains("created --> user: "));
    return commonName;
  }
}

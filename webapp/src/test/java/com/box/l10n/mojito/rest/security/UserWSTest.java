package com.box.l10n.mojito.rest.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.box.l10n.mojito.bootstrap.BootstrapConfig;
import com.box.l10n.mojito.rest.WSTestBase;
import com.box.l10n.mojito.rest.client.UserClient;
import com.box.l10n.mojito.rest.client.exception.ResourceNotCreatedException;
import com.box.l10n.mojito.rest.client.exception.ResourceNotFoundException;
import com.box.l10n.mojito.rest.entity.Authority;
import com.box.l10n.mojito.rest.entity.Role;
import com.box.l10n.mojito.rest.entity.User;
import com.box.l10n.mojito.test.TestIdWatcher;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;

/**
 * @author jyi
 */
public class UserWSTest extends WSTestBase {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(UserWSTest.class);

  @Rule public TestIdWatcher testIdWatcher = new TestIdWatcher();

  @Autowired BootstrapConfig bootstrapConfig;

  @Autowired UserClient userClient;

  @Test
  public void testGetUsers() {
    List<User> users = userClient.getUsers(null);
    assertFalse(users.isEmpty());

    for (User user : users) {
      logger.debug("{}: {}", user.getUsername(), authoritiesToString(user.getAuthorities()));
    }
  }

  @Test
  public void testGetDefaultUser() {
    List<User> users = userClient.getUsers(bootstrapConfig.getDefaultUser().getUsername());
    assertEquals(1, users.size());
    assertEquals(bootstrapConfig.getDefaultUser().getUsername(), users.get(0).getUsername());
  }

  @Test
  public void testCreateAndDeleteUser()
      throws ResourceNotCreatedException, ResourceNotFoundException {

    String username = testIdWatcher.getEntityName("user");
    User user = createTestUser(username);

    List<User> users = userClient.getUsers(username);
    assertEquals(username, users.get(0).getUsername());

    // try creating same user again
    boolean shouldFailToCreateWithExistingUsername = false;
    try {
      createTestUser(username);
    } catch (ResourceNotCreatedException ex) {
      shouldFailToCreateWithExistingUsername = true;
    }
    assertTrue(shouldFailToCreateWithExistingUsername);

    users = userClient.getUsers(username);
    assertFalse(users.isEmpty());
    userClient.deleteUserByUsername(username);
    users = userClient.getUsers(username);
    assertTrue(users.isEmpty());

    // try deleting same user again
    boolean shouldFailToDeleteNonExistingUsername = false;
    try {
      userClient.deleteUserByUsername(username);
    } catch (ResourceNotFoundException ex) {
      shouldFailToDeleteNonExistingUsername = true;
    }
    assertTrue(shouldFailToDeleteNonExistingUsername);
  }

  @Test
  public void testUpdateUser() throws ResourceNotCreatedException, ResourceNotFoundException {
    String username = testIdWatcher.getEntityName("user");
    User user = createTestUser(username);

    String password = "test_updated";
    Role role = Role.ROLE_ADMIN;
    String surname = "Mojito_updated";
    String givenName = "Test_updated";
    String commonName = "Test_updated Mojito_updated";

    userClient.updateUserByUsername(username, password, role, surname, givenName, commonName);
    List<User> users = userClient.getUsers(username);
    assertFalse(users.isEmpty());
    User updatedUser = users.get(0);
    assertEquals(user.getId(), updatedUser.getId());
    assertEquals(username, updatedUser.getUsername());
    assertEquals(1, user.getAuthorities().size());
    assertEquals(role.name(), updatedUser.getAuthorities().iterator().next().getAuthority());
    assertEquals(surname, updatedUser.getSurname());
    assertEquals(givenName, updatedUser.getGivenName());
    assertEquals(commonName, updatedUser.getCommonName());
  }

  private User createTestUser(String username) throws ResourceNotCreatedException {

    String password = "test";
    Role role = Role.ROLE_USER;
    String surname = "Mojito";
    String givenName = "Test";
    String commonName = "Test Mojito";

    User user =
        userClient.createUser(username, password, Role.ROLE_USER, surname, givenName, commonName);
    assertEquals(username, user.getUsername());
    assertEquals(role.name(), user.getAuthorities().iterator().next().getAuthority());
    assertEquals(surname, user.getSurname());
    assertEquals(givenName, user.getGivenName());
    assertEquals(commonName, user.getCommonName());

    return user;
  }

  private String authoritiesToString(Set<Authority> authorities) {
    List<String> list = new ArrayList<>();
    for (Authority authority : authorities) {
      list.add(authority.getAuthority());
    }
    return StringUtils.collectionToCommaDelimitedString(list);
  }
}

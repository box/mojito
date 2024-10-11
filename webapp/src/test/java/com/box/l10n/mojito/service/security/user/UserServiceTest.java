package com.box.l10n.mojito.service.security.user;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.box.l10n.mojito.entity.security.user.User;
import com.box.l10n.mojito.security.Role;
import com.box.l10n.mojito.service.assetExtraction.ServiceTestBase;
import com.box.l10n.mojito.test.TestIdWatcher;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

public class UserServiceTest extends ServiceTestBase {

  @Autowired UserService userService;

  @Autowired UserRepository userRepository;

  @Rule public TestIdWatcher testIdWatcher = new TestIdWatcher();

  @Test
  public void testCreateBasicUser() {

    String username = testIdWatcher.getEntityName("testUser");
    String pwd = "testPwd1234";
    String surname = "surname";
    String givenName = "givenName";
    String commonName = "commonName";
    Role userRole = Role.ROLE_USER;
    String expectedAuthorityName = userService.createAuthorityName(userRole);

    User userWithRole =
        userService.createUserWithRole(
            username, pwd, userRole, givenName, surname, commonName, false);

    User byUsername = userRepository.findByUsername(username);

    assertEquals("ID should be the same", userWithRole.getId(), byUsername.getId());
    assertNotSame("Password should not be plain", pwd, byUsername.getPassword());
    assertFalse("Should have at least one authority", byUsername.getAuthorities().isEmpty());
    assertEquals(
        "Should have user role",
        expectedAuthorityName,
        byUsername.getAuthorities().iterator().next().getAuthority());
    assertEquals(surname, byUsername.getSurname());
    assertEquals(givenName, byUsername.getGivenName());
    assertEquals(commonName, byUsername.getCommonName());
  }

  @Test(expected = IllegalStateException.class)
  public void testCreateUserWithEmptyPassword() {
    String username = "testUser";
    String pwd = "";
    userService.createUserWithRole(username, pwd, Role.ROLE_USER);
  }

  @Test(expected = NullPointerException.class)
  public void testCreateUserWithNullPassword() {
    String username = "testUser";
    String pwd = null;
    userService.createUserWithRole(username, pwd, Role.ROLE_USER);
  }

  @Test
  public void testSystemUserExist() {
    User systemUser = userService.findSystemUser();
    assertNotNull("System user should always been created.", systemUser);
    assertNotNull("System user has a createdByUser", systemUser.getCreatedByUser());
    assertFalse("System user should be disabled", systemUser.getEnabled());
  }

  @Test
  @Transactional
  public void testFindAllWithoutCommonName() {
    this.userService.createUserWithRole(
        "username", "password", Role.ROLE_USER, "givenName", "surname", null, false);
    Page<User> usersPage =
        this.userService.findByUsernameOrName("username", null, Pageable.ofSize(10));
    assertEquals(1, usersPage.getContent().size());
    assertEquals("username", usersPage.getContent().getFirst().getUsername());
    assertEquals("givenName", usersPage.getContent().getFirst().getGivenName());
    assertEquals("surname", usersPage.getContent().getFirst().getSurname());
    assertNull(usersPage.getContent().getFirst().getCommonName());

    usersPage =
        this.userService.findByUsernameOrName(null, "givenname surname", Pageable.ofSize(10));
    assertEquals(1, usersPage.getContent().size());
    assertEquals("username", usersPage.getContent().getFirst().getUsername());
    assertEquals("givenName", usersPage.getContent().getFirst().getGivenName());
    assertEquals("surname", usersPage.getContent().getFirst().getSurname());
    assertNull(usersPage.getContent().getFirst().getCommonName());

    usersPage =
        this.userService.findByUsernameOrName(null, "givenname surnam", Pageable.ofSize(10));
    assertEquals(1, usersPage.getContent().size());
    assertEquals("username", usersPage.getContent().getFirst().getUsername());
    assertEquals("givenName", usersPage.getContent().getFirst().getGivenName());
    assertEquals("surname", usersPage.getContent().getFirst().getSurname());
    assertNull(usersPage.getContent().getFirst().getCommonName());

    usersPage =
        this.userService.findByUsernameOrName(null, "ivenName surname", Pageable.ofSize(10));
    assertEquals(1, usersPage.getContent().size());
    assertEquals("username", usersPage.getContent().getFirst().getUsername());
    assertEquals("givenName", usersPage.getContent().getFirst().getGivenName());
    assertEquals("surname", usersPage.getContent().getFirst().getSurname());
    assertNull(usersPage.getContent().getFirst().getCommonName());

    usersPage =
        this.userService.findByUsernameOrName(null, "GIVENNAME SURNAME", Pageable.ofSize(10));
    assertEquals(1, usersPage.getContent().size());
    assertEquals("username", usersPage.getContent().getFirst().getUsername());
    assertEquals("givenName", usersPage.getContent().getFirst().getGivenName());
    assertEquals("surname", usersPage.getContent().getFirst().getSurname());
    assertNull(usersPage.getContent().getFirst().getCommonName());

    usersPage = this.userService.findByUsernameOrName(null, "username", Pageable.ofSize(10));
    assertEquals(1, usersPage.getContent().size());
    assertEquals("username", usersPage.getContent().getFirst().getUsername());
    assertEquals("givenName", usersPage.getContent().getFirst().getGivenName());
    assertEquals("surname", usersPage.getContent().getFirst().getSurname());
    assertNull(usersPage.getContent().getFirst().getCommonName());

    usersPage = this.userService.findByUsernameOrName(null, "usernam", Pageable.ofSize(10));
    assertEquals(1, usersPage.getContent().size());
    assertEquals("username", usersPage.getContent().getFirst().getUsername());
    assertEquals("givenName", usersPage.getContent().getFirst().getGivenName());
    assertEquals("surname", usersPage.getContent().getFirst().getSurname());
    assertNull(usersPage.getContent().getFirst().getCommonName());

    usersPage = this.userService.findByUsernameOrName(null, "sername", Pageable.ofSize(10));
    assertEquals(1, usersPage.getContent().size());
    assertEquals("username", usersPage.getContent().getFirst().getUsername());
    assertEquals("givenName", usersPage.getContent().getFirst().getGivenName());
    assertEquals("surname", usersPage.getContent().getFirst().getSurname());
    assertNull(usersPage.getContent().getFirst().getCommonName());

    usersPage = this.userService.findByUsernameOrName(null, "USERNAME", Pageable.ofSize(10));
    assertEquals(1, usersPage.getContent().size());
    assertEquals("username", usersPage.getContent().getFirst().getUsername());
    assertEquals("givenName", usersPage.getContent().getFirst().getGivenName());
    assertEquals("surname", usersPage.getContent().getFirst().getSurname());
    assertNull(usersPage.getContent().getFirst().getCommonName());

    usersPage =
        this.userService.findByUsernameOrName("usernameWithoutMatch", null, Pageable.ofSize(10));
    assertEquals(0, usersPage.getContent().size());

    usersPage = this.userService.findByUsernameOrName(null, "noMatch", Pageable.ofSize(10));
    assertEquals(0, usersPage.getContent().size());
  }

  @Test
  @Transactional
  public void testFindAllWithCommonName() {
    this.userService.createUserWithRole(
        "test_username",
        "password",
        Role.ROLE_USER,
        "testGivenName",
        "testSurname",
        "commonName",
        false);
    Page<User> usersPage =
        this.userService.findByUsernameOrName("test_username", null, Pageable.ofSize(10));
    assertEquals(1, usersPage.getContent().size());
    assertEquals("test_username", usersPage.getContent().getFirst().getUsername());
    assertEquals("testGivenName", usersPage.getContent().getFirst().getGivenName());
    assertEquals("testSurname", usersPage.getContent().getFirst().getSurname());
    assertEquals("commonName", usersPage.getContent().getFirst().getCommonName());

    usersPage = this.userService.findByUsernameOrName(null, "commonName", Pageable.ofSize(10));
    assertEquals(1, usersPage.getContent().size());
    assertEquals("test_username", usersPage.getContent().getFirst().getUsername());
    assertEquals("testGivenName", usersPage.getContent().getFirst().getGivenName());
    assertEquals("testSurname", usersPage.getContent().getFirst().getSurname());
    assertEquals("commonName", usersPage.getContent().getFirst().getCommonName());

    usersPage = this.userService.findByUsernameOrName(null, "commonNam", Pageable.ofSize(10));
    assertEquals(1, usersPage.getContent().size());
    assertEquals("test_username", usersPage.getContent().getFirst().getUsername());
    assertEquals("testGivenName", usersPage.getContent().getFirst().getGivenName());
    assertEquals("testSurname", usersPage.getContent().getFirst().getSurname());
    assertEquals("commonName", usersPage.getContent().getFirst().getCommonName());

    usersPage = this.userService.findByUsernameOrName(null, "ommonName", Pageable.ofSize(10));
    assertEquals(1, usersPage.getContent().size());
    assertEquals("test_username", usersPage.getContent().getFirst().getUsername());
    assertEquals("testGivenName", usersPage.getContent().getFirst().getGivenName());
    assertEquals("testSurname", usersPage.getContent().getFirst().getSurname());
    assertEquals("commonName", usersPage.getContent().getFirst().getCommonName());

    usersPage = this.userService.findByUsernameOrName(null, "COMMONNAME", Pageable.ofSize(10));
    assertEquals(1, usersPage.getContent().size());
    assertEquals("test_username", usersPage.getContent().getFirst().getUsername());
    assertEquals("testGivenName", usersPage.getContent().getFirst().getGivenName());
    assertEquals("testSurname", usersPage.getContent().getFirst().getSurname());
    assertEquals("commonName", usersPage.getContent().getFirst().getCommonName());

    usersPage =
        this.userService.findByUsernameOrName(null, "no commonName match", Pageable.ofSize(10));
    assertEquals(0, usersPage.getContent().size());

    usersPage =
        this.userService.findByUsernameOrName(
            null, "testGivenName testSurname", Pageable.ofSize(10));
    assertEquals(0, usersPage.getContent().size());
  }

  @Test
  @Transactional
  public void testFindAllWithBlankSearch() {
    this.userService.createUserWithRole(
        "blank_username",
        "password",
        Role.ROLE_USER,
        "BlankGivenName",
        "BlankSurname",
        null,
        false);
    Page<User> usersPage =
        this.userService.findByUsernameOrName("blank_username", "", Pageable.ofSize(10));
    assertEquals(1, usersPage.getContent().size());
    assertEquals("blank_username", usersPage.getContent().getFirst().getUsername());
    assertEquals("BlankGivenName", usersPage.getContent().getFirst().getGivenName());
    assertEquals("BlankSurname", usersPage.getContent().getFirst().getSurname());
    assertNull(usersPage.getContent().getFirst().getCommonName());

    usersPage = this.userService.findByUsernameOrName(null, "", Pageable.ofSize(10));
    assertFalse(usersPage.getContent().isEmpty());
    assertTrue(
        usersPage.getContent().stream()
            .anyMatch(user -> user.getUsername().equals("blank_username")));
  }

  @Test
  @Transactional
  public void testFindAllForMultipleMatches() {
    this.userService.createUserWithRole(
        "first_username",
        "password",
        Role.ROLE_USER,
        "FirstGivenName",
        "FirstSurname",
        null,
        false);
    this.userService.createUserWithRole(
        "second_username",
        "password",
        Role.ROLE_USER,
        "SecondGivenName",
        "FirstSurname",
        null,
        false);
    this.userService.createUserWithRole(
        "third_username",
        "password",
        Role.ROLE_USER,
        "SecondGivenName",
        "ThirdSurname",
        "SecondGivenName",
        false);
    Page<User> usersPage =
        this.userService.findByUsernameOrName(null, "username", Pageable.ofSize(10));
    assertEquals(3, usersPage.getContent().size());
    assertEquals("first_username", usersPage.getContent().getFirst().getUsername());
    assertEquals("second_username", usersPage.getContent().get(1).getUsername());
    assertEquals("third_username", usersPage.getContent().getLast().getUsername());

    usersPage = this.userService.findByUsernameOrName(null, "FirstSurname", Pageable.ofSize(10));
    assertEquals(2, usersPage.getContent().size());
    assertEquals("first_username", usersPage.getContent().getFirst().getUsername());
    assertEquals("second_username", usersPage.getContent().getLast().getUsername());

    usersPage = this.userService.findByUsernameOrName(null, "SecondGivenName", Pageable.ofSize(10));
    assertEquals(2, usersPage.getContent().size());
    assertEquals("second_username", usersPage.getContent().getFirst().getUsername());
    assertEquals("third_username", usersPage.getContent().getLast().getUsername());
  }
}

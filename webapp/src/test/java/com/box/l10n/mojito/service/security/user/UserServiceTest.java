package com.box.l10n.mojito.service.security.user;

import com.box.l10n.mojito.entity.security.user.User;
import com.box.l10n.mojito.security.Role;
import com.box.l10n.mojito.service.assetExtraction.ServiceTestBase;
import com.box.l10n.mojito.test.TestIdWatcher;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import org.junit.Rule;

public class UserServiceTest extends ServiceTestBase {

    @Autowired
    UserService userService;

    @Autowired
    UserRepository userRepository;

    @Rule
    public TestIdWatcher testIdWatcher = new TestIdWatcher();

    @Test
    public void testCreateBasicUser() {

        String username = testIdWatcher.getEntityName("testUser");
        String pwd = "testPwd1234";
        String surname = "surname";
        String givenName = "givenName";
        String commonName = "commonName";
        Role userRole = Role.USER;
        String expectedAuthorityName = userService.createAuthorityName(userRole);

        User userWithRole = userService.createUserWithRole(username, pwd, userRole, givenName, surname, commonName, false);

        User byUsername = userRepository.findByUsername(username);

        assertEquals("ID should be the same", userWithRole.getId(), byUsername.getId());
        assertNotSame("Password should not be plain", pwd, byUsername.getPassword());
        assertFalse("Should have at least one authority", byUsername.getAuthorities().isEmpty());
        assertEquals("Should have user role", expectedAuthorityName, byUsername.getAuthorities().iterator().next().getAuthority());
        assertEquals(surname, byUsername.getSurname());
        assertEquals(givenName, byUsername.getGivenName());
        assertEquals(commonName, byUsername.getCommonName());
    }

    @Test(expected = IllegalStateException.class)
    public void testCreateUserWithEmptyPassword() {
        String username = "testUser";
        String pwd = "";
        userService.createUserWithRole(username, pwd, Role.USER);
    }

    @Test(expected = NullPointerException.class)
    public void testCreateUserWithNullPassword() {
        String username = "testUser";
        String pwd = null;
        userService.createUserWithRole(username, pwd, Role.USER);
    }

    @Test
    public void testSystemUserExist() {
        User systemUser = userService.findSystemUser();
        assertNotNull("System user should always been created.", systemUser);
        assertNotNull("System user has a createdByUser", systemUser.getCreatedByUser());
        assertFalse("System user should be disabled", systemUser.getEnabled());
    }
}

package com.box.l10n.mojito.cli.command;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.internal.DefaultConsole;
import com.box.l10n.mojito.cli.CLITestBase;
import com.box.l10n.mojito.cli.Console;
import static com.box.l10n.mojito.cli.command.UserUpdateCommandTest.logger;
import com.box.l10n.mojito.cli.command.param.Param;
import com.box.l10n.mojito.entity.security.user.User;
import com.box.l10n.mojito.security.Role;
import com.box.l10n.mojito.service.security.user.UserRepository;
import java.lang.reflect.Field;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import static org.mockito.Matchers.anyString;
import org.mockito.Mockito;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author jyi
 */
public class UserCreateCommandTest extends CLITestBase {
    
    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(UserCreateCommandTest.class);
    
    @Autowired
    UserRepository userRepository;
    
    @Test
    public void testCreateUserWithDefaultRole() throws Exception {
        
        String username = testIdWatcher.getEntityName("user");
        String commonName = createTestUser(username, null);

        User user = userRepository.findByUsername(username);
        assertEquals(commonName, user.getCommonName());
        assertTrue(user.getAuthorities().iterator().next().getAuthority().contains(Role.USER.toString()));

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
        assertTrue(outputCapture.toString().contains("Error creating user"));
    }
        
    private String createTestUser(String username, String role) throws Exception {
        String surname = "Mojito";
        String givenName = "Test";
        String commonName = "Test Mojito " + username;
              
          logger.debug("Mocking the console input for password");
        Console mockConsole = mock(Console.class);
        when(mockConsole.readPassword()).thenAnswer(new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocation) throws Throwable {
                return "test";
            }
        });
        
        L10nJCommander l10nJCommander = getL10nJCommander();
        UserCreateCommand userCreateCommand = l10nJCommander.getCommand(UserCreateCommand.class);
        userCreateCommand.console = mockConsole;
        
        
        logger.debug("Creating user with username: {}", username);
        if (role == null) {
            l10nJCommander.run("user-create", Param.USERNAME_SHORT, username, Param.SURNAME_SHORT, surname, Param.GIVEN_NAME_SHORT, givenName, Param.COMMON_NAME_SHORT, commonName);
        } else {
            l10nJCommander.run("user-create", Param.USERNAME_SHORT, username, Param.ROLE_SHORT, role, Param.SURNAME_SHORT, surname, Param.GIVEN_NAME_SHORT, givenName, Param.COMMON_NAME_SHORT, commonName);
        }
        
        assertTrue(outputCapture.toString().contains("created --> user: "));
        return commonName;
    }
    
}

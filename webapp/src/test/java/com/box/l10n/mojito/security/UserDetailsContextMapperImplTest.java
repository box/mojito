package com.box.l10n.mojito.security;

import com.box.l10n.mojito.entity.security.user.User;
import com.box.l10n.mojito.service.security.user.UserRepository;
import com.box.l10n.mojito.service.security.user.UserService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.Assert;

import static org.mockito.Mockito.anyObject;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author wyau
 */
@RunWith(MockitoJUnitRunner.class)
public class UserDetailsContextMapperImplTest {

    @InjectMocks
    UserDetailsContextMapperImpl userDetailsContextMapper;

    @Mock
    UserService userService;

    @Mock
    UserRepository userRepository;

    @Test
    public void testMapUserFromContextWhenUserNameIsNotFound() throws Exception {
        when(userRepository.findByUsername(anyString())).thenReturn(null);

        when(userService.createOrUpdateBasicUser(anyObject(), anyString(), anyString(), anyString(),
                anyString())).thenReturn(mock(User.class));

        DirContextOperations dirContextOperations = mock(DirContextOperations.class);
        when(dirContextOperations.getStringAttribute("givenname")).thenReturn("givename");
        when(dirContextOperations.getStringAttribute("sn")).thenReturn("sn");
        when(dirContextOperations.getStringAttribute("cn")).thenReturn("cn");

        UserDetails userDetails = userDetailsContextMapper.mapUserFromContext(dirContextOperations, "testUsername", null);

        Assert.notNull(userDetails);
        verify(dirContextOperations, times(3)).getStringAttribute(anyString());
    }

    @Test
    public void testMapUserFromContextWhenUserNameIsFound() throws Exception {
        when(userRepository.findByUsername(anyString())).thenReturn(mock(User.class));

        DirContextOperations dirContextOperations = mock(DirContextOperations.class);
        UserDetails userDetails = userDetailsContextMapper.mapUserFromContext(dirContextOperations, "testUsername", null);

        Assert.notNull(userDetails);
        verify(dirContextOperations, never()).getStringAttribute(anyString());
        verify(userService, never()).createOrUpdateBasicUser(anyObject(), anyString(),
                        anyString(), anyString(), anyString());
    }
}

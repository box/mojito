package com.box.l10n.mojito.security;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.util.Assert;

/**
 * @author wyau
 */
@RunWith(MockitoJUnitRunner.class)
public class UserDetailsContextMapperImplTest {

    @InjectMocks
    UserDetailsContextMapperImpl userDetailsContextMapper;

    @Mock
    UserDetailsServiceImpl userDetailsService;

    @Test
    public void testMapUserFromContextWhenUserNameIsNotFound() throws Exception {
        Mockito.when(userDetailsService.loadUserByUsername(Mockito.anyString()))
                .thenThrow(new UsernameNotFoundException(""));

        Mockito.when(userDetailsService.createBasicUser(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(),
                Mockito.anyString())).thenReturn(Mockito.mock(UserDetails.class));

        DirContextOperations dirContextOperations = Mockito.mock(DirContextOperations.class);
        Mockito.when(dirContextOperations.getStringAttribute("givenname")).thenReturn("givename");
        Mockito.when(dirContextOperations.getStringAttribute("sn")).thenReturn("sn");
        Mockito.when(dirContextOperations.getStringAttribute("cn")).thenReturn("cn");

        UserDetails userDetails = userDetailsContextMapper.mapUserFromContext(dirContextOperations, "testUsername", null);

        Assert.notNull(userDetails);
        Mockito.verify(dirContextOperations, Mockito.times(3)).getStringAttribute(Mockito.anyString());
    }

    @Test
    public void testMapUserFromContextWhenUserNameIsFound() throws Exception {
        Mockito.when(userDetailsService.loadUserByUsername(Mockito.anyString())).thenReturn(Mockito.mock(UserDetails.class));

        Mockito.when(userDetailsService.createBasicUser(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(),
                Mockito.anyString())).thenReturn(Mockito.mock(UserDetails.class));

        DirContextOperations dirContextOperations = Mockito.mock(DirContextOperations.class);
        Mockito.when(dirContextOperations.getStringAttribute("givenname")).thenReturn("givename");
        Mockito.when(dirContextOperations.getStringAttribute("sn")).thenReturn("sn");
        Mockito.when(dirContextOperations.getStringAttribute("cn")).thenReturn("cn");

        UserDetails userDetails = userDetailsContextMapper.mapUserFromContext(dirContextOperations, "testUsername", null);

        Assert.notNull(userDetails);
        Mockito.verify(dirContextOperations, Mockito.never()).getStringAttribute(Mockito.anyString());
        Mockito.verify(userDetailsService, Mockito.never()).createBasicUser(Mockito.anyString(),
                        Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
    }
}

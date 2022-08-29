package com.box.l10n.mojito.security;

import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.box.l10n.mojito.entity.security.user.User;
import com.box.l10n.mojito.service.security.user.UserService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.Assert;

/** @author wyau */
@RunWith(MockitoJUnitRunner.class)
public class UserDetailsContextMapperImplTest {

  @InjectMocks UserDetailsContextMapperImpl userDetailsContextMapper;

  @Mock UserService userService;

  @Test
  public void testMapUserFromContextWhenUserNameIsNotFound() throws Exception {
    doReturn(mock(User.class))
        .when(userService)
        .getOrCreateOrUpdateBasicUser("testUsername", "givename", "sn", "cn");

    DirContextOperations dirContextOperations = mock(DirContextOperations.class);
    when(dirContextOperations.getStringAttribute("givenname")).thenReturn("givename");
    when(dirContextOperations.getStringAttribute("sn")).thenReturn("sn");
    when(dirContextOperations.getStringAttribute("cn")).thenReturn("cn");

    UserDetails userDetails =
        userDetailsContextMapper.mapUserFromContext(dirContextOperations, "testUsername", null);

    Assert.notNull(userDetails);
    verify(dirContextOperations, times(3)).getStringAttribute(anyString());
  }
}

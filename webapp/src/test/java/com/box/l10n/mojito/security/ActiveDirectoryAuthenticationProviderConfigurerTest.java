package com.box.l10n.mojito.security;

import java.util.Collection;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.ldap.core.DirContextAdapter;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.ldap.authentication.ad.ActiveDirectoryLdapAuthenticationProvider;
import org.springframework.security.ldap.userdetails.UserDetailsContextMapper;

/** @author jaurambault */
public class ActiveDirectoryAuthenticationProviderConfigurerTest {

  @Test
  public void testBuilderFunctions() throws Exception {

    ActiveDirectoryAuthenticationProviderConfigurer<AuthenticationManagerBuilder> instance =
        new ActiveDirectoryAuthenticationProviderConfigurer<>();

    UserDetailsContextMapper userDetailsContextMapper =
        new UserDetailsContextMapper() {
          @Override
          public UserDetails mapUserFromContext(
              DirContextOperations ctx,
              String username,
              Collection<? extends GrantedAuthority> authorities) {
            throw new UnsupportedOperationException(
                "Not supported yet."); // To change body of generated methods, choose Tools |
            // Templates.
          }

          @Override
          public void mapUserToContext(UserDetails user, DirContextAdapter ctx) {
            throw new UnsupportedOperationException(
                "Not supported yet."); // To change body of generated methods, choose Tools |
            // Templates.
          }
        };

    ActiveDirectoryLdapAuthenticationProvider provider =
        instance
            .url("testurl")
            .domain("testdomain")
            .rootDn("testRootDn")
            .userServiceDetailMapper(userDetailsContextMapper)
            .build();

    Assert.assertEquals("testurl", FieldUtils.readField(provider, "url", true));
    Assert.assertEquals("testdomain", FieldUtils.readField(provider, "domain", true));
    Assert.assertEquals("testrootdn", FieldUtils.readField(provider, "rootDn", true));
    Assert.assertEquals(
        userDetailsContextMapper, FieldUtils.readField(provider, "userDetailsContextMapper", true));
  }
}

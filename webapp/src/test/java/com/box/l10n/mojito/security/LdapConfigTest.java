package com.box.l10n.mojito.security;

import com.box.l10n.mojito.service.assetExtraction.ServiceTestBase;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author jaurambault
 */
public class LdapConfigTest extends ServiceTestBase {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(LdapConfigTest.class);

  @Autowired LdapConfig ldapConfig;

  @Test
  public void testConfig() {
    Assert.assertEquals("cn", ldapConfig.getGroupRoleAttribute());
    Assert.assertEquals("", ldapConfig.getGroupSearchBase());
    Assert.assertEquals("(uniqueMember={0})", ldapConfig.getGroupSearchFilter());
    Assert.assertEquals("classpath:embedded-ldap-server.ldif", ldapConfig.getLdif());
    Assert.assertEquals("cn=bind_user,ou=Managers,dc=mojito,dc=org", ldapConfig.getManagerDn());
    Assert.assertEquals("test", ldapConfig.getManagerPassword());
    Assert.assertNull(ldapConfig.getPort());
    Assert.assertEquals("dc=mojito,dc=org", ldapConfig.getRoot());
    Assert.assertNull(ldapConfig.getUrl());
    Assert.assertEquals("ou=Users", ldapConfig.getUserSearchBase());
    Assert.assertEquals("(&(objectclass=posixAccount)(uid={0}))", ldapConfig.getUserSearchFilter());
  }
}

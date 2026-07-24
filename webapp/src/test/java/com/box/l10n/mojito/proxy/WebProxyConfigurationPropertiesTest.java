package com.box.l10n.mojito.proxy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class WebProxyConfigurationPropertiesTest {

  @Test
  public void testResolvePrefersPerServiceWhenHostAndPortSet() {
    WebProxyConfigurationProperties webProxy = new WebProxyConfigurationProperties();
    webProxy.setHost("shared.example.com");
    webProxy.setPort(8080);
    webProxy.setUser("shared-user");
    webProxy.setPassword("shared-password");
    webProxy.setAllowBasicTunneling(true);

    ResolvedWebProxy resolved =
        webProxy.resolve("service.example.com", 3128, "service-user", "service-password");

    assertEquals("service.example.com", resolved.host());
    assertEquals(Integer.valueOf(3128), resolved.port());
    assertEquals("service-user", resolved.user());
    assertEquals("service-password", resolved.password());
    assertTrue(webProxy.isAllowBasicTunneling());
  }

  @Test
  public void testResolveFallsBackToSharedWhenServiceIncomplete() {
    WebProxyConfigurationProperties webProxy = new WebProxyConfigurationProperties();
    webProxy.setHost("shared.example.com");
    webProxy.setPort(8080);
    webProxy.setUser("shared-user");
    webProxy.setPassword("shared-password");
    webProxy.setAllowBasicTunneling(true);

    ResolvedWebProxy resolved = webProxy.resolve("service.example.com", null, "ignored", "ignored");

    assertEquals("shared.example.com", resolved.host());
    assertEquals(Integer.valueOf(8080), resolved.port());
    assertEquals("shared-user", resolved.user());
    assertEquals("shared-password", resolved.password());
  }

  @Test
  public void testResolveReturnsNoneWhenNothingConfigured() {
    ResolvedWebProxy resolved =
        new WebProxyConfigurationProperties().resolve(null, null, null, null);

    assertFalse(resolved.isConfigured());
    assertNull(resolved.host());
  }

  @Test
  public void testIsConfiguredRequiresHostAndPort() {
    assertFalse(WebProxyConfigurationProperties.isConfigured(null, 3128));
    assertFalse(WebProxyConfigurationProperties.isConfigured("   ", 3128));
    assertFalse(WebProxyConfigurationProperties.isConfigured("proxy.example.com", null));
    assertTrue(WebProxyConfigurationProperties.isConfigured("proxy.example.com", 3128));
  }
}

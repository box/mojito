package com.box.l10n.mojito.service.oaitranslate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(
    classes = {AiTranslateConfigurationPropertiesTest.class},
    properties = {
      "l10n.ai-translate.openai-client-token=test-token",
      "l10n.ai-translate.model-name=gpt-4o-test",
      "l10n.ai-translate.proxy-host=proxy.example.com",
      "l10n.ai-translate.proxy-port=3128",
      "l10n.ai-translate.proxy-user=proxy-user",
      "l10n.ai-translate.proxy-password=proxy-password",
      "l10n.ai-translate.proxy-preferred-auth-schemes=Basic,Digest"
    })
@EnableConfigurationProperties(AiTranslateConfigurationProperties.class)
public class AiTranslateConfigurationPropertiesTest {

  @Autowired AiTranslateConfigurationProperties aiTranslateConfigurationProperties;

  @Test
  public void testProxyPropertiesAreBound() {
    assertEquals("test-token", aiTranslateConfigurationProperties.getOpenaiClientToken());
    assertEquals("gpt-4o-test", aiTranslateConfigurationProperties.getModelName());
    assertEquals("proxy.example.com", aiTranslateConfigurationProperties.getProxyHost());
    assertEquals(Integer.valueOf(3128), aiTranslateConfigurationProperties.getProxyPort());
    assertEquals("proxy-user", aiTranslateConfigurationProperties.getProxyUser());
    assertEquals("proxy-password", aiTranslateConfigurationProperties.getProxyPassword());
    assertEquals("Basic,Digest", aiTranslateConfigurationProperties.getProxyPreferredAuthSchemes());
  }

  @Test
  public void testProxyPropertiesDefaultToNullWhenUnset() {
    AiTranslateConfigurationProperties properties = new AiTranslateConfigurationProperties();

    assertNull(properties.getProxyHost());
    assertNull(properties.getProxyPort());
    assertNull(properties.getProxyUser());
    assertNull(properties.getProxyPassword());
    assertNull(properties.getProxyPreferredAuthSchemes());
  }
}

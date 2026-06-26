package com.box.l10n.mojito.service.oaireview;

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
    classes = {AiReviewConfigurationPropertiesTest.class},
    properties = {
      "l10n.ai-review.openai-client-token=test-token",
      "l10n.ai-review.model-name=gpt-4o-test",
      "l10n.ai-review.proxy-host=proxy.example.com",
      "l10n.ai-review.proxy-port=3128",
      "l10n.ai-review.proxy-user=proxy-user",
      "l10n.ai-review.proxy-password=proxy-password"
    })
@EnableConfigurationProperties(AiReviewConfigurationProperties.class)
public class AiReviewConfigurationPropertiesTest {

  @Autowired AiReviewConfigurationProperties aiReviewConfigurationProperties;

  @Test
  public void testProxyPropertiesAreBound() {
    assertEquals("test-token", aiReviewConfigurationProperties.getOpenaiClientToken());
    assertEquals("gpt-4o-test", aiReviewConfigurationProperties.getModelName());
    assertEquals("proxy.example.com", aiReviewConfigurationProperties.getProxyHost());
    assertEquals(Integer.valueOf(3128), aiReviewConfigurationProperties.getProxyPort());
    assertEquals("proxy-user", aiReviewConfigurationProperties.getProxyUser());
    assertEquals("proxy-password", aiReviewConfigurationProperties.getProxyPassword());
  }

  @Test
  public void testProxyPropertiesDefaultToNullWhenUnset() {
    AiReviewConfigurationProperties properties = new AiReviewConfigurationProperties();

    assertNull(properties.getProxyHost());
    assertNull(properties.getProxyPort());
    assertNull(properties.getProxyUser());
    assertNull(properties.getProxyPassword());
  }
}

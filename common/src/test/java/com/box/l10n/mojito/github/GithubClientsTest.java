package com.box.l10n.mojito.github;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(
    classes = {GithubClientsConfiguration.class, GithubClients.class},
    properties = {
      "l10n.githubClients.client-1.owner=testOwner1",
      "l10n.githubClients.client-1.appId=testAppId1",
      "l10n.githubClients.client-1.key=testKey1",
      "l10n.githubClients.client-2.owner=testOwner2",
      "l10n.githubClients.client-2.appId=testAppId2",
      "l10n.githubClients.client-2.key=testKey2",
    })
@EnableConfigurationProperties
public class GithubClientsTest {

  @Autowired GithubClients githubClients;

  @Test
  public void testClientCreation() {
    GithubClient githubClient1 = githubClients.getClient("testOwner1");
    assertEquals("testOwner1", githubClient1.getOwner());
    assertEquals("testAppId1", githubClient1.getAppId());

    GithubClient githubClient2 = githubClients.getClient("testOwner2");
    assertEquals("testOwner2", githubClient2.getOwner());
    assertEquals("testAppId2", githubClient2.getAppId());
  }
}

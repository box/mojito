package com.box.l10n.mojito.slack;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.aspectj.EnableSpringConfigured;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = {SlackClientTest.class})
@EnableAutoConfiguration
public class SlackClientTest {

    @Value("${l10n.slack.token}")
    String token;

    @Ignore
    @Test
    public void testClient() throws SlackClientException {
        SlackClient slackClient = new SlackClient(token);
        slackClient.sendInstantMessage("someone@mojito.com", "test");

    }

}
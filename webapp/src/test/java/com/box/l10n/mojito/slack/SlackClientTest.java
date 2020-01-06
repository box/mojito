package com.box.l10n.mojito.slack;

import com.box.l10n.mojito.mustache.MustacheTemplateEngine;
import com.box.l10n.mojito.slack.request.Channel;
import com.box.l10n.mojito.slack.request.Message;
import com.box.l10n.mojito.slack.response.ChatPostMessageResponse;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.aspectj.EnableSpringConfigured;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

@RunWith(SpringJUnit4ClassRunner.class)
@EnableAutoConfiguration
@SpringBootTest(properties = "spring.datasource.initialize=false",
        classes = {SlackClientTest.class, SlackClientConfiguration.class},
      webEnvironment = SpringBootTest.WebEnvironment.NONE)
public class SlackClientTest {

    @Autowired(required = false)
    SlackClient slackClient;

    @Value("${test.l10n.slack.email.destination:someemail@test.com}")
    String email;

    @Before
    public void assumeClient() {
        Assume.assumeNotNull(slackClient);
    }

    @Test
    public void testClient() throws SlackClientException {
        Channel instantMessageChannel = slackClient.getInstantMessageChannel(email);

        Message message = new Message();
        message.setText("test");
        message.setChannel(instantMessageChannel.getId());
        ChatPostMessageResponse chatPostMessageResponse = slackClient.sendInstantMessage(message);

        Message reply = new Message();
        reply.setChannel(instantMessageChannel.getId());
        reply.setText("response");
        reply.setThreadTs(chatPostMessageResponse.getTs());

        slackClient.sendInstantMessage(reply);
    }
}
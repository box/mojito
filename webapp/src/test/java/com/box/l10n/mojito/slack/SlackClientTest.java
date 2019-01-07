package com.box.l10n.mojito.slack;

import com.box.l10n.mojito.slack.request.Channel;
import com.box.l10n.mojito.slack.request.Message;
import com.box.l10n.mojito.slack.response.ChatPostMessageResponse;
import org.junit.Assume;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = {SlackClientTest.class})
@EnableAutoConfiguration
public class SlackClientTest {

    @Value("${l10n.slack.token}")
    String token;

    @Value("${test.l10n.slack.email.destination:someemail@test.com}")
    String email;

    @Test
    public void testClient() throws SlackClientException {
        Assume.assumeNotNull(token);

        SlackClient slackClient = new SlackClient(token);

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
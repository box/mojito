package com.box.l10n.mojito.thirdpartynotification.slack;

import com.box.l10n.mojito.slack.SlackClient;
import com.box.l10n.mojito.slack.SlackClientException;
import com.ibm.icu.text.MessageFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty("l10n.slack.token")
public class SlackChannels {

    @Autowired
    SlackClient slackClient;

    public String getSlackChannelForDirectOrBotMessage(boolean useDirectMessage, String username, String userEmailPattern) throws SlackClientException {
        String channel;

        if (useDirectMessage) {
            channel = slackClient.getInstantMessageChannel(getEmail(userEmailPattern, username)).getId();
        } else {
            channel = getSlackbotChannel(username);
        }

        return channel;
    }

    private String getSlackbotChannel(String username) {
        return "@" + username;
    }

    private String getEmail(String userEmailPattern, String username) {
        return MessageFormat.format(userEmailPattern, username);
    }
}

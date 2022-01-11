package com.box.l10n.mojito.notifications.service.slack;

import com.box.l10n.mojito.slack.SlackClient;
import com.box.l10n.mojito.slack.SlackClientException;
import com.ibm.icu.text.MessageFormat;

public class SlackUtils {

    public static String getSlackChannel(boolean useDirectMessage, String username, String userEmailPattern, SlackClient slackClient) throws SlackClientException {
        String channel;

        if (useDirectMessage) {
            channel = slackClient.getInstantMessageChannel(getEmail(userEmailPattern, username)).getId();
        } else {
            channel = getSlackbotChannel(username);
        }

        return channel;
    }

    private static String getSlackbotChannel(String username) {
        return "@" + username;
    }

    private static String getEmail(String userEmailPattern, String username) {
        return MessageFormat.format(userEmailPattern, username);
    }
}

package com.box.l10n.mojito.service.branch.notification.phabricator;

import com.box.l10n.mojito.service.branch.BranchUrlBuilder;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

import static org.slf4j.LoggerFactory.getLogger;

@Component
public class BranchNotificationMessageBuilderPhabricator {

    /**
     * logger
     */
    static Logger logger = getLogger(BranchNotificationMessageBuilderPhabricator.class);

    @Autowired
    BranchUrlBuilder branchUrlBuilder;

    @Value("${l10n.branchNotification.phabricator.notification.message.new:We received your strings! " +
            "Please **add screenshots** as soon as possible and **wait for translations** before releasing. }")
    String newNotificationMsg;

    @Value("${l10n.branchNotification.phabricator.notification.message.updated:Your branch was updated with new strings! " +
            "Please **add screenshots** as soon as possible and **wait for translations** before releasing. }")
    String updatedNotificationMsg;

    @Value("${l10n.branchNotification.phabricator.notification.message.noMoreStrings:The branch was updated and there are no more strings to translate.}")
    String noMoreStringsMsg;

    @Value("${l10n.branchNotification.phabricator.notification.message.translationsReady:Translations are ready!!}")
    String translationsReadyMsg;

    @Value("${l10n.branchNotification.phabricator.notification.message.screenshotsMissing:Please provide screenshots to help localization team}")
    String screenshotsMissingMsg;

    public String getNewMessage(String branchName, List<String> sourceStrings) {
        return newNotificationMsg +
                getLinkGoToMojito(branchName) + "\n\n" +
                getFormattedSourceStrings(sourceStrings);
    }

    public String getUpdatedMessage(String branchName, List<String> sourceStrings) {

        String msg = null;

        if (sourceStrings.isEmpty()) {
            msg = noMoreStringsMsg;
        } else {
            msg = updatedNotificationMsg +
                    getLinkGoToMojito(branchName) + "\n\n" +
                    getFormattedSourceStrings(sourceStrings);
        }
        return msg;
    }

    public String getNoMoreStringsMessage() { return noMoreStringsMsg; }

    public String getTranslatedMessage() {
        return translationsReadyMsg;
    }

    public String getScreenshotMissingMessage() {
        return screenshotsMissingMsg;
    }

    String getFormattedSourceStrings(List<String> sourceStrings) {
        return "**Strings:**\n" + sourceStrings.stream().map(t -> " - " + t).collect(Collectors.joining("\n"));
    }

    String getLinkGoToMojito(String branchName) {
        return "[→ Go to Mojito](" + branchUrlBuilder.getBranchDashboardUrl(branchName) + ")";
    }

}

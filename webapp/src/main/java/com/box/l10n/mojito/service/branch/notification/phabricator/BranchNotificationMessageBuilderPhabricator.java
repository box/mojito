package com.box.l10n.mojito.service.branch.notification.phabricator;

import com.box.l10n.mojito.service.branch.BranchUrlBuilder;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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

    public String getNewMessage(String branchName, List<String> sourceStrings) {
        return "We received your strings! Please **add screenshots** " +
                "as soon as possible and **wait for translations** before releasing. " +
                getLinkGoToMojito(branchName) + "\n\n" +
                getFormattedSourceStrings(sourceStrings);
    }

    public String getUpdatedMessage(String branchName, List<String> sourceStrings) {
        return "Your branch was updated with new strings! Please **add screenshots** " +
                "as soon as possible and **wait for translations** before releasing. " +
                getLinkGoToMojito(branchName) + "\n\n" +
                getFormattedSourceStrings(sourceStrings);
    }

    public String getTranslatedMessage() {
        return "Translations are ready!!";
    }

    public String getScreenshotMissingMessage() {
        return "Screenshots missing";
    }

    String getFormattedSourceStrings(List<String> sourceStrings) {
        return "**Strings:**\n" + sourceStrings.stream().map(t -> " - " + t).collect(Collectors.joining("\n"));
    }

    String getLinkGoToMojito(String branchName) {
        return "[→ Go to Mojito](" + branchUrlBuilder.getBranchDashboardUrl(branchName) + ")";
    }

}

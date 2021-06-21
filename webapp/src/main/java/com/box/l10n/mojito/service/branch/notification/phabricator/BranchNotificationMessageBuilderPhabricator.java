package com.box.l10n.mojito.service.branch.notification.phabricator;

import com.box.l10n.mojito.service.branch.BranchUrlBuilder;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
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

        String msg = null;

        if (sourceStrings.isEmpty()) {
            msg = "The branch was updated and there are no more strings to translate.";
        } else {
            msg = "Your branch was updated with new strings! Please **add screenshots** " +
                    "as soon as possible and **wait for translations** before releasing. " +
                    getLinkGoToMojito(branchName) + "\n\n" +
                    getFormattedSourceStrings(sourceStrings);
        }
        return msg;
    }

    public String getTranslatedMessage() {
        return "Translations are ready!!";
    }

    public String getScreenshotMissingMessage() {
        return "Please provide screenshots to help localization team";
    }

    String getFormattedSourceStrings(List<String> sourceStrings) {
        return "**Strings:**\n" + sourceStrings.stream().map(t -> " - " + t).collect(Collectors.joining("\n"));
    }

    String getLinkGoToMojito(String branchName) {
        return "[→ Go to Mojito](" + branchUrlBuilder.getBranchDashboardUrl(branchName) + ")";
    }

}

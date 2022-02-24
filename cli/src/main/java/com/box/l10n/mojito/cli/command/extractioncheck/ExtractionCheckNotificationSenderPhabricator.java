package com.box.l10n.mojito.cli.command.extractioncheck;

import com.box.l10n.mojito.cli.command.checks.CliCheckResult;
import com.box.l10n.mojito.phabricator.DifferentialRevision;
import com.box.l10n.mojito.thirdpartynotification.phabricator.PhabricatorIcon;
import com.google.common.base.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import java.util.List;
import java.util.stream.Collectors;

@Configurable
public class ExtractionCheckNotificationSenderPhabricator extends ExtractionCheckNotificationSender {

    @Autowired
    DifferentialRevision differentialRevision;

    String objectId;

    public ExtractionCheckNotificationSenderPhabricator(String objectId, String messageTemplate, String hardFailureMessage, String checksSkippedMessage) {
        super(messageTemplate, hardFailureMessage, checksSkippedMessage);
        if (Strings.isNullOrEmpty(objectId)) {
            throw new ExtractionCheckNotificationSenderException("Phabricator object id must be provided if using Phabricator notifications");
        }
        this.objectId = objectId;
    }

    @Override
    public void sendFailureNotification(List<CliCheckResult> results, boolean hardFail) {
        if (!isNullOrEmpty(results) && results.stream().anyMatch(result -> !result.isSuccessful())) {
            StringBuilder sb = new StringBuilder();
            sb.append("**i18n source string checks failed**" + getDoubleNewLines());
            if (hardFail) {
                sb.append("The following checks had hard failures:" + System.lineSeparator() +
                        getCheckerHardFailures(results).map(failure -> "**" + failure.getCheckName() + "**").collect(Collectors.joining(System.lineSeparator())));
            }
            sb.append(getDoubleNewLines());
            sb.append("**" + "Failed checks:" + "**" + getDoubleNewLines());
            sb.append(results.stream().map(check -> "**" + check.getCheckName() + "**" + getDoubleNewLines() + check.getNotificationText()).collect(Collectors.joining(System.lineSeparator())));
            sb.append(getDoubleNewLines() + "**" + "Please correct the above issues in a new commit." + "**");
            String message = getFormattedNotificationMessage(messageTemplate, "baseMessage", replaceQuoteMarkers(appendHardFailureMessage(hardFail, sb)));
            differentialRevision.addComment(objectId, PhabricatorIcon.WARNING + " " + message);
        }
    }

    @Override
    public void sendChecksSkippedNotification() {
        if (!Strings.isNullOrEmpty(checksSkippedMessage)) {
            differentialRevision.addComment(objectId, PhabricatorIcon.WARNING + " " +checksSkippedMessage);
        }
    }

    @Override
    public String replaceQuoteMarkers(String message) {
        return message.replaceAll(QUOTE_MARKER, "`");
    }
}

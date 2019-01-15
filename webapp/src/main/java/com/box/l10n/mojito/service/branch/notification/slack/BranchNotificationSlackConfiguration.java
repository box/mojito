package com.box.l10n.mojito.service.branch.notification.slack;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("l10n.branchNotification.slack")
class BranchNotificationSlackConfiguration {

    String mojitoUrl;

    String userEmailPattern;

    public String getMojitoUrl() {
        return mojitoUrl;
    }

    public void setMojitoUrl(String mojitoUrl) {
        this.mojitoUrl = mojitoUrl;
    }

    public String getUserEmailPattern() {
        return userEmailPattern;
    }

    public void setUserEmailPattern(String userEmailPattern) {
        this.userEmailPattern = userEmailPattern;
    }
}

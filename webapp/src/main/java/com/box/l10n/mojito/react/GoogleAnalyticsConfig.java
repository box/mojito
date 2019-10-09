package com.box.l10n.mojito.react;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("l10n.google-analytics") //TODO need to update config
public class GoogleAnalyticsConfig {

    boolean enabled = false;

    boolean hashedUserId = false;

    String trackingId;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getTrackingId() {
        return trackingId;
    }

    public void setTrackingId(String trackingId) {
        this.trackingId = trackingId;
    }

    public boolean isHashedUserId() {
        return hashedUserId;
    }

    public void setHashedUserId(boolean hashedUserId) {
        this.hashedUserId = hashedUserId;
    }
}

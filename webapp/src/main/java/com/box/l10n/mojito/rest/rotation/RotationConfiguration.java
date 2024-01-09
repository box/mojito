package com.box.l10n.mojito.rest.rotation;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("rotation")
public class RotationConfiguration {

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Enable support for server rotation.
     * When enabled, server can be toggled in and out of rotation through an API endpoint {@link RotationWS#setRotation}.
     * Putting server out of rotation will be reflected in its actuator health status {@link HealthRotation}.
     */
    boolean enabled = true;

}

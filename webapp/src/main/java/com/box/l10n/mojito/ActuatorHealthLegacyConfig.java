package com.box.l10n.mojito;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

/**
 * Opt-in forwarding of /health to /actuator/health for legacy support.
 * <p>
 * With spring boot 1 (hence previous version of Mojito) the health check was on /health. Since rotation and health
 * checks were never clearly documented and implyied relying on spring boot capabilities, we make this as opt-in.
 *
 * @author jaurambault
 */
@Configuration
@ConfigurationProperties("l10n.actuator.health.legacy")
public class ActuatorHealthLegacyConfig {

    boolean forwarding = false;

    public boolean isForwarding() {
        return forwarding;
    }

    public void setForwarding(boolean forwarding) {
        this.forwarding = forwarding;
    }
}

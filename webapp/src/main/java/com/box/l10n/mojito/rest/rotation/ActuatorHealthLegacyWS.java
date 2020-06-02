package com.box.l10n.mojito.rest.rotation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

/**
 * Opt-in forwarding of /health to /actuator/health for legacy support.
 *
 * With spring boot 1 (hence previous version of Mojito) the health check was on /health. Since rotation and health
 * checks were never clearly documented and implyied relying on spring boot capabilities, we make this as opt-in.
 *
 * @author jaurambault
 */
@ConditionalOnProperty(value = "l10n.actuator.health.legacy.forwarding", havingValue = "true")
@RestController
public class ActuatorHealthLegacyWS {

    @GetMapping("/health")
    public ModelAndView redirectWithUsingForwardPrefix(ModelMap model) {
        model.addAttribute("attribute", "forwardWithForwardPrefix");
        return new ModelAndView("forward:/actuator/health", model);
    }
}

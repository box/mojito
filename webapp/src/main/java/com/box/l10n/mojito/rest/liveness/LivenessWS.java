package com.box.l10n.mojito.rest.liveness;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Basic liveness endpoint. Note that this is only needed until Spring Boot update 2.3
 * (since then Actuator provides a built-in liveness probe as well).
 */
@RestController
public class LivenessWS {

    @RequestMapping(method = RequestMethod.GET, value = "/ping")
    @ResponseStatus(HttpStatus.OK)
    public String ping() {
        return "pong";
    }
}

package com.box.l10n.mojito.rest.rotation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * @author jaurambault
 */
//TODO(spring2) this is not working anymore
@Component
public class HealthRotation implements HealthIndicator {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(HealthRotation.class);

    Boolean inRotation = true;

    String host;

    @PostConstruct
    public void init() {
        host = getHost();
    }

    @Override
    public Health health() {
        Health.Builder builder;

        if (inRotation) {
            builder = Health.up();
        } else {
            builder = Health.down();
        }
        return builder.withDetail("host", host).build();
    }

    public void setInRotation(Boolean inRotation) {
        this.inRotation = inRotation;
    }


    String getHost() {
        String host;
        try {
            host = getHostAsync().get(1, TimeUnit.SECONDS);
        } catch (Exception e) {
            host = "Can't get host";
        }
        return host;
    }

    @Async
    Future<String> getHostAsync() throws UnknownHostException {
        String host = InetAddress.getLocalHost().getHostName();
        return new AsyncResult<>(host);
    }

}

package com.box.l10n.mojito.utils;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * General config related to Mojito server deployment
 */
@Component
@ConfigurationProperties("l10n.server")
public class ServerConfig {

    /**
     * Should be configured with CNAME of the Mojito instance: host or load balancer
     */
    String url = "http://localhost:8080/";

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}


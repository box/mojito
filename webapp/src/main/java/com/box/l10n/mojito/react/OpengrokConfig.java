package com.box.l10n.mojito.react;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@ConfigurationProperties("l10n.opengrok")
public class OpengrokConfig {

    String server;

    String extractedFilePrefix;

    Map<String, String> repositoryMapping = new HashMap<>();

    public String getServer() {
        return server;
    }

    public void setServer(String server) {
        this.server = server;
    }

    public String getExtractedFilePrefix() {
        return extractedFilePrefix;
    }

    public void setExtractedFilePrefix(String extractedFilePrefix) {
        this.extractedFilePrefix = extractedFilePrefix;
    }

    public Map<String, String> getRepositoryMapping() {
        return repositoryMapping;
    }

    public void setRepositoryMapping(Map<String, String> repositoryMapping) {
        this.repositoryMapping = repositoryMapping;
    }
}

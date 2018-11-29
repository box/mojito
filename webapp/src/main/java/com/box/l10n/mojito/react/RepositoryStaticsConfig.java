package com.box.l10n.mojito.react;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("l10n.repositoryStatistics")
public class RepositoryStaticsConfig {
    Boolean computeOutOfSla = false;

    public Boolean getComputeOutOfSla() {
        return computeOutOfSla;
    }

    public void setComputeOutOfSla(Boolean computeOutOfSla) {
        this.computeOutOfSla = computeOutOfSla;
    }
}

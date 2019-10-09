package com.box.l10n.mojito.service.drop;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * To configure the service that import/export drops.
 * 
 * @author jaurambault
 */
@Component
@ConfigurationProperties("l10n.dropservice")
public class DropServiceConfig {

    /**
     * Week offset (from the current week) used when computing the drop name.
     */
    int dropNameWeekOffset = 0;

    public int getDropNameWeekOffset() {
        return dropNameWeekOffset;
    }

    public void setDropNameWeekOffset(int dropNameWeekOffset) {
        this.dropNameWeekOffset = dropNameWeekOffset;
    }

}

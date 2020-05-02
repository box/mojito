package com.box.l10n.mojito.service.drop.exporter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 *
 * @author jaurambault
 */
@Component
@ConfigurationProperties("l10n.drop-exporter")
public class DropExporterConfig {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(DropExporterConfig.class);

    /**
     * Specify which system is used to export drops.
     */
    DropExporterType type = DropExporterType.FILE_SYSTEM;
 
    public DropExporterType getType() {
        return type;
    }

    public void setType(DropExporterType type) {
        this.type = type;
    }
    
}

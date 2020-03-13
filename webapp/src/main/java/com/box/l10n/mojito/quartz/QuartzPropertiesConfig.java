package com.box.l10n.mojito.quartz;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

@Configuration
@ConfigurationProperties("l10n.org")
public class QuartzPropertiesConfig {

    static Logger logger = LoggerFactory.getLogger(QuartzPropertiesConfig.class);

    Map<String, String> quartz = new HashMap<>();

    public Map<String, String> getQuartz() {
        return quartz;
    }

    @Bean
    public Properties getQuartzProperties() {

        Properties properties = new Properties();

        for (Map.Entry<String, String> entry : quartz.entrySet()) {
            properties.put("org.quartz." + entry.getKey(), entry.getValue());
            logger.debug("org.quartz.{}={}" , entry.getKey(), entry.getValue());
        }

        return properties;
    }
}

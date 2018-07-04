package com.box.l10n.mojito.quartz;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

@Configuration
@ConfigurationProperties("l10n.org")
public class QuartzPropertiesConfig {

    Map<String, String> quartz = new HashMap<>();

    public Map<String, String> getQuartz() {
        return quartz;
    }

    @Bean
    public Properties getQuartzProperties() {

        Properties properties = new Properties();

        for (Map.Entry<String, String> entry : quartz.entrySet()) {
            properties.put("org.quartz." + entry.getKey(), entry.getValue());
        }

        return properties;
    }
}
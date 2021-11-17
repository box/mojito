package com.box.l10n.mojito.phabricator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@ConditionalOnProperty("l10n.phabricator.url")
@Configuration
public class PhabricatorConfiguration {

    @Autowired
    PhabricatorConfigurationProperties phabricatorConfigurationProperties;

    @Bean
    public PhabricatorHttpClient phabricatorHttpClient() {
        return new PhabricatorHttpClient(phabricatorConfigurationProperties.getUrl(), phabricatorConfigurationProperties.getToken());
    }

    @Bean
    Harbormaster harbormaster() {
        return new Harbormaster(phabricatorHttpClient());
    }

    @Bean
    DifferentialRevision differentialRevision() {
        return new DifferentialRevision(phabricatorHttpClient());
    }

    @Bean
    DifferentialDiff differentialDiff() {
        return new DifferentialDiff(phabricatorHttpClient());
    }

    @Bean
    Phabricator phabricator() {
        return new Phabricator(differentialDiff(), harbormaster(), differentialRevision());
    }

    @Bean
    PhabricatorMessageBuilder phabricatorMessageBuilder() {
        return new PhabricatorMessageBuilder();
    }
}

package com.box.l10n.mojito.metrics;

import org.junit.jupiter.api.Test;

import io.micrometer.core.instrument.config.NamingConvention;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class StatsdNamingConventionConfigTest {

    @Test
    void dotConventionDoesntchangesNaming() {

        StatsdNamingConventionConfig config = new StatsdNamingConventionConfig();
        config.setNamingConvention(NamingConvention.dot);

        assertThat(config.changesNaming()).isFalse();
    }

    @Test
    void identityConventionDoesntchangesNaming() {

        StatsdNamingConventionConfig config = new StatsdNamingConventionConfig();
        config.setNamingConvention(NamingConvention.identity);

        assertThat(config.changesNaming()).isFalse();
    }

    @Test
    void snakeCaseConventionDoesntchangesNaming() {

        StatsdNamingConventionConfig config = new StatsdNamingConventionConfig();
        config.setNamingConvention(NamingConvention.snakeCase);

        assertThat(config.changesNaming()).isTrue();
    }

    @Test
    void camelCaseConventionDoesntchangesNaming() {

        StatsdNamingConventionConfig config = new StatsdNamingConventionConfig();
        config.setNamingConvention(NamingConvention.camelCase);

        assertThat(config.changesNaming()).isTrue();
    }

    @Test
    void slashesConventionDoesntchangesNaming() {

        StatsdNamingConventionConfig config = new StatsdNamingConventionConfig();
        config.setNamingConvention(NamingConvention.slashes);

        assertThat(config.changesNaming()).isTrue();
    }
}

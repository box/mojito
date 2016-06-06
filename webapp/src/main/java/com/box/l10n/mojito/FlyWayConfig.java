package com.box.l10n.mojito;

import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configure Flyway to provide an option to clean the database before starting
 * the migration. This means the database will be recreated from scratch.
 *
 * @author jaurambault
 */
@ConfigurationProperties(prefix = "l10n.flyway")
@Configuration
public class FlyWayConfig {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(FlyWayConfig.class);

    /**
     * To make flyway clean the database before doing migration (ie. re-create
     * the schema).
     */
    boolean clean = false;

    @Bean
    public FlywayMigrationStrategy cleanMigrateStrategy() {

        FlywayMigrationStrategy strategy = new FlywayMigrationStrategy() {

            @Override
            public void migrate(Flyway flyway) {

                if (clean) {
                    logger.info("Clean DB with Flyway");
                    flyway.clean();
                } else {
                    logger.info("Don't clean DB with Flyway");
                }

                flyway.migrate();
            }
        };

        return strategy;
    }
}

package com.box.l10n.mojito;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
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

                try {
                    logger.info("Flyway migrate() start");
                    flyway.migrate();
                    logger.info("Flyway migrate() finished");
                } catch (FlywayException fe) {
                    if (flyway.info().current().getVersion().getVersion().equals("50")) {
                        tryToMigrateIfMysql8Migration(flyway, fe);
                    } else
                        throw fe;
                }
            }
        };

        return strategy;
    }

    public boolean isClean() {
        return clean;
    }

    public void setClean(boolean clean) {
        this.clean = clean;
    }

    /**
     * To migrate to Mysql 8, we need to escape new reserved keyword "groups" in
     * V1__Initial_Setup.sql, which in turns changes the Flyway MD5. If the Flyway
     * migrate() fails for
     * that specific reason we just repair the schema, i.e. just change the MD5 for
     * version 1. *
     *
     * @param flyway
     * @param fe
     */
    void tryToMigrateIfMysql8Migration(Flyway flyway, FlywayException fe) {
        if (fe.getMessage()
                .contains(
                        "Migration checksum mismatch for migration version 1\n"
                                + "-> Applied to database : 1443976515\n"
                                + "-> Resolved locally    : -998267617")) {

            logger.info("Flyway repair()");
            flyway.repair();
            logger.info("Flyway repair() finished");
        } else {
            throw fe;
        }
    }
}

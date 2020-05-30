package com.box.l10n.mojito;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.util.Map;

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

    @Autowired
    DataSource dataSource;

    @Bean
    public FlywayMigrationStrategy cleanMigrateStrategy() {
        return flyway -> {
            if (EmbeddedDatabaseConnection.isEmbedded(dataSource)) {
                logger.info("Embbeded database don't run flyway");
                return;
            } else {
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
                    tryToMigrateIfSpringMigration(flyway, fe);
                }
            }
        };
    }

    void tryToMigrateIfSpringMigration(Flyway flyway, FlywayException fe) {
        if (fe.getMessage().contains("but no schema history table. Use baseline() or set baselineOnMigrate to true to initialize the schema history table")) {
            logger.info("There is no schema history table, we assume were migrating from Spring 1.x to 2.x");

            String baselineVersion = getBaselineVersionFromOldFlywayTable();
            logger.info("version: {}", baselineVersion);
            if (baselineVersion == null) {
                throw new RuntimeException("Can't read the version from the database, you must provide a Flyway baseline version to upgrade Mojito using spring.flyway.baseline-version");
            }

            flyway = rebuildFlywayInstanceWithBaselineFromOldTable(flyway, baselineVersion);

            logger.info("Run baseline() with version: {}", flyway.getConfiguration().getBaselineVersion().getVersion());
            flyway.baseline();

            logger.info("Try Flyway migrate() again");
            flyway.migrate();
            logger.info("Flyway migrate() finished");
        } else {
            throw fe;
        }
    }

    Flyway rebuildFlywayInstanceWithBaselineFromOldTable(Flyway flyway, String baselineVersion) {
        FluentConfiguration fluentConfiguration = new FluentConfiguration();
        fluentConfiguration.configuration(flyway.getConfiguration());
        fluentConfiguration.baselineVersion(baselineVersion);
        flyway = fluentConfiguration.load();
        return flyway;
    }

    String getBaselineVersionFromOldFlywayTable() {
        logger.info("Looking for a baseline version from the old Flyway version");
        String baselineVersion = null;
        try {
            JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
            Map<String, Object> stringObjectMap = jdbcTemplate.queryForMap("select MAX(CAST(version as UNSIGNED)) as version from schema_version");
            baselineVersion = String.valueOf(stringObjectMap.get("version"));
        } catch (Exception e) {
            logger.info("Couldn't get the old version");
        }
        return baselineVersion;
    }

    public boolean isClean() {
        return clean;
    }

    public void setClean(boolean clean) {
        this.clean = clean;
    }

}

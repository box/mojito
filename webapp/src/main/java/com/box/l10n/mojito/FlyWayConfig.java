package com.box.l10n.mojito;

import java.util.Map;
import javax.sql.DataSource;
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

/**
 * Configure Flyway to provide an option to clean the database before starting the migration. This
 * means the database will be recreated from scratch.
 *
 * @author jaurambault
 */
@ConfigurationProperties(prefix = "l10n.flyway")
@Configuration
public class FlyWayConfig {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(FlyWayConfig.class);

  /** To make flyway clean the database before doing migration (ie. re-create the schema). */
  boolean clean = false;

  @Autowired DataSource dataSource;

  @Bean
  public FlywayMigrationStrategy cleanMigrateStrategy() {
    return flyway -> {
      if (EmbeddedDatabaseConnection.isEmbedded(dataSource)) {
        logger.info("Embbeded database don't run flyway");
        return;
      } else {
        optionalClean(flyway);
        try {
          logger.info("Flyway migrate() start");
          flyway.migrate();
          logger.info("Flyway migrate() finished");
        } catch (FlywayException fe) {
          if (flyway.info().current().getVersion().getVersion().equals("63")) {
            tryToMigrateIfMysql8Migration(flyway, fe);
          } else {
            tryToMigrateIfSpringMigration(flyway, fe);
          }
        }
      }
    };
  }

  void optionalClean(Flyway flyway) {
    if (clean) {
      cleanIfNotProtected(flyway);
    } else {
      logger.info("Don't clean DB with Flyway");
    }
  }

  void cleanIfNotProtected(Flyway flyway) {
    if (isDBCleanProtectionEnabled()) {
      throw new RuntimeException(
          "Attempting to perform Flyway clean on a protected schema, abort. Please, check your configuration");
    } else {
      logger.info("Clean DB with Flyway");
      flyway.clean();
    }
  }

  void tryToMigrateIfSpringMigration(Flyway flyway, FlywayException fe) {
    if (fe.getMessage()
        .contains(
            "but no schema history table. Use baseline() or set baselineOnMigrate to true to initialize the schema history table")) {
      logger.info(
          "There is no schema history table, we assume were migrating from Spring 1.x to 2.x");

      String baselineVersion = getBaselineVersionFromOldFlywayTable();
      logger.info("version: {}", baselineVersion);
      if (baselineVersion == null) {
        throw new RuntimeException(
            "Can't read the version from the database, you must provide a Flyway baseline version to upgrade Mojito using spring.flyway.baseline-version");
      }

      flyway = rebuildFlywayInstanceWithBaselineFromOldTable(flyway, baselineVersion);

      logger.info(
          "Run baseline() with version: {}",
          flyway.getConfiguration().getBaselineVersion().getVersion());
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
      Map<String, Object> stringObjectMap =
          jdbcTemplate.queryForMap(
              "select MAX(CAST(version as UNSIGNED)) as version from schema_version");
      baselineVersion = String.valueOf(stringObjectMap.get("version"));
    } catch (Exception e) {
      logger.info("Couldn't get the old version");
    }
    return baselineVersion;
  }

  /**
   * To migrate to Mysql 8, we need to escape new reserved keyword "groups" in
   * V1__Initial_Setup.sql, which in turns changes the Flyway MD5. If the Flyway migrate() fails for
   * that specific reason we just repair the schema, i.e. just change the MD5 for version 1. *
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

  /**
   * Additional check to avoid cleaning a critical environment by mistake. This is a weak check
   * since a failure to perform the query will show that the DB is not protected.
   *
   * <p>This is an extra check added to the settings: spring.flyway.clean-disabled=true (now default
   * in Mojito) and l10n.flyway.clean=false (that is usally set manualy, but can be wrongly enabled)
   * and shouldn't be soly relied upon.
   *
   * <p>For now this is enabled manually in the database with: CREATE TABLE
   * flyway_clean_protection(enabled boolean default true); INSERT INTO flyway_clean_protection
   * (enabled) VALUES (1)
   *
   * @return
   */
  boolean isDBCleanProtectionEnabled() {
    boolean isDBCleanProtectionEnabled = false;

    try {
      JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
      Map<String, Object> stringObjectMap =
          jdbcTemplate.queryForMap("select enabled as enabled from flyway_clean_protection");
      isDBCleanProtectionEnabled = (Boolean) stringObjectMap.get("enabled");
    } catch (Exception e) {
      logger.info(
          "Can't check if the flyway clean protection is enabled, assume it is not protected");
    }

    return isDBCleanProtectionEnabled;
  }

  public boolean isClean() {
    return clean;
  }

  public void setClean(boolean clean) {
    this.clean = clean;
  }
}

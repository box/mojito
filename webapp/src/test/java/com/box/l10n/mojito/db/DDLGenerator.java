package com.box.l10n.mojito.db;

import com.box.l10n.mojito.io.Files;
import com.box.l10n.mojito.service.assetExtraction.ServiceTestBase;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.hbm2ddl.SchemaUpdate;
import org.hibernate.tool.schema.TargetType;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Not a test, this generates db migration scripts, that can be used to write actual migration
 * script stored in db/migration
 *
 * @author jaurambault
 */
public class DDLGenerator extends ServiceTestBase {

  static final String TARGET_DB_MIGRATION_VX_YY_ZZ_SQL = "target/db/migration/Vx__yy_zz.sql";

  /** logger */
  static Logger logger = LoggerFactory.getLogger(DDLGenerator.class);

  @Test
  public void generateCreateAnUpdateDDL() throws IOException {
    logger.debug("Generate create and update DDL");

    Files.createDirectories(Paths.get("target/db/migration/"));

    SchemaUpdate schemaUpdate = new SchemaUpdate();
    schemaUpdate.setDelimiter(";");
    schemaUpdate.setOutputFile(TARGET_DB_MIGRATION_VX_YY_ZZ_SQL);
    schemaUpdate.execute(
        EnumSet.of(TargetType.SCRIPT),
        HibernateInfoHolder.getMetadata(),
        HibernateInfoHolder.getServiceRegistry());
    //        removeLinesWithOldEnversFKs(Paths.get(TARGET_DB_MIGRATION_VX_YY_ZZ_SQL));

    SchemaExport schemaExport = new SchemaExport();
    schemaExport.setDelimiter(";");
    schemaExport.setOutputFile("target/db/migration/create.sql");
    schemaExport.execute(
        EnumSet.of(TargetType.SCRIPT),
        SchemaExport.Action.CREATE,
        HibernateInfoHolder.getMetadata());
  }

  /**
   * With Hibernate updates, the FKs generated for Envers have changed. Since I don't know about a
   * way to define proper ones, for now this will just skip the new reccords that match old keys.
   */
  public void removeLinesWithOldEnversFKs(Path path) {

    List<String> oldEnversFKs =
        Arrays.asList(
            "FKenhcxs3vtootah6jt7d39vdi0",
            "FKib0ce175ny4l9rn80bmul9grm",
            "FKd0w64q4vevkdepmpjy1rrosge",
            "FKge7vxubhuc8jmk8hk53r4j6fu",
            "FKhbmic26gsoa88els2s9bq7tvc",
            "FKjo0wwuetlr3r5prsyulljqm5i",
            "FK9m2hkxrk7o2k0n7tm5qx949s1",
            "FK2i6wx3ciy94a0djmyc0eu778",
            "FKrlir38c08c479vpkqo58t69t9");

    List<String> linesWithoutOldFKs =
        Files.lines(path)
            .filter(
                line -> {
                  return oldEnversFKs.stream().noneMatch(line::contains);
                })
            .collect(Collectors.toList());

    Files.write(path, linesWithoutOldFKs);
  }
}

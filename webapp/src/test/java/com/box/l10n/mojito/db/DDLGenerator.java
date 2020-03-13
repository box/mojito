package com.box.l10n.mojito.db;

import com.box.l10n.mojito.service.assetExtraction.ServiceTestBase;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.jpa.internal.EntityManagerFactoryImpl;
import org.hibernate.service.internal.SessionFactoryServiceRegistryImpl;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.hbm2ddl.SchemaUpdate;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Not a test, this generates db migration scripts, that can be used to write
 * actual migration script stored in db/migration
 *
 * @author jaurambault
 */
public class DDLGenerator extends ServiceTestBase {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(DDLGenerator.class);

    @Autowired
    LocalContainerEntityManagerFactoryBean lcemfb;

    @Ignore("TODO see if we can still support that or if we remove it")
    @Test
    public void generateCreateAnUpdateDDL() throws IOException {
        logger.debug("Generate create and update DDL");

        EntityManagerFactoryImpl emf = (EntityManagerFactoryImpl) lcemfb.getNativeEntityManagerFactory();

        SessionFactoryImplementor sf = emf.getSessionFactory();
        SessionFactoryServiceRegistryImpl serviceRegistry = (SessionFactoryServiceRegistryImpl) sf.getServiceRegistry();

        MetadataImplementor metadata = null;

//        try {
//            Field field = SessionFactoryServiceRegistryImpl.class.getDeclaredField("configuration");
//            field.setAccessible(true);
//            metadata = (MetadataImplementor) field.get(serviceRegistry);
//        } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
//            throw new RuntimeException(e);
//        }

        Files.createDirectories(Paths.get("target/db/migration/"));

        SchemaUpdate update = new SchemaUpdate(serviceRegistry, metadata);
        update.setDelimiter(";");
        update.setOutputFile("target/db/migration/Vx__yy_zz.sql");
        update.execute(false, false);

        SchemaExport export = new SchemaExport(serviceRegistry, metadata);
        export.setDelimiter(";");
        export.setOutputFile("target/db/migration/create.sql");
        export.execute(false, false, false, true);
    }

}

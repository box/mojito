package com.box.l10n.mojito.db;

import com.box.l10n.mojito.service.assetExtraction.ServiceTestBase;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Paths;

import com.box.l10n.mojito.service.eventlistener.EntityCrudEventListener;
import org.hibernate.cfg.Configuration;
import org.hibernate.internal.SessionFactoryImpl;
import org.hibernate.service.internal.SessionFactoryServiceRegistryImpl;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.hbm2ddl.SchemaUpdate;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;

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

//    @PersistenceUnit
//    EntityManagerFactory emf;

    @Test
    public void generateCreateAnUpdateDDL() throws IOException {
        logger.debug("Generate create and update DDL");

//        SessionFactoryImpl sessionFactory = emf.unwrap(SessionFactoryImpl.class);
//        SessionFactoryServiceRegistryImpl serviceRegistry = (SessionFactoryServiceRegistryImpl) sessionFactory.getServiceRegistry();
//        Configuration cfg = null;
//
//        try {
//            Field field = SessionFactoryServiceRegistryImpl.class.getDeclaredField("configuration");
//            field.setAccessible(true);
//            cfg = (Configuration) field.get(serviceRegistry);
//        } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
//            throw new RuntimeException(e);
//        }
//

//        TOOD
//        Files.createDirectories(Paths.get("target/db/migration/"));
//
//        SchemaUpdate update = new SchemaUpdate(serviceRegistry, cfg);
//        update.setDelimiter(";");
//        update.setOutputFile("target/db/migration/Vx__yy_zz.sql");
//        update.execute(false, false);
//
//        SchemaExport export = new SchemaExport(serviceRegistry, cfg);
//        export.setDelimiter(";");
//        export.setOutputFile("target/db/migration/create.sql");
//        export.execute(false, false, false, true);
    }

}

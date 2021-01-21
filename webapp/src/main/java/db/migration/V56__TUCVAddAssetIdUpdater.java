package db.migration;

import com.box.l10n.mojito.service.tm.TUCVAddAssetIdUpdater;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import javax.sql.DataSource;

/**
 * @author jaurambault
 */
public class V56__TUCVAddAssetIdUpdater extends BaseJavaMigration {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(V56__TUCVAddAssetIdUpdater.class);

    @Override
    public void migrate(Context context) throws Exception {
        migrate(new SingleConnectionDataSource(context.getConnection(), true));
    }

    public void migrate(DataSource dataSource) throws Exception {
        logger.info("Denormalize asset id in tm_text_unit_current_variant");
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        new TUCVAddAssetIdUpdater().performUpdate(jdbcTemplate);
    }

}

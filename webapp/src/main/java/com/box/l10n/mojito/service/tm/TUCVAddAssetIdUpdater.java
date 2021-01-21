package com.box.l10n.mojito.service.tm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class TUCVAddAssetIdUpdater {
    static Logger logger = LoggerFactory.getLogger(TUCVAddAssetIdUpdaterJob.class);

    public void performUpdate(JdbcTemplate jdbcTemplate) {
        int updateCount = 0;
        do {
            try {
                updateCount = jdbcTemplate.update(""
                        + "update tm_text_unit_current_variant tucv, (\n"
                        + "    select tucv.id as tucv_id, tu.asset_id as asset_id\n"
                        + "    from tm_text_unit_current_variant tucv\n"
                        + "    inner join tm_text_unit as tu on tu.id = tucv.tm_text_unit_id\n"
                        + "    where \n"
                        + "        tucv.asset_id is null\n"
                        + "    limit 100000 \n"
                        + "    ) d\n"
                        + "set tucv.asset_id = d.asset_id "
                        + "where tucv.id = d.tucv_id and tucv.asset_id is null");

                logger.info("TmTextUnitCurrentVariant update count: {}", updateCount);
            } catch (Exception e) {
                logger.error("Couldn't update asset id, ignore", e);
            }
        } while (updateCount > 0 );
    }
}

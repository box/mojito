package com.box.l10n.mojito.service.pluralform;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * This is to update all text units with missing plural form since the
 * introduction of the new plural form support.
 *
 * Instead of a scheduler it could be called during asset extraction but this
 * way don't impact the standard workflow.
 *
 * This task could be removed later when everything as been migrated.
 *
 * @author jaurambault
 */
@Service
public class PluralFormUpdater {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(PluralFormUpdater.class);

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Value("${spring.jpa.database}")
    String driver;
    
    @Value("${l10n.PluralFormUpdater:false}")
    boolean enabled;

    @Scheduled(fixedDelay = 30000)
    private void updateTextUnitsWithMissingPluralForms() {
        
        if (!enabled) {
           logger.debug("PluralFormUpdater is disabled");    
        } else if ("HSQL".equals(driver)) {
           logger.debug("Don't update (DB is HSQL)");
        } else {

            logger.debug("Update old text unit with plural form that are now avaible with new plural support");

            try {
                int updateCount = jdbcTemplate.update(""
                        + "update tm_text_unit tu, (\n"
                        + "    select tu.id as tu_id, atu.plural_form_id as plural_form_id, atu.plural_form_other as plural_form_other \n"
                        + "    from tm_text_unit tu\n"
                        + "    inner join asset_text_unit_to_tm_text_unit map on map.tm_text_unit_id = tu.id\n"
                        + "    inner join asset_text_unit atu on map.asset_text_unit_id = atu.id\n"
                        + "    where \n"
                        + "        tu.plural_form_id is null and atu.plural_form_id is not null\n"
                        + "    ) d\n"
                        + "set tu.plural_form_id = d.plural_form_id, tu.plural_form_other =  d.plural_form_other "
                        + "where tu.id = d.tu_id");

                logger.debug("TmTextUnit update count: {}", updateCount);
            } catch (Exception e) {
                logger.error("Couldn't update plural forms, ignore", e);
            }
        }
    }
}

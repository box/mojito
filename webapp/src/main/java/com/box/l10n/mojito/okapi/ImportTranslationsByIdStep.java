package com.box.l10n.mojito.okapi;

import com.box.l10n.mojito.entity.TMTextUnit;
import net.sf.okapi.common.resource.ITextUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Configurable;

/**
 * @author jaurambault
 */
@Configurable
public class ImportTranslationsByIdStep extends AbstractImportTranslationsStep {

    /**
     * Logger
     */
    static Logger logger = LoggerFactory.getLogger(ImportTranslationsByIdStep.class);

    @Override
    public String getName() {
        return "Import translations";
    }

    @Override
    public String getDescription() {
        return "Updates the TM with the extracted new/changed variants."
                + " Expects: raw document. Sends back: original events.";
    }

    @Override
    TMTextUnit getTMTextUnit() {

        TMTextUnit tmTextUnit = null;

        try {
            Long tmTextUnitId = Long.valueOf(textUnit.getId());
            tmTextUnit = tmTextUnitRepository.findOne(tmTextUnitId);
        } catch (NumberFormatException nfe) {
            logger.debug("Could not convert the textUnit id into a Long (TextUnit id)", nfe);
        }

        return tmTextUnit;
    }
    
}

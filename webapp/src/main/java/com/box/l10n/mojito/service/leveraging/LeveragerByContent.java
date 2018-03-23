package com.box.l10n.mojito.service.leveraging;

import com.box.l10n.mojito.entity.TMTextUnit;
import com.box.l10n.mojito.service.tm.search.StatusFilter;
import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcherParameters;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Performs leveraging based on the content of the text units. The
 * leveraged translations will need to be re-translated
 * 
 * @author jaurambault
 */
@Component
public class LeveragerByContent extends AbstractLeverager {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(LeveragerByContent.class);

    @Override
    public List<TextUnitDTO> getLeveragingMatches(TMTextUnit tmTextUnit, Long sourceTmId) {
        logger.debug("Get TextUnitDTOs for leveraging by content");

        TextUnitSearcherParameters textUnitSearcherParameters = new TextUnitSearcherParameters();
        textUnitSearcherParameters.setTmId(sourceTmId);
        textUnitSearcherParameters.setSource(tmTextUnit.getContent());
        textUnitSearcherParameters.setStatusFilter(StatusFilter.TRANSLATED);
        if (tmTextUnit.getPluralForm() != null) {
            textUnitSearcherParameters.setPluralFormId(tmTextUnit.getPluralForm().getId());
        }
        return textUnitSearcher.search(textUnitSearcherParameters);
    }

    @Override
    public boolean isTranslationNeededIfUniqueMatch() {
        return true;
    }

    @Override
    public String getType() {
        return "Leverage by content";
    }

}

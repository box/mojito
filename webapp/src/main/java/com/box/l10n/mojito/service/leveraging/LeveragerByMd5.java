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
 *
 * @author jaurambault
 */
@Component
public class LeveragerByMd5 extends AbstractLeverager {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(LeveragerByMd5.class);

    @Override
    public List<TextUnitDTO> getLeveragingMatches(TMTextUnit tmTextUnit, Long sourceTmId, Long sourceAssetId) {
        logger.debug("Get TextUnitDTOs for leveraging by MD5");

        TextUnitSearcherParameters textUnitSearcherParameters = new TextUnitSearcherParameters();
        textUnitSearcherParameters.setMd5(tmTextUnit.getMd5());
        textUnitSearcherParameters.setTmId(sourceTmId);
        textUnitSearcherParameters.setAssetId(sourceAssetId);
        textUnitSearcherParameters.setStatusFilter(StatusFilter.TRANSLATED);

        return textUnitSearcher.search(textUnitSearcherParameters);
    }

    @Override
    public boolean isTranslationNeededIfUniqueMatch() {
        return false;
    }

    @Override
    public String getType() {
        return "Leverage by source Md5";
    }

}

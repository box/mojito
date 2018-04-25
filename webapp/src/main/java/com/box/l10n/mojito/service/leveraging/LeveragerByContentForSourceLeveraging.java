package com.box.l10n.mojito.service.leveraging;

import com.box.l10n.mojito.entity.TMTextUnit;
import static com.box.l10n.mojito.service.leveraging.AbstractLeverager.logger;
import com.box.l10n.mojito.service.tm.search.StatusFilter;
import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcherParameters;
import com.box.l10n.mojito.service.tm.search.UsedFilter;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Performs leveraging based on the content of text units. The leveraged
 * translations don't need to be re-translated.
 * <p>
 * In other words, this will catch modification to the name (and optionally in
 * the comments too).
 *
 * @author jaurambault
 */
@Component
public class LeveragerByContentForSourceLeveraging extends AbstractLeverager {

    @Override
    public String getType() {
        return "by content for source leveraging";
    }

    @Override
    public List<TextUnitDTO> getLeveragingMatches(TMTextUnit tmTextUnit, Long sourceTmId, Long sourceAssetId) {

        logger.debug("Get TextUnitDTOs for leveraging by content");

        TextUnitSearcherParameters textUnitSearcherParameters = new TextUnitSearcherParameters();
        textUnitSearcherParameters.setSource(tmTextUnit.getContent());
        textUnitSearcherParameters.setStatusFilter(StatusFilter.TRANSLATED);
        textUnitSearcherParameters.setUsedFilter(UsedFilter.USED);
        textUnitSearcherParameters.setAssetId(tmTextUnit.getAsset().getId());
        if (tmTextUnit.getPluralForm() != null) {
            textUnitSearcherParameters.setPluralFormId(tmTextUnit.getPluralForm().getId());
        }
        return textUnitSearcher.search(textUnitSearcherParameters);

    }

    @Override
    public boolean isTranslationNeededIfUniqueMatch() {
        return false;
    }

}

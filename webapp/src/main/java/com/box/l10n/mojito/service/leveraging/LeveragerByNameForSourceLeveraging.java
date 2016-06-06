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
 * Performs leveraging based on the name of text units. The leveraged
 * translations need to be re-translated.
 * <p>
 * In other words, this will catch modification in the source text (and
 * optionally in the comments too).
 *
 * @author jaurambault
 */
@Component
public class LeveragerByNameForSourceLeveraging extends AbstractLeverager {

    @Override
    public String getType() {
        return "by name for source leveraging";
    }

    @Override
    public List<TextUnitDTO> getLeveragingMatches(TMTextUnit tmTextUnit, Long sourceTmId) {
        logger.debug("Get TextUnitDTOs for leveraging by name");

        TextUnitSearcherParameters textUnitSearcherParameters = new TextUnitSearcherParameters();
        textUnitSearcherParameters.setName(tmTextUnit.getName());
        textUnitSearcherParameters.setStatusFilter(StatusFilter.TRANSLATED);
        textUnitSearcherParameters.setUsedFilter(UsedFilter.USED);
        textUnitSearcherParameters.setAssetId(tmTextUnit.getAsset().getId());

        return textUnitSearcher.search(textUnitSearcherParameters);
    }

    @Override
    public boolean isTranslationNeededIfUniqueMatch() {
        return true;
    }

}

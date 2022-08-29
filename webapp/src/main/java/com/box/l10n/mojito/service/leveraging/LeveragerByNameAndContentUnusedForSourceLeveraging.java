package com.box.l10n.mojito.service.leveraging;

import com.box.l10n.mojito.entity.TMTextUnit;
import com.box.l10n.mojito.service.tm.search.StatusFilter;
import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcherParameters;
import com.box.l10n.mojito.service.tm.search.UsedFilter;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Performs leveraging based on the name and content of the text units which can be unused. The
 * leveraged translations don't need re-translated.
 *
 * <p>In other words this will catch modification in comments only.
 *
 * @author jaurambault
 */
@Component
public class LeveragerByNameAndContentUnusedForSourceLeveraging extends AbstractLeverager {

  @Override
  public String getType() {
    return "by name and content from unused for source leveraging";
  }

  @Override
  public List<TextUnitDTO> getLeveragingMatches(
      TMTextUnit tmTextUnit, Long sourceTmId, Long sourceAssetId) {

    logger.debug("Get TextUnitDTOs for leveraging by name and content from unused");

    TextUnitSearcherParameters textUnitSearcherParameters = new TextUnitSearcherParameters();
    textUnitSearcherParameters.setName(tmTextUnit.getName());
    textUnitSearcherParameters.setSource(tmTextUnit.getContent());
    textUnitSearcherParameters.setStatusFilter(StatusFilter.TRANSLATED);
    textUnitSearcherParameters.setUsedFilter(UsedFilter.UNUSED);
    textUnitSearcherParameters.setAssetId(tmTextUnit.getAsset().getId());

    return textUnitSearcher.search(textUnitSearcherParameters);
  }

  @Override
  public boolean isTranslationNeededIfUniqueMatch() {
    return false;
  }
}

package com.box.l10n.mojito.service.leveraging;

import static com.box.l10n.mojito.service.leveraging.AbstractLeverager.logger;

import com.box.l10n.mojito.entity.TMTextUnit;
import com.box.l10n.mojito.service.tm.search.StatusFilter;
import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcherParameters;
import com.box.l10n.mojito.service.tm.search.UsedFilter;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Performs leveraging based on the name and content of the text units. The leveraged translations
 * don't need re-translated.
 *
 * <p>In other words this will catch modification in comments only.
 *
 * @author jaurambault
 */
@Component
public class LeveragerByNameAndContentForSourceLeveraging extends AbstractLeverager {

  @Override
  public String getType() {
    return "by name and content for source leveraging";
  }

  @Override
  public List<TextUnitDTO> getLeveragingMatches(
      TMTextUnit tmTextUnit, Long sourceTmId, Long sourceAssetId) {

    logger.debug("Get TextUnitDTOs for leveraging by name and content");

    TextUnitSearcherParameters textUnitSearcherParameters = new TextUnitSearcherParameters();
    textUnitSearcherParameters.setName(tmTextUnit.getName());
    textUnitSearcherParameters.setSource(tmTextUnit.getContent());
    textUnitSearcherParameters.setStatusFilter(StatusFilter.TRANSLATED);
    textUnitSearcherParameters.setUsedFilter(UsedFilter.USED);
    textUnitSearcherParameters.setAssetId(tmTextUnit.getAsset().getId());

    return textUnitSearcher.search(textUnitSearcherParameters);
  }

  @Override
  public boolean isTranslationNeededIfUniqueMatch() {
    return false;
  }
}
